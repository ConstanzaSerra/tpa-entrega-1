package ar.edu.utn.frba.dds;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcesadorMatchmaking {

  public PropuestaMatchmaking procesar(Donacion donacion,
                                       List<EntidadBeneficiaria> todasLasEntidades,
                                       List<Donacion> historial) {

    CompatibilidadSemantica semantico = new CompatibilidadSemantica();
    PrioridadSubatendidos prioridad = new PrioridadSubatendidos();

    List<EntidadBeneficiaria> rankingA = semantico.calcularSugerencias(donacion, todasLasEntidades, historial);
    List<EntidadBeneficiaria> rankingB = prioridad.calcularSugerencias(donacion, todasLasEntidades, historial);

    List<EntidadBeneficiaria> enAmbosRankings = rankingA.stream()
        .filter(rankingB::contains)
        .collect(Collectors.toList());

    if (!enAmbosRankings.isEmpty()) {
      return new PropuestaMatchmaking(donacion, enAmbosRankings, true);
    } else {
      List<EntidadBeneficiaria> todasLasSugeridas = new ArrayList<>(rankingA);
      for (EntidadBeneficiaria e : rankingB) {
        if (!todasLasSugeridas.contains(e)) {
          todasLasSugeridas.add(e);
        }
      }
      return new PropuestaMatchmaking(donacion, todasLasSugeridas, false);
    }
  }
}