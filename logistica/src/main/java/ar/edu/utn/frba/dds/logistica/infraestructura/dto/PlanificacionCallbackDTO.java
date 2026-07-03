package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

import java.util.List;

public class PlanificacionCallbackDTO {
    public List<RutaArmadaDTO> rutas;
    public List<Long> donacionesNoAsignadas;

    public PlanificacionCallbackDTO() {}

    public static class RutaArmadaDTO {
        public Long camionId;
        public List<ParadaArmadaDTO> paradas;
        public RutaArmadaDTO() {}
    }

    public static class ParadaArmadaDTO {
        public Long entidadBeneficiariaId;
        public String direccion;
        public List<Long> donacionesIds;
        public ParadaArmadaDTO() {}
    }
}
