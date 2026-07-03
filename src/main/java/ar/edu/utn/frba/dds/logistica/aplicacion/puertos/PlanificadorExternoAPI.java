package ar.edu.utn.frba.dds.logistica.aplicacion.puertos;

import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionRequestDTO;

public interface PlanificadorExternoAPI {
    void solicitarPlanificacion(PlanificacionRequestDTO request);
}
