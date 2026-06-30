package ar.edu.utn.frba.dds.notificaciones.dto;

import java.util.List;

public record EventoInicioRutaDTO(
    Long rutaId,
    String patenteCamion,
    String linkMapa,
    List<EntregaAfectadaDTO> entregas
) {}
