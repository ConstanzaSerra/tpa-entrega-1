<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/CargaDeDonacion.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.donaciones;

import ar.edu.utn.frba.dds.bienes.Bien;
import ar.edu.utn.frba.dds.bienes.BienConEstado;
import ar.edu.utn.frba.dds.bienes.BienPercibible;
import ar.edu.utn.frba.dds.bienes.Subcategoria;
import ar.edu.utn.frba.dds.donantes.PersonaDonante;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/donaciones/CargaDeDonacion.java

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

      Boolean esNuevo = null;
      if (bien.getSubcategoria().requiereEstado()) {
        if (!(bien instanceof BienConEstado)) {
          throw new IllegalArgumentException(
              "El bien '" + bien.getDescripcion() + "' pertenece a una subcategoría que requiere " +
              "estado (nuevo/usado) pero no es un BienConEstado.");
        }
        esNuevo = ((BienConEstado) bien).esNuevo();
      }

      ClaveSegmentacion clave = new ClaveSegmentacion(
          bien.getSubcategoria(),
          fechaVencimiento,
          bien.getUnidadDeMedida(),
          esNuevo
      );
      cantidadPorClave.merge(clave, bien.getCantidad(), Integer::sum);
    }

    return cantidadPorClave.entrySet().stream()
        .map(entry -> new Donacion(
            entry.getKey().subcategoria(),
            entry.getValue(),
            entry.getKey().unidadMedida(),
            this.donante,
            this.fecha,
            entry.getKey().esNuevo()
        ))
        .collect(Collectors.toList());
  }

<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/CargaDeDonacion.java
  private record ClaveSegmentacion(Subcategoria subcategoria, LocalDate fechaVencimiento, String unidadMedida) {}

  public String getDescripcionGeneral() {
    return descripcionGeneral;
  }

  public List<Bien> getBienes() {
    return bienes;
  }
========
  private record ClaveSegmentacion(Subcategoria subcategoria, LocalDate fechaVencimiento, String unidadMedida, Boolean esNuevo) {}
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/donaciones/CargaDeDonacion.java
}
