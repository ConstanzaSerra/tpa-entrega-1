package ar.edu.utn.frba.dds.bienes;

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
