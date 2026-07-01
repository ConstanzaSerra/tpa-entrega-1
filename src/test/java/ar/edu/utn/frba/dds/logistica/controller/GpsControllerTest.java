package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.Camion;
import ar.edu.utn.frba.dds.logistica.domain.PosicionCamion;
import ar.edu.utn.frba.dds.logistica.dto.PosicionCamionRequestDTO;
import ar.edu.utn.frba.dds.logistica.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.repository.GpsRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GpsControllerTest {

    private GpsRepository gpsRepository;
    private CamionRepository camionRepository;
    private GpsController gpsController;
    private Context ctx;

    @BeforeEach
    void setUp() {
        gpsRepository = mock(GpsRepository.class);
        camionRepository = mock(CamionRepository.class);
        gpsController = new GpsController(gpsRepository, camionRepository);
        ctx = mock(Context.class);
    }

    @Test
    void testReportarPosicion_Exito() {
        when(ctx.pathParam("id")).thenReturn("1");
        
        Camion camion = new Camion("ABC", 1, 1, 1);
        when(camionRepository.buscarPorId(1L)).thenReturn(Optional.of(camion));
        
        PosicionCamionRequestDTO dto = new PosicionCamionRequestDTO(-34.6, -58.4, 60.5, Instant.now());
        when(ctx.bodyAsClass(PosicionCamionRequestDTO.class)).thenReturn(dto);
        when(ctx.status(HttpStatus.OK)).thenReturn(ctx);

        gpsController.reportarPosicion(ctx);

        ArgumentCaptor<PosicionCamion> captor = ArgumentCaptor.forClass(PosicionCamion.class);
        verify(gpsRepository, times(1)).guardarPosicion(captor.capture());
        
        PosicionCamion guardada = captor.getValue();
        assertEquals(1L, guardada.getCamionId());
        assertEquals(-34.6, guardada.getLatitud());
        assertEquals(60.5, guardada.getVelocidad());
        verify(ctx).status(HttpStatus.OK);
    }

    @Test
    void testReportarPosicion_LatitudInvalida() {
        when(ctx.pathParam("id")).thenReturn("1");
        
        Camion camion = new Camion("ABC", 1, 1, 1);
        when(camionRepository.buscarPorId(1L)).thenReturn(Optional.of(camion));
        
        PosicionCamionRequestDTO dto = new PosicionCamionRequestDTO(100.0, -58.4, 60.5, Instant.now());
        when(ctx.bodyAsClass(PosicionCamionRequestDTO.class)).thenReturn(dto);
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        gpsController.reportarPosicion(ctx);

        verify(gpsRepository, never()).guardarPosicion(any());
        verify(ctx).status(HttpStatus.BAD_REQUEST);
    }
}
