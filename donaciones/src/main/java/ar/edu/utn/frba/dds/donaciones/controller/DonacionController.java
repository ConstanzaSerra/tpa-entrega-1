package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.dto.AsignarEntidadDTO;
import ar.edu.utn.frba.dds.donaciones.dto.CambioEstadoDTO;
import ar.edu.utn.frba.dds.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.dds.donaciones.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expone las operaciones REST sobre donaciones.
 */
public class DonacionController {

  private final DonacionRepository donacionRepository;
  private final DonanteRepository donanteRepository;
  private final EntidadRepository entidadRepository;

  public DonacionController(DonacionRepository donacionRepository,
                            DonanteRepository donanteRepository,
                            EntidadRepository entidadRepository) {
    this.donacionRepository = donacionRepository;
    this.donanteRepository = donanteRepository;
    this.entidadRepository = entidadRepository;
  }

  /**
   * GET /donaciones?estado=ASIGNACION_REALIZADA
   * Vista reducida que consume Logistica (contrato entre microservicios).
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

  // POST /donaciones
  public void crear(Context ctx) {
    DonacionDTO dto = ctx.bodyAsClass(DonacionDTO.class);

    if (dto.donanteId == null) {
      ctx.status(400).json(Map.of("error", "Falta 'donanteId'"));
      return;
    }
    var oDonante = donanteRepository.buscarPorId(dto.donanteId);
    if (oDonante.isEmpty()) {
      ctx.status(400).json(Map.of("error", "No existe el donante " + dto.donanteId));
      return;
    }
    if (dto.subcategoriaNombre == null || dto.categoria == null) {
      ctx.status(400).json(Map.of("error", "Faltan 'subcategoriaNombre' y/o 'categoria'"));
      return;
    }

    Categoria categoria;
    try {
      categoria = Categoria.valueOf(dto.categoria);
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Categoria invalida: " + dto.categoria));
      return;
    }

    Subcategoria subcategoria = new Subcategoria(dto.subcategoriaNombre, categoria, false, false);
    int cantidad = dto.cantidad != null ? dto.cantidad : 0;

    oDonante.get().registrarInteraccion(); // crear una donacion cuenta como interaccion
    Donacion donacion = new Donacion(subcategoria, cantidad, dto.unidadMedida, oDonante.get(), LocalDate.now());
    donacionRepository.guardar(donacion);
    ctx.status(201).json(aDTO(donacion));
  }

  // GET /donaciones/{id}
  public void obtenerPorId(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    donacionRepository.buscarPorId(id).ifPresentOrElse(
        d -> ctx.json(aDTO(d)),
        () -> ctx.status(404).json(Map.of("error", "No existe la donacion " + id)));
  }

  // PATCH /donaciones/{id}  (actualizacion parcial: no cambia estado ni donante)
  public void actualizar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    var existente = donacionRepository.buscarPorId(id);
    if (existente.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la donacion " + id));
      return;
    }
    DonacionDTO dto = ctx.bodyAsClass(DonacionDTO.class);
    Donacion d = existente.get();

    // Cambio de estado: lo usa Logistica al planificar (PATCH {"estado":"LISTA_PARA_ENTREGAR"}).
    // No setea el campo directamente: canaliza la transicion validada del dominio.
    if (dto.estado != null) {
      EstadoDonacion objetivo;
      try {
        objetivo = EstadoDonacion.valueOf(dto.estado);
      } catch (IllegalArgumentException e) {
        ctx.status(400).json(Map.of("error", "Estado invalido: " + dto.estado));
        return;
      }
      try {
        d.avanzarHacia(objetivo);
      } catch (IllegalStateException e) {
        ctx.status(409).json(Map.of("error", e.getMessage()));
        return;
      }
    }

    try {
      if (dto.cantidad != null) {
        d.setCantidad(dto.cantidad);
      }
      if (dto.unidadMedida != null) {
        d.setUnidadMedida(dto.unidadMedida);
      }
      if (dto.subcategoriaNombre != null && dto.categoria != null) {
        d.setSubcategoria(new Subcategoria(dto.subcategoriaNombre, Categoria.valueOf(dto.categoria), false, false));
      }
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Categoria invalida: " + dto.categoria));
      return;
    }
    ctx.json(aDTO(d));
  }

  // DELETE /donaciones/{id}
  public void eliminar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    boolean eliminado = donacionRepository.eliminar(id);
    if (eliminado) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("error", "No existe la donacion " + id));
    }
  }

  // GET /donaciones/{id}/historial  (trazabilidad de estados)
  public void historial(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    donacionRepository.buscarPorId(id).ifPresentOrElse(
        d -> {
          List<CambioEstadoDTO> historial = d.getHistorial().stream()
              .map(c -> new CambioEstadoDTO(c.getEstado().name(), c.getFechaHora().toString()))
              .collect(Collectors.toList());
          ctx.json(historial);
        },
        () -> ctx.status(404).json(Map.of("error", "No existe la donacion " + id)));
  }

  // POST /donaciones/{id}/asignacion  (accion de negocio: asignar destino)
  public void asignar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    var oDonacion = donacionRepository.buscarPorId(id);
    if (oDonacion.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la donacion " + id));
      return;
    }
    AsignarEntidadDTO dto = ctx.bodyAsClass(AsignarEntidadDTO.class);
    if (dto.entidadBeneficiariaId == null) {
      ctx.status(400).json(Map.of("error", "Falta 'entidadBeneficiariaId'"));
      return;
    }
    var oEntidad = entidadRepository.buscarPorId(dto.entidadBeneficiariaId);
    if (oEntidad.isEmpty()) {
      ctx.status(400).json(Map.of("error", "No existe la entidad " + dto.entidadBeneficiariaId));
      return;
    }
    try {
      oDonacion.get().asignarEntidad(oEntidad.get());
    } catch (IllegalStateException e) {
      // La donacion no estaba en un estado que permita la asignacion
      ctx.status(409).json(Map.of("error", e.getMessage()));
      return;
    }
    ctx.json(aDTO(oDonacion.get()));
  }

  // ---------- mapeo dominio -> DTO ----------

  private DonacionDTO aDTO(Donacion d) {
    DonacionDTO dto = new DonacionDTO();
    dto.id = d.getId();
    dto.estado = d.getEstado() != null ? d.getEstado().name() : null;
    dto.cantidad = d.getCantidad();
    dto.unidadMedida = d.getUnidadMedida();
    dto.fechaRegistro = d.getFechaRegistro() != null ? d.getFechaRegistro().toString() : null;
    dto.donanteId = d.getDonante() != null ? d.getDonante().getId() : null;

    Subcategoria s = d.getSubcategoria();
    if (s != null) {
      dto.subcategoriaNombre = s.getNombre();
      dto.categoria = s.getCategoria() != null ? s.getCategoria().name() : null;
    }
    dto.entidadAsignadaId = d.getEntidadAsignada() != null ? d.getEntidadAsignada().getId() : null;
    return dto;
  }
}
