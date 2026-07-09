package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.dto.NecesidadDTO;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CRUD de necesidades materiales (recurrentes y extraordinarias) de una entidad beneficiaria.
 * Las necesidades viven dentro de la entidad (son parte de su agregado), por eso los endpoints
 * cuelgan de /entidades/{id}/necesidades.
 */
public class NecesidadController {

  private final EntidadRepository entidadRepository;
  private long proximoId = 1L;

  public NecesidadController(EntidadRepository entidadRepository) {
    this.entidadRepository = entidadRepository;
  }

  // POST /entidades/{id}/necesidades
  public void crear(Context ctx) {
    Optional<EntidadBeneficiaria> oEntidad = buscarEntidad(ctx);
    if (oEntidad.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la entidad " + ctx.pathParam("id")));
      return;
    }

    NecesidadDTO dto = ctx.bodyAsClass(NecesidadDTO.class);
    if (dto.tipo == null || dto.subcategoriaNombre == null || dto.categoria == null) {
      ctx.status(400).json(Map.of("error", "Faltan 'tipo', 'subcategoriaNombre' y/o 'categoria'"));
      return;
    }

    Necesidad necesidad;
    try {
      necesidad = aDominio(dto);
    } catch (RuntimeException e) {
      // Validaciones del dominio (categoria/cantidades/periodo)
      ctx.status(400).json(Map.of("error", e.getMessage()));
      return;
    }

    necesidad.setId(proximoId++);
    oEntidad.get().agregarNecesidad(necesidad);
    ctx.status(201).json(aDTO(necesidad));
  }

  // GET /entidades/{id}/necesidades
  public void listar(Context ctx) {
    Optional<EntidadBeneficiaria> oEntidad = buscarEntidad(ctx);
    if (oEntidad.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la entidad " + ctx.pathParam("id")));
      return;
    }
    List<NecesidadDTO> dtos = oEntidad.get().getNecesidades().stream()
        .map(this::aDTO)
        .collect(Collectors.toList());
    ctx.json(dtos);
  }

  // PATCH /entidades/{id}/necesidades/{nid}
  public void actualizar(Context ctx) {
    Optional<EntidadBeneficiaria> oEntidad = buscarEntidad(ctx);
    if (oEntidad.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la entidad " + ctx.pathParam("id")));
      return;
    }
    Long nid = Long.valueOf(ctx.pathParam("nid"));
    Optional<Necesidad> oNec = oEntidad.get().buscarNecesidad(nid);
    if (oNec.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la necesidad " + nid));
      return;
    }
    NecesidadDTO dto = ctx.bodyAsClass(NecesidadDTO.class);
    Necesidad necesidad = oNec.get();
    if (dto.descripcion != null) {
      necesidad.setDescripcion(dto.descripcion);
    }
    ctx.json(aDTO(necesidad));
  }

  // DELETE /entidades/{id}/necesidades/{nid}
  public void eliminar(Context ctx) {
    Optional<EntidadBeneficiaria> oEntidad = buscarEntidad(ctx);
    if (oEntidad.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la entidad " + ctx.pathParam("id")));
      return;
    }
    Long nid = Long.valueOf(ctx.pathParam("nid"));
    boolean eliminado = oEntidad.get().eliminarNecesidad(nid);
    if (eliminado) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("error", "No existe la necesidad " + nid));
    }
  }

  // ---------- helpers / mapeos ----------

  private Optional<EntidadBeneficiaria> buscarEntidad(Context ctx) {
    return entidadRepository.buscarPorId(Long.valueOf(ctx.pathParam("id")));
  }

  private Necesidad aDominio(NecesidadDTO dto) {
    Categoria categoria = Categoria.valueOf(dto.categoria);
    Subcategoria subcategoria = new Subcategoria(dto.subcategoriaNombre, categoria, false, false);
    int recibida = dto.cantidadRecibida != null ? dto.cantidadRecibida : 0;

    switch (dto.tipo) {
      case "RECURRENTE":
        PeriodoConsumo periodo = new PeriodoConsumo(dto.cantidadObjetivo, dto.periodoDescripcion, recibida);
        return new NecesidadRecurrente(subcategoria, dto.descripcion, periodo);
      case "EXTRAORDINARIA":
        return new NecesidadExtraordinaria(subcategoria, dto.descripcion, dto.cantidadRequerida, recibida);
      default:
        throw new IllegalArgumentException("Tipo invalido: " + dto.tipo + " (RECURRENTE | EXTRAORDINARIA)");
    }
  }

  private NecesidadDTO aDTO(Necesidad n) {
    NecesidadDTO dto = new NecesidadDTO();
    dto.id = n.getId();
    dto.descripcion = n.getDescripcion();
    dto.satisfecha = n.estaSatisfecha();

    Subcategoria s = n.getSubcategoria();
    if (s != null) {
      dto.subcategoriaNombre = s.getNombre();
      dto.categoria = s.getCategoria() != null ? s.getCategoria().name() : null;
    }

    if (n instanceof NecesidadRecurrente r) {
      dto.tipo = "RECURRENTE";
      PeriodoConsumo p = r.getPeriodoConsumo();
      dto.cantidadObjetivo = p.getCantidadObjetivo();
      dto.periodoDescripcion = p.getDescripcion();
      dto.cantidadRecibida = p.getCantidadRecibida();
    } else if (n instanceof NecesidadExtraordinaria e) {
      dto.tipo = "EXTRAORDINARIA";
      dto.cantidadRequerida = e.getCantidadRequerida();
      dto.cantidadRecibida = e.getCantidadRecibida();
    }
    return dto;
  }
}
