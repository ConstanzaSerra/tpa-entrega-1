package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.Entrega;
import ar.edu.utn.frba.dds.logistica.domain.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.domain.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.domain.Ruta;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.repository.RutaRepository;
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
