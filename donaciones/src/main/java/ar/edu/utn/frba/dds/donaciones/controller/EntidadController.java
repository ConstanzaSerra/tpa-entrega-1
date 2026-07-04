package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expone las operaciones REST sobre entidades beneficiarias.
 * (Por ahora crear y leer; actualizar/eliminar quedan para completar la tarea #2.)
 */
public class EntidadController {

  private final EntidadRepository entidadRepository;

  public EntidadController(EntidadRepository entidadRepository) {
    this.entidadRepository = entidadRepository;
  }

  // POST /entidades
  public void crear(Context ctx) {
    EntidadBeneficiariaDTO dto = ctx.bodyAsClass(EntidadBeneficiariaDTO.class);
    if (dto.razonSocial == null || dto.direccion == null) {
      ctx.status(400).json(Map.of("error", "Faltan 'razonSocial' y/o 'direccion'"));
      return;
    }
    List<String> emails = dto.emailsRepresentantes != null ? dto.emailsRepresentantes : new ArrayList<>();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        dto.razonSocial, dto.direccion, dto.telefono, emails, new ArrayList<>());
    entidadRepository.guardar(entidad);
    ctx.status(201).json(aDTO(entidad));
  }

  // GET /entidades
  public void listar(Context ctx) {
    List<EntidadBeneficiariaDTO> dtos = entidadRepository.obtenerTodas().stream()
        .map(this::aDTO)
        .collect(Collectors.toList());
    ctx.json(dtos);
  }

  // GET /entidades/{id}
  public void obtenerPorId(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    entidadRepository.buscarPorId(id).ifPresentOrElse(
        e -> ctx.json(aDTO(e)),
        () -> ctx.status(404).json(Map.of("error", "No existe la entidad " + id)));
  }

  // PATCH /entidades/{id}  (actualizacion parcial)
  public void actualizar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    var existente = entidadRepository.buscarPorId(id);
    if (existente.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la entidad " + id));
      return;
    }
    EntidadBeneficiariaDTO dto = ctx.bodyAsClass(EntidadBeneficiariaDTO.class);
    EntidadBeneficiaria entidad = existente.get();
    if (dto.razonSocial != null) {
      entidad.setRazonSocial(dto.razonSocial);
    }
    if (dto.direccion != null) {
      entidad.setDireccion(dto.direccion);
    }
    if (dto.telefono != null) {
      entidad.setTelefono(dto.telefono);
    }
    if (dto.emailsRepresentantes != null) {
      entidad.setEmailsRepresentantes(dto.emailsRepresentantes);
    }
    ctx.json(aDTO(entidad));
  }

  // DELETE /entidades/{id}
  public void eliminar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    boolean eliminado = entidadRepository.eliminar(id);
    if (eliminado) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("error", "No existe la entidad " + id));
    }
  }

  private EntidadBeneficiariaDTO aDTO(EntidadBeneficiaria e) {
    EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
    dto.id = e.getId();
    dto.razonSocial = e.getRazonSocial();
    dto.direccion = e.getDireccion();
    dto.telefono = e.getTelefono();
    dto.emailsRepresentantes = e.getEmailsRepresentantes();
    return dto;
  }
}
