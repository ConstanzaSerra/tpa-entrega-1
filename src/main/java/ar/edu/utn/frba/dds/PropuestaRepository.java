package ar.edu.utn.frba.dds;

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
