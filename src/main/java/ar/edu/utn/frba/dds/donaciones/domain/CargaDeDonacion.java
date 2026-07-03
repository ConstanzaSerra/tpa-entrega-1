package ar.edu.utn.frba.dds.donaciones.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CargaDeDonacion {
  private String descripcionGeneral;
  private LocalDate fecha;
  private PersonaDonante donante;
  private List<Bien> bienes;

  public CargaDeDonacion(String descripcionGeneral, LocalDate fecha, PersonaDonante donante, List<Bien> bienes) {
    this.descripcionGeneral = descripcionGeneral;
    this.fecha = fecha;
    this.donante = donante;
    this.bienes = bienes;
  }

  public void agregarBien(Bien bien) {
    this.bienes.add(bien);
  }

  public List<Donacion> segmentar() {
    Map<ClaveSegmentacion, Integer> cantidadPorClave = new HashMap<>();

    for (Bien bien : bienes) {
      LocalDate fechaVencimiento = (bien instanceof BienPercibible)
          ? ((BienPercibible) bien).getFechaVencimiento()
          : null;

      ClaveSegmentacion clave = new ClaveSegmentacion(
          bien.getSubcategoria(),
          fechaVencimiento,
          bien.getUnidadDeMedida()
      );
      cantidadPorClave.merge(clave, bien.getCantidad(), Integer::sum);
    }

    return cantidadPorClave.entrySet().stream()
        .map(entry -> new Donacion(
            entry.getKey().subcategoria(),
            entry.getValue(),
            entry.getKey().unidadMedida(),
            this.donante,
            this.fecha
        ))
        .collect(Collectors.toList());
  }

  private record ClaveSegmentacion(Subcategoria subcategoria, LocalDate fechaVencimiento, String unidadMedida) {}

  public String getDescripcionGeneral() {
    return descripcionGeneral;
  }

  public List<Bien> getBienes() {
    return bienes;
  }
}
