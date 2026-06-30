package ar.edu.utn.frba.dds.logistica.dto;

import java.util.List;

public class DonacionParaRutaDTO {
    public Long donacionId;
    public Long entidadBeneficiariaId;
    public String direccionDestino;

    public DonacionParaRutaDTO() {}

    public DonacionParaRutaDTO(Long donacionId, Long entidadBeneficiariaId, String direccionDestino) {
        this.donacionId = donacionId;
        this.entidadBeneficiariaId = entidadBeneficiariaId;
        this.direccionDestino = direccionDestino;
    }
}
