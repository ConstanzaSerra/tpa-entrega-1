package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PropuestaRepository {
  private List<PropuestaMatchmaking> propuestas = new ArrayList<>();

  public void guardar(PropuestaMatchmaking propuesta) {
    this.propuestas.add(propuesta);
  }

  public List<PropuestaMatchmaking> obtenerTodas() {
    return propuestas;
  }

  // Devuelve la ultima propuesta generada para una donacion, si existe.
  public Optional<PropuestaMatchmaking> buscarPorDonacion(Long donacionId) {
    PropuestaMatchmaking encontrada = null;
    for (PropuestaMatchmaking p : propuestas) {
      if (p.getDonacion() != null && donacionId.equals(p.getDonacion().getId())) {
        encontrada = p;
      }
    }
    return Optional.ofNullable(encontrada);
  }
}
