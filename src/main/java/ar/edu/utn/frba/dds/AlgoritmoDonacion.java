package ar.edu.utn.frba.dds;

import java.util.List;

public interface AlgoritmoDonacion {
  List<EntidadBeneficiaria> calcularSugerencias(Donacion donacion,
                                                List<EntidadBeneficiaria> entidades,
                                                List<Donacion> historialDonaciones);
}