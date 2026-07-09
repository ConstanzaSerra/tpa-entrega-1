package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.dominio.PosicionCamion;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DashboardCamionDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PosicionCamionRequestDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.GpsRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GpsController {
    private final GpsRepository gpsRepository;
    private final CamionRepository camionRepository;
    private final RutaRepository rutaRepository;

    public GpsController(GpsRepository gpsRepository, CamionRepository camionRepository, RutaRepository rutaRepository) {
        this.gpsRepository = gpsRepository;
        this.camionRepository = camionRepository;
        this.rutaRepository = rutaRepository;
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
        if (request.velocidad != null && request.velocidad < 0) {
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
        Map<Long, PosicionCamion> posiciones = gpsRepository.obtenerTodasLasPosiciones();

        List<DashboardCamionDTO> dashboard = camionRepository.buscarTodos().stream()
                .map(camion -> {
                    PosicionCamion pos = posiciones.get(camion.getId());
                    Optional<Ruta> rutaActiva = rutaRepository.buscarTodas().stream()
                            .filter(r -> r.getEstado() == EstadoRuta.EN_CURSO
                                    && r.getCamion() != null
                                    && r.getCamion().getId().equals(camion.getId()))
                            .findFirst();
                    return DashboardCamionDTO.build(camion, pos, rutaActiva.orElse(null));
                })
                .toList();

        ctx.json(dashboard);
    }
}
