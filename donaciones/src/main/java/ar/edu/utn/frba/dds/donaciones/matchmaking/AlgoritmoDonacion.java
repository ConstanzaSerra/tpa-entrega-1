package ar.edu.utn.frba.dds.donaciones.matchmaking;

import ar.edu.utn.frba.dds.donaciones.domain.*;

import java.util.List;

public interface AlgoritmoDonacion {
  List<EntidadBeneficiaria> calcularSugerencias(Donacion donacion,
                                                List<EntidadBeneficiaria> entidades,
                                                List<Donacion> historialDonaciones);
}