package ar.edu.utn.frba.dds.notificaciones.dto;

import java.time.LocalDateTime;

public record EventoEntregaFallidaDTO(
    Long entregaId,
    Long donacionId,
    Long entidadBeneficiariaId,
    String motivo,
    LocalDateTime fechaHoraIntento
) {}
