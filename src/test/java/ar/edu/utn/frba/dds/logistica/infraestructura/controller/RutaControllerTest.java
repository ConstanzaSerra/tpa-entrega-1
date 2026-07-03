package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
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
    private DonacionesAPI donacionesAPI;
    private RutaController rutaController;
    private Context ctx;

    @BeforeEach
    void setUp() {
        rutaRepository = mock(RutaRepository.class);
        donacionesAPI = mock(DonacionesAPI.class);
        rutaController = new RutaController(rutaRepository, donacionesAPI);
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
}
