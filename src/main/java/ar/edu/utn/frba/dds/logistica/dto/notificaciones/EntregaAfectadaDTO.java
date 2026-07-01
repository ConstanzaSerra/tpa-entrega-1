package ar.edu.utn.frba.dds.logistica.dto.notificaciones;

public class EntregaAfectadaDTO {
    public Long entregaId;
    public Long donacionId;
    public Long entidadId;

    public EntregaAfectadaDTO() {}

    public EntregaAfectadaDTO(Long entregaId, Long donacionId, Long entidadId) {
        this.entregaId = entregaId;
        this.donacionId = donacionId;
        this.entidadId = entidadId;
    }
}
