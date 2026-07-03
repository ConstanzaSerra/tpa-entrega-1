package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.EstadoDonacion;
import ar.edu.utn.frba.dds.donaciones.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expone las operaciones REST sobre donaciones.
 */
public class DonacionController {

  private final DonacionRepository donacionRepository;

  public DonacionController(DonacionRepository donacionRepository) {
    this.donacionRepository = donacionRepository;
  }

  /**
   * GET /donaciones?estado=ASIGNACION_REALIZADA
   * Devuelve las donaciones en el estado pedido, mapeadas al DTO que consume Logistica.
   */
  public void listarPorEstado(Context ctx) {
    String estadoParam = ctx.queryParam("estado");

    if (estadoParam == null || estadoParam.isBlank()) {
      ctx.status(400).json(Map.of("error", "Falta el parametro 'estado'"));
      return;
    }

    EstadoDonacion estado;
    try {
      estado = EstadoDonacion.valueOf(estadoParam);
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Estado invalido: " + estadoParam));
      return;
    }

    List<DonacionParaRutaDTO> resultado = donacionRepository.buscarPorEstado(estado).stream()
        .filter(d -> d.getEntidadAsignada() != null)
        .map(d -> new DonacionParaRutaDTO(
            d.getId(),
            d.getEntidadAsignada().getId(),
            d.getEntidadAsignada().getDireccion()))
        .collect(Collectors.toList());

    ctx.json(resultado);
  }
}
