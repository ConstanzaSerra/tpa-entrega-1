package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DonacionRepository {
  private final List<Donacion> donaciones = new ArrayList<>();
  private Long proximoId = 1L;

  public void guardar(Donacion donacion) {
    if (donacion.getId() == null) {
      donacion.setId(proximoId++);
    }
    this.donaciones.add(donacion);
  }

  public List<Donacion> buscarPorEstado(EstadoDonacion estado) {
    return donaciones.stream()
        .filter(d -> d.getEstado() == estado)
        .collect(Collectors.toList());
  }

  public Optional<Donacion> buscarPorId(Long id) {
    return donaciones.stream()
        .filter(d -> id.equals(d.getId()))
        .findFirst();
  }

  public List<Donacion> obtenerDonacionesEnDeposito() {
    return buscarPorEstado(EstadoDonacion.EN_DEPOSITO);
  }

  public List<Donacion> obtenerTodas() {
    return new ArrayList<>(donaciones);
  }

  public boolean eliminar(Long id) {
    return donaciones.removeIf(d -> id.equals(d.getId()));
  }
}
