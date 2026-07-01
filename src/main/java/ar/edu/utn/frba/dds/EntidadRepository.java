package ar.edu.utn.frba.dds;

import java.util.ArrayList;
import java.util.List;

public class EntidadRepository {
  private List<EntidadBeneficiaria> entidades = new ArrayList<>();

  public void guardar(EntidadBeneficiaria entidad) {
    this.entidades.add(entidad);
  }

  public List<EntidadBeneficiaria> obtenerTodas() {
    return new ArrayList<>(entidades);
  }
}
