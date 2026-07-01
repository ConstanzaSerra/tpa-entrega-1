package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.PosicionCamion;
import ar.edu.utn.frba.dds.logistica.dto.PosicionCamionRequestDTO;
import ar.edu.utn.frba.dds.logistica.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.repository.GpsRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class GpsController {
    private final GpsRepository gpsRepository;
    private final CamionRepository camionRepository;

    public GpsController(GpsRepository gpsRepository, CamionRepository camionRepository) {
        this.gpsRepository = gpsRepository;
        this.camionRepository = camionRepository;
    }

    public void reportarPosicion(Context ctx) {
        Long camionId = Long.parseLong(ctx.pathParam("id"));
        
        if (camionRepository.buscarPorId(camionId).isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
            return;
        }

        PosicionCamionRequestDTO request = ctx.bodyAsClass(PosicionCamionRequestDTO.class);
        
        if (request.latitud < -90 || request.latitud > 90) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Latitud invalida");
            return;
        }
        if (request.longitud < -180 || request.longitud > 180) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Longitud invalida");
            return;
        }
        if (request.velocidad < 0) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Velocidad invalida");
            return;
        }
        if (request.timestamp == null) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Falta timestamp");
            return;
        }

        PosicionCamion posicion = new PosicionCamion(
                camionId, 
                request.latitud, 
                request.longitud, 
                request.velocidad, 
                request.timestamp
        );
        
        gpsRepository.guardarPosicion(posicion);
        ctx.status(HttpStatus.OK).result("Posicion registrada");
    }

    public void getDashboard(Context ctx) {
        // En un caso de uso real esto devolveria quizas un DTO de Dashboard
        // Pero devolver el mapa directamente sirve para exponerlo como JSON en esta entrega.
        ctx.json(gpsRepository.obtenerTodasLasPosiciones());
    }
}
