package ar.edu.utn.frba.dds;

import java.util.List;
import java.util.stream.Collectors;

public class CompatibilidadSemantica implements AlgoritmoDonacion {

  @Override
  public List<EntidadBeneficiaria> calcularSugerencias(Donacion donacion,
                                                       List<EntidadBeneficiaria> entidades,
                                                       List<Donacion> historialDonaciones) {
    Subcategoria subcategoriaDonada = donacion.getSubcategoria();

    return entidades.stream()
        .filter(entidad -> entidad.getNecesidades().stream()
            .anyMatch(n -> n.getSubcategoria().equals(subcategoriaDonada)))
        .limit(10)
        .collect(Collectors.toList());
  }
}