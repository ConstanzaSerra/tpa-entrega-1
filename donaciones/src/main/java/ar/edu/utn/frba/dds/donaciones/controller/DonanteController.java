package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.dto.DonanteDTO;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expone las operaciones REST sobre personas donantes (humanas y juridicas).
 */
public class DonanteController {

  private final DonanteRepository donanteRepository;

  public DonanteController(DonanteRepository donanteRepository) {
    this.donanteRepository = donanteRepository;
  }

  // GET /donantes
  public void listar(Context ctx) {
    List<DonanteDTO> dtos = donanteRepository.obtenerTodos().stream()
        .map(this::aDTO)
        .collect(Collectors.toList());
    ctx.json(dtos);
  }

  // GET /donantes/{id}
  public void obtenerPorId(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    donanteRepository.buscarPorId(id).ifPresentOrElse(
        d -> ctx.json(aDTO(d)),
        () -> ctx.status(404).json(Map.of("error", "No existe el donante " + id)));
  }

  // POST /donantes
  public void crear(Context ctx) {
    DonanteDTO dto = ctx.bodyAsClass(DonanteDTO.class);
    PersonaDonante donante;
    try {
      donante = aDominio(dto);
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", e.getMessage()));
      return;
    }
    if (dto.ultimaInteraccion != null) {
      try {
        donante.setUltimaInteraccion(LocalDate.parse(dto.ultimaInteraccion));
      } catch (DateTimeParseException e) {
        ctx.status(400).json(Map.of("error", "ultimaInteraccion debe ser una fecha ISO (yyyy-MM-dd)"));
        return;
      }
    }
    donanteRepository.guardar(donante);
    ctx.status(201).json(aDTO(donante));
  }

  // PATCH /donantes/{id}
  public void actualizar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    var existente = donanteRepository.buscarPorId(id);
    if (existente.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe el donante " + id));
      return;
    }
    DonanteDTO dto = ctx.bodyAsClass(DonanteDTO.class);
    PersonaDonante d = existente.get();
    try {
      aplicarCambios(d, dto);
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", e.getMessage()));
      return;
    }
    ctx.json(aDTO(d));
  }

  // DELETE /donantes/{id}
  public void eliminar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    boolean eliminado = donanteRepository.eliminar(id);
    if (eliminado) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("error", "No existe el donante " + id));
    }
  }

  private void aplicarCambios(PersonaDonante d, DonanteDTO dto) {
    if (dto.email != null) {
      d.actualizarEmail(dto.email);
    }
    if (dto.telefono != null) {
      d.actualizarTelefono(dto.telefono);
    }
    if (d instanceof PersonaHumana h) {
      if (dto.nombre != null) h.setNombre(dto.nombre);
      if (dto.apellido != null) h.setApellido(dto.apellido);
      if (dto.edad != null) h.setEdad(dto.edad);
      if (dto.dni != null) h.setDni(dto.dni);
      if (dto.genero != null) h.setGenero(dto.genero);
      if (dto.direccion != null) h.setDireccion(dto.direccion);
    } else if (d instanceof PersonaJuridica j) {
      if (dto.razonSocial != null) j.setRazonSocial(dto.razonSocial);
      if (dto.tipoJuridico != null) j.setTipo(TipoJuridico.valueOf(dto.tipoJuridico));
      if (dto.rubro != null) j.setRubro(dto.rubro);
    }
  }

  // ---------- mapeos dominio <-> DTO ----------

  private PersonaDonante aDominio(DonanteDTO dto) {
    if (dto.tipo == null) {
      throw new IllegalArgumentException("Falta el campo 'tipo' (HUMANA o JURIDICA)");
    }
    Email email = new Email(dto.email);
    Telefono telefono = new Telefono(dto.telefono);
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, telefono));

    switch (dto.tipo) {
      case "HUMANA":
        return new PersonaHumana(medios, email, dto.nombre, dto.apellido,
            dto.edad, dto.dni, dto.genero, dto.direccion);
      case "JURIDICA":
        TipoJuridico tj = dto.tipoJuridico != null ? TipoJuridico.valueOf(dto.tipoJuridico) : null;
        return new PersonaJuridica(medios, email, dto.razonSocial, tj, dto.rubro, new ArrayList<>());
      default:
        throw new IllegalArgumentException("Tipo invalido: " + dto.tipo);
    }
  }

  private DonanteDTO aDTO(PersonaDonante d) {
    DonanteDTO dto = new DonanteDTO();
    dto.id = d.getId();
    dto.email = valorDe(d, Email.class);
    dto.telefono = valorDe(d, Telefono.class);
    dto.ultimaInteraccion = d.getUltimaInteraccion() != null ? d.getUltimaInteraccion().toString() : null;

    if (d instanceof PersonaHumana h) {
      dto.tipo = "HUMANA";
      dto.nombre = h.getNombre();
      dto.apellido = h.getApellido();
      dto.edad = h.getEdad();
      dto.dni = h.getDni();
      dto.genero = h.getGenero();
      dto.direccion = h.getDireccion();
    } else if (d instanceof PersonaJuridica j) {
      dto.tipo = "JURIDICA";
      dto.razonSocial = j.getRazonSocial();
      dto.tipoJuridico = j.getTipo() != null ? j.getTipo().name() : null;
      dto.rubro = j.getRubro();
    }
    return dto;
  }

  private String valorDe(PersonaDonante d, Class<? extends MedioDeContacto> tipo) {
    return d.getMedioDeContactos().stream()
        .filter(tipo::isInstance)
        .map(MedioDeContacto::getValor)
        .findFirst()
        .orElse(null);
  }
}
