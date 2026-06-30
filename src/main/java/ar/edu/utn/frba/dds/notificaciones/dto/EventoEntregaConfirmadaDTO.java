package ar.edu.utn.frba.dds.notificaciones.dto;

import java.time.LocalDateTime;

public record EventoEntregaConfirmadaDTO(
    Long entregaId,
    Long donacionId,
    Long entidadBeneficiariaId,
    String patenteCamion,
    LocalDateTime fechaHoraEntrega
) {}
