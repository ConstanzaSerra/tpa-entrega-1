package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.RutaRequestDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RutaControllerTest {
    private RutaRepository rutaRepository;
    private CamionRepository camionRepository;
    private EntregaRepository entregaRepository;
    private DonacionesAPI donacionesAPI;
    private RutaController rutaController;
    private Context ctx;

    @BeforeEach
    void setUp() {
        rutaRepository = mock(RutaRepository.class);
        camionRepository = mock(CamionRepository.class);
        entregaRepository = mock(EntregaRepository.class);
        donacionesAPI = mock(DonacionesAPI.class);
        rutaController = new RutaController(rutaRepository, camionRepository, entregaRepository, donacionesAPI);
        ctx = mock(Context.class);
    }

    // Si se intenta iniciar una ruta con un ID que no existe, debe responder 404 y no notificar a Donaciones
    @Test
    void testIniciarRuta_RutaNoExiste_Devuelve404() {
        when(ctx.pathParam("id")).thenReturn("99");
        when(rutaRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(ctx.status(HttpStatus.NOT_FOUND)).thenReturn(ctx);

        rutaController.iniciarRuta(ctx);

        verify(ctx).status(HttpStatus.NOT_FOUND);
        verify(donacionesAPI, never()).notificarInicioRuta(any());
    }

    // Si la ruta ya fue iniciada (EN_CURSO), intentar iniciarla de nuevo debe responder 400
    @Test
    void testIniciarRuta_RutaYaEnCurso_Devuelve400() {
        Ruta ruta = new Ruta();
        ruta.setId(1L);
        ruta.iniciar(); // ya está EN_CURSO

        when(ctx.pathParam("id")).thenReturn("1");
        when(rutaRepository.buscarPorId(1L)).thenReturn(Optional.of(ruta));
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        rutaController.iniciarRuta(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(donacionesAPI, never()).notificarInicioRuta(any());
    }

    // Caso feliz: iniciar ruta pasa la ruta a EN_CURSO, las entregas a EN_TRASLADO y notifica a Donaciones
    @Test
    void testIniciarRuta_NotificaADonacionesYCambiaEstados() {
        Ruta ruta = new Ruta();
        ruta.setId(1L);
        ParadaDeRuta parada = new ParadaDeRuta(10L, "Calle");
        Entrega entrega = new Entrega(100L, 10L);
        entrega.setId(500L);
        parada.agregarEntrega(entrega);
        ruta.agregarParada(parada);

        when(ctx.pathParam("id")).thenReturn("1");
        when(rutaRepository.buscarPorId(1L)).thenReturn(Optional.of(ruta));
        when(ctx.status(HttpStatus.OK)).thenReturn(ctx);

        rutaController.iniciarRuta(ctx);

        assertEquals(EstadoRuta.EN_CURSO, ruta.getEstado());
        
        ArgumentCaptor<EventoInicioRutaDTO> captor = ArgumentCaptor.forClass(EventoInicioRutaDTO.class);
        verify(donacionesAPI, times(1)).notificarInicioRuta(captor.capture());
        
        EventoInicioRutaDTO evento = captor.getValue();
        assertEquals(1L, evento.rutaId);
        assertEquals(1, evento.entregasAfectadas.size());
        assertEquals(500L, evento.entregasAfectadas.get(0).entregaId);
        
        verify(ctx).status(HttpStatus.OK);
    }

    // POST /rutas crea la ruta con sus paradas y entregas y responde 201
    @Test
    void testCrearRuta_Devuelve201YGuardaEntregas() {
        Camion camion = new Camion("ABC1234", 1, 1, 1);
        camion.setId(5L);

        RutaRequestDTO dto = new RutaRequestDTO();
        dto.camionId = 5L;
        RutaRequestDTO.ParadaRequestDTO parada = new RutaRequestDTO.ParadaRequestDTO();
        parada.entidadBeneficiariaId = 10L;
        parada.direccion = "Av. Siempreviva 742";
        parada.donacionesIds = java.util.List.of(100L, 101L);
        dto.paradas = java.util.List.of(parada);

        when(ctx.bodyAsClass(RutaRequestDTO.class)).thenReturn(dto);
        when(camionRepository.buscarPorId(5L)).thenReturn(Optional.of(camion));
        when(rutaRepository.guardar(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ctx.status(HttpStatus.CREATED)).thenReturn(ctx);

        rutaController.create(ctx);

        verify(entregaRepository, times(2)).guardar(any(Entrega.class));
        ArgumentCaptor<Ruta> captor = ArgumentCaptor.forClass(Ruta.class);
        verify(rutaRepository).guardar(captor.capture());
        assertEquals(1, captor.getValue().getParadas().size());
        verify(ctx).status(HttpStatus.CREATED);
    }

    // POST /rutas con un camion inexistente responde 404
    @Test
    void testCrearRuta_CamionNoExiste_Devuelve404() {
        RutaRequestDTO dto = new RutaRequestDTO();
        dto.camionId = 99L;

        when(ctx.bodyAsClass(RutaRequestDTO.class)).thenReturn(dto);
        when(camionRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(ctx.status(HttpStatus.NOT_FOUND)).thenReturn(ctx);

        rutaController.create(ctx);

        verify(ctx).status(HttpStatus.NOT_FOUND);
        verify(rutaRepository, never()).guardar(any());
    }

    // DELETE /rutas/{id} elimina una ruta PLANIFICADA
    @Test
    void testEliminarRuta_Planificada_Elimina() {
        Ruta ruta = new Ruta();
        ruta.setId(1L); // queda PLANIFICADA

        when(ctx.pathParam("id")).thenReturn("1");
        when(rutaRepository.buscarPorId(1L)).thenReturn(Optional.of(ruta));
        when(ctx.status(HttpStatus.NO_CONTENT)).thenReturn(ctx);

        rutaController.delete(ctx);

        verify(rutaRepository).eliminar(1L);
        verify(ctx).status(HttpStatus.NO_CONTENT);
    }

    // DELETE /rutas/{id} sobre una ruta EN_CURSO responde 400: la trazabilidad no se borra
    @Test
    void testEliminarRuta_EnCurso_Devuelve400() {
        Ruta ruta = new Ruta();
        ruta.setId(1L);
        ruta.iniciar();

        when(ctx.pathParam("id")).thenReturn("1");
        when(rutaRepository.buscarPorId(1L)).thenReturn(Optional.of(ruta));
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        rutaController.delete(ctx);

        verify(rutaRepository, never()).eliminar(any());
        verify(ctx).status(HttpStatus.BAD_REQUEST);
    }
}
