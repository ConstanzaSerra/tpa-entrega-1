package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.PosicionCamion;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PosicionCamionRequestDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.GpsRepository;
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

    // Caso feliz: posicion valida se guarda correctamente con los datos del camion y coordenadas
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

    // Si el camion no esta registrado en la flota, no se guarda ninguna posicion y se responde 404
    @Test
    void testReportarPosicion_CamionNoExiste_Devuelve404() {
        when(ctx.pathParam("id")).thenReturn("99");
        when(camionRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(ctx.status(HttpStatus.NOT_FOUND)).thenReturn(ctx);

        gpsController.reportarPosicion(ctx);

        verify(gpsRepository, never()).guardarPosicion(any());
        verify(ctx).status(HttpStatus.NOT_FOUND);
    }

    // Longitud fuera del rango [-180, 180] debe rechazarse con 400 sin guardar nada
    @Test
    void testReportarPosicion_LongitudInvalida() {
        when(ctx.pathParam("id")).thenReturn("1");
        when(camionRepository.buscarPorId(1L)).thenReturn(Optional.of(new Camion("ABC", 1, 1, 1)));

        PosicionCamionRequestDTO dto = new PosicionCamionRequestDTO(-34.6, 200.0, 60.0, Instant.now());
        when(ctx.bodyAsClass(PosicionCamionRequestDTO.class)).thenReturn(dto);
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        gpsController.reportarPosicion(ctx);

        verify(gpsRepository, never()).guardarPosicion(any());
        verify(ctx).status(HttpStatus.BAD_REQUEST);
    }

    // Velocidad negativa no tiene sentido fisico; debe rechazarse con 400 sin guardar nada
    @Test
    void testReportarPosicion_VelocidadNegativa() {
        when(ctx.pathParam("id")).thenReturn("1");
        when(camionRepository.buscarPorId(1L)).thenReturn(Optional.of(new Camion("ABC", 1, 1, 1)));

        PosicionCamionRequestDTO dto = new PosicionCamionRequestDTO(-34.6, -58.4, -10.0, Instant.now());
        when(ctx.bodyAsClass(PosicionCamionRequestDTO.class)).thenReturn(dto);
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        gpsController.reportarPosicion(ctx);

        verify(gpsRepository, never()).guardarPosicion(any());
        verify(ctx).status(HttpStatus.BAD_REQUEST);
    }

    // Sin timestamp no se puede saber cuándo se reportó la posicion; debe rechazarse con 400
    @Test
    void testReportarPosicion_TimestampNulo() {
        when(ctx.pathParam("id")).thenReturn("1");
        when(camionRepository.buscarPorId(1L)).thenReturn(Optional.of(new Camion("ABC", 1, 1, 1)));

        PosicionCamionRequestDTO dto = new PosicionCamionRequestDTO(-34.6, -58.4, 60.0, null);
        when(ctx.bodyAsClass(PosicionCamionRequestDTO.class)).thenReturn(dto);
        when(ctx.status(HttpStatus.BAD_REQUEST)).thenReturn(ctx);

        gpsController.reportarPosicion(ctx);

        verify(gpsRepository, never()).guardarPosicion(any());
        verify(ctx).status(HttpStatus.BAD_REQUEST);
    }

    // Latitud fuera del rango [-90, 90] debe rechazarse con 400 sin guardar nada
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
