<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/BienPercibible.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.bienes;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/bienes/BienPercibible.java

import java.time.LocalDate;

public class BienPercibible extends Bien {
  private LocalDate fechaVencimiento;

  public BienPercibible(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida, LocalDate fechaVencimiento) {
    super(descripcion, foto, subcategoria, cantidad, unidadDeMedida);
    this.fechaVencimiento = fechaVencimiento;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }
}
