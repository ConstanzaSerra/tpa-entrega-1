package ar.edu.utn.frba.dds.notificaciones.dto;

public record EntregaAfectadaDTO(
    Long entregaId,
    Long donacionId,
    Long entidadBeneficiariaId
) {}
