package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

public class RechazarEntregaRequestDTO {
    public String motivo;

    public RechazarEntregaRequestDTO() {}

    public RechazarEntregaRequestDTO(String motivo) {
        this.motivo = motivo;
    }
}
