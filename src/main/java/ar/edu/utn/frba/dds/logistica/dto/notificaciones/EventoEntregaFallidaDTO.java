package ar.edu.utn.frba.dds.logistica.dto.notificaciones;

public class EventoEntregaFallidaDTO {
    public Long entregaId;
    public Long donacionId;
    public String motivo;

    public EventoEntregaFallidaDTO() {}

    public EventoEntregaFallidaDTO(Long entregaId, Long donacionId, String motivo) {
        this.entregaId = entregaId;
        this.donacionId = donacionId;
        this.motivo = motivo;
    }
}
