package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;

import java.util.ArrayList;
import java.util.List;

public class PropuestaRepository {
  private List<PropuestaMatchmaking> propuestas = new ArrayList<>();

  public void guardar(PropuestaMatchmaking propuesta) {
    this.propuestas.add(propuesta);
  }

  public List<PropuestaMatchmaking> obtenerTodas() {
    return propuestas;
  }
}
