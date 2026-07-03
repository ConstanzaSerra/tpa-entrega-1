package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;

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
