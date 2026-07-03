package ar.edu.utn.frba.dds.donaciones.matchmaking;

import ar.edu.utn.frba.dds.donaciones.domain.*;

import java.util.List;
import java.util.stream.Collectors;

public class PrioridadSubatendidos implements AlgoritmoDonacion{

  public List<EntidadBeneficiaria> calcularSugerencias(Donacion donacion,
                                                       List<EntidadBeneficiaria> entidades,
                                                       List<Donacion> todasLasDonaciones) {
    return entidades.stream()
        .sorted((e1, e2) -> Integer.compare(
            contarAsignaciones(e1, todasLasDonaciones),
            contarAsignaciones(e2, todasLasDonaciones)
        ))
        .limit(10)
        .collect(Collectors.toList());
  }

private int contarAsignaciones(EntidadBeneficiaria entidad, List<Donacion> donaciones) {
    return (int) donaciones.stream()
        .filter(d -> entidad.equals(d.getEntidadAsignada()))
        .count();
  }
}