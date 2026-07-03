package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DonacionRepository {
  private List<Donacion> donaciones = new ArrayList<>();

  public void guardar(Donacion donacion) {
    this.donaciones.add(donacion);
  }

  public List<Donacion> obtenerDonacionesEnDeposito() {
    return donaciones.stream()
        .filter(d -> d.getEstado() == EstadoDonacion.EN_DEPOSITO)
        .collect(Collectors.toList());
  }

  public List<Donacion> obtenerTodas() {
    return new ArrayList<>(donaciones);
  }
}
