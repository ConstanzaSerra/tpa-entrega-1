package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

import java.time.LocalDateTime;

public class ConfirmarEntregaRequestDTO {
    public Long camionId;
    public LocalDateTime fechaHora;

    public ConfirmarEntregaRequestDTO() {}

    public ConfirmarEntregaRequestDTO(Long camionId, LocalDateTime fechaHora) {
        this.camionId = camionId;
        this.fechaHora = fechaHora;
    }
}
