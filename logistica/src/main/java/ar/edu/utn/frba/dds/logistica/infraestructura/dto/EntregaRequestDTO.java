package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

public class EntregaRequestDTO {
    public Long donacionId;
    public Long entidadBeneficiariaId;
    public String direccionDestino;

    public EntregaRequestDTO() {}

    public EntregaRequestDTO(Long donacionId, Long entidadBeneficiariaId, String direccionDestino) {
        this.donacionId = donacionId;
        this.entidadBeneficiariaId = entidadBeneficiariaId;
        this.direccionDestino = direccionDestino;
    }
}
