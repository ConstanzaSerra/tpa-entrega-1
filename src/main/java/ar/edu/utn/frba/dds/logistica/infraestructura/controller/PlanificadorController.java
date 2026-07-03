package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.aplicacion.service.PlanificadorRutasService;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionCallbackDTO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class PlanificadorController {
    private final PlanificadorRutasService planificadorService;

    public PlanificadorController(PlanificadorRutasService planificadorService) {
        this.planificadorService = planificadorService;
    }

    public void recibirCallback(Context ctx) { //eventualmente cuando el servicio externo me responda
        PlanificacionCallbackDTO callbackData = ctx.bodyAsClass(PlanificacionCallbackDTO.class);
        planificadorService.registrarResultado(callbackData);
        ctx.status(HttpStatus.OK).result("Planificación procesada correctamente.");
    }
}
