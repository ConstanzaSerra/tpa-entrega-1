package ar.edu.utn.frba.dds.logistica.puertos;

import ar.edu.utn.frba.dds.logistica.dto.PlanificacionRequestDTO;

public interface PlanificadorExternoAPI {
    void solicitarPlanificacion(PlanificacionRequestDTO request);
}
