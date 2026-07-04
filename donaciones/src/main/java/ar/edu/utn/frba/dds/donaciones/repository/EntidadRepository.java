package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntidadRepository {
  private final List<EntidadBeneficiaria> entidades = new ArrayList<>();
  private Long proximoId = 1L;

  public void guardar(EntidadBeneficiaria entidad) {
    if (entidad.getId() == null) {
      entidad.setId(proximoId++);
    }
    this.entidades.add(entidad);
  }

  public Optional<EntidadBeneficiaria> buscarPorId(Long id) {
    return entidades.stream()
        .filter(e -> id.equals(e.getId()))
        .findFirst();
  }

  public List<EntidadBeneficiaria> obtenerTodas() {
    return new ArrayList<>(entidades);
  }

  public boolean eliminar(Long id) {
    return entidades.removeIf(e -> id.equals(e.getId()));
  }
}
