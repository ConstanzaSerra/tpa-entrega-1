package ar.edu.utn.frba.dds.donaciones.matchmaking;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import java.util.ArrayList;
import java.util.List;

public class ProcesadorMatchmaking {

  // se agrega una colección de estrategias
  private final List<AlgoritmoDonacion> algoritmos;

  public ProcesadorMatchmaking(List<AlgoritmoDonacion> algoritmos) {
    this.algoritmos = algoritmos;
  }

  public PropuestaMatchmaking procesar(Donacion donacion,
                                       List<EntidadBeneficiaria> todasLasEntidades,
                                       List<Donacion> historial) {

    List<EntidadBeneficiaria> todasLasSugeridas = new ArrayList<>();

    for (AlgoritmoDonacion algoritmo : algoritmos) {
      List<EntidadBeneficiaria> sugerenciasDelAlgoritmo = algoritmo.calcularSugerencias(donacion, todasLasEntidades, historial).stream()
          .limit(10).toList();

      for (EntidadBeneficiaria entidad : sugerenciasDelAlgoritmo) {
        if (!todasLasSugeridas.contains(entidad)) {
          todasLasSugeridas.add(entidad);
        }
      }
    }

    boolean huboCoincidencias = !todasLasSugeridas.isEmpty();

    return new PropuestaMatchmaking(donacion, todasLasSugeridas, huboCoincidencias);
  }
}