package ar.edu.utn.frba.dds.logistica.dto;

import java.util.List;

public class PlanificacionRequestDTO {
    public String callbackUrl;
    public List<DonacionParaRutaDTO> donaciones;
    public List<CamionDTO> camionesDisponibles;

    public PlanificacionRequestDTO() {}

    public PlanificacionRequestDTO(String callbackUrl, List<DonacionParaRutaDTO> donaciones, List<CamionDTO> camionesDisponibles) {
        this.callbackUrl = callbackUrl;
        this.donaciones = donaciones;
        this.camionesDisponibles = camionesDisponibles;
    }
}
