package ar.edu.utn.frba.dds.logistica.dto.notificaciones;

import java.time.LocalDateTime;

public class EventoEntregaConfirmadaDTO {
    public Long entregaId;
    public Long donacionId;
    public String patenteCamion;
    public LocalDateTime fechaHora;

    public EventoEntregaConfirmadaDTO() {}

    public EventoEntregaConfirmadaDTO(Long entregaId, Long donacionId, String patenteCamion, LocalDateTime fechaHora) {
        this.entregaId = entregaId;
        this.donacionId = donacionId;
        this.patenteCamion = patenteCamion;
        this.fechaHora = fechaHora;
    }
}
