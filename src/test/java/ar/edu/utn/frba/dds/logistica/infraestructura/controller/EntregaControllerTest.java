package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.ConfirmarEntregaRequestDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.RechazarEntregaRequestDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EntregaControllerTest {
    private EntregaRepository entregaRepository;
    private CamionRepository camionRepository;
    private DonacionesAPI donacionesAPI;
    private EntregaController entregaController;
    private Context ctx;

    @BeforeEach
    void setUp() {
        entregaRepository = mock(EntregaRepository.class);
        camionRepository = mock(CamionRepository.class);
        donacionesAPI = mock(DonacionesAPI.class);
        entregaController = new EntregaController(entregaRepository, camionRepository, donacionesAPI);
        ctx = mock(Context.class);
    }

    // Caso feliz: confirmar una entrega EN_TRASLADO la pasa a ENTREGADA y notifica a Donaciones con la patente del camion
    @Test
    void testConfirmarEntrega_NotificaYCambiaEstado() {
        Entrega entrega = new Entrega(10L, 20L);
        entrega.setId(1L);
        entrega.iniciarTraslado(); // Debe estar en traslado para confirmar

        Camion camion = new Camion("ABC1234", 1, 1, 1);
        camion.setId(5L);

        when(ctx.pathParam("id")).thenReturn("1");
        ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO(5L, LocalDateTime.now());
        when(ctx.bodyAsClass(ConfirmarEntregaRequestDTO.class)).thenReturn(dto);
        when(entregaRepository.buscarPorId(1L)).thenReturn(Optional.of(entrega));
        when(camionRepository.buscarPorId(5L)).thenReturn(Optional.of(camion));
        when(ctx.status(HttpStatus.OK)).thenReturn(ctx);

        entregaController.confirmar(ctx);

        assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
        
        ArgumentCaptor<EventoEntregaConfirmadaDTO> captor = ArgumentCaptor.forClass(EventoEntregaConfirmadaDTO.class);
        verify(donacionesAPI, times(1)).notificarEntregaConfirmada(captor.capture());
        assertEquals("ABC1234", captor.getValue().patenteCamion);
    }

    // Si la entrega no existe, debe responder 404 y no notificar a Donaciones
    @Test
    void testConfirmarEntrega_EntregaNoEncontrada_Devuelve404() {
        when(ctx.pathParam("id")).thenReturn("99");
        when(entregaRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(ctx.status(HttpStatus.NOT_FOUND)).thenReturn(ctx);

        entregaController.confirmar(ctx);

        verify(ctx).status(HttpStatus.NOT_FOUND);
        verify(donacionesAPI, never()).notificarEntregaConfirmada(any());
    }

    // Si el camion indicado no existe en la flota, debe responder 404 y no notificar a Donaciones
    @Test
    void testConfirmarEntrega_CamionNoEncontrado_Devuelve404() {
        Entrega entrega = new Entrega(10L, 20L);
        entrega.setId(1L);
        entrega.iniciarTraslado();

        when(ctx.pathParam("id")).thenReturn("1");
        ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO(99L, LocalDateTime.now());
        when(ctx.bodyAsClass(ConfirmarEntregaRequestDTO.class)).thenReturn(dto);
        when(entregaRepository.buscarPorId(1L)).thenReturn(Optional.of(entrega));
        when(camionRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(ctx.status(HttpStatus.NOT_FOUND)).thenReturn(ctx);

        entregaController.confirmar(ctx);

        verify(ctx).status(HttpStatus.NOT_FOUND);
        verify(donacionesAPI, never()).notificarEntregaConfirmada(any());
    }

    // No se puede confirmar una entrega que no esta EN_TRASLADO (ej: PENDIENTE); debe responder 400
    @Test
    void testConfirmarEntrega_EstadoInvalido_Devuelve400() {
        Entrega entrega = new Entrega(10L, 20L);
        entrega.setId(1L);
        // queda en PENDIENTE, no EN_TRASLADO → confirmar debe fallar

        Camion camion = new Camion("ABC1234", 1, 1, 1);
        camion.setId(5L);

        when(ctx.pathParam("id")).thenReturn("1");
        ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO(5L, LocalDateTime.now());
        when(ctx.bodyAsClass(ConfirmarEntregaRequestDTO.class)).thenReturn(dto);
        when(entregaRepository.buscarPorId(1L)).thenReturn(Optional.of(entrega));
        when(camionRepository.buscarPorId(5L)).thenReturn(Optional.of(camion));
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        entregaController.confirmar(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(donacionesAPI, never()).notificarEntregaConfirmada(any());
    }

    // Caso feliz: rechazar una entrega EN_TRASLADO la pasa a NO_RECIBIDA y notifica a Donaciones con el motivo
    @Test
    void testRechazarEntrega_NotificaYCambiaEstado() {
        Entrega entrega = new Entrega(10L, 20L);
        entrega.setId(1L);
        entrega.iniciarTraslado();

        when(ctx.pathParam("id")).thenReturn("1");
        RechazarEntregaRequestDTO dto = new RechazarEntregaRequestDTO("No habia nadie");
        when(ctx.bodyAsClass(RechazarEntregaRequestDTO.class)).thenReturn(dto);
        when(entregaRepository.buscarPorId(1L)).thenReturn(Optional.of(entrega));
        when(ctx.status(HttpStatus.OK)).thenReturn(ctx);

        entregaController.rechazar(ctx);

        assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado());
        
        ArgumentCaptor<EventoEntregaFallidaDTO> captor = ArgumentCaptor.forClass(EventoEntregaFallidaDTO.class);
        verify(donacionesAPI, times(1)).notificarEntregaFallida(captor.capture());
        assertEquals("No habia nadie", captor.getValue().motivo);
    }
}
