package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

import java.util.List;

public class RutaRequestDTO {
    public Long camionId;
    public List<ParadaRequestDTO> paradas;

    public RutaRequestDTO() {}

    public static class ParadaRequestDTO {
        public Long entidadBeneficiariaId;
        public String direccion;
        public List<Long> donacionesIds;

        public ParadaRequestDTO() {}
    }
}
