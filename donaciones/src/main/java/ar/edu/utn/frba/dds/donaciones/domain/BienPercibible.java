package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("PERECEDERO")
public class BienPercibible extends Bien {

  @Column(name = "fecha_vencimiento")
  private LocalDate fechaVencimiento;

  protected BienPercibible() {}

  public BienPercibible(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida, LocalDate fechaVencimiento) {
    super(descripcion, foto, subcategoria, cantidad, unidadDeMedida);
    this.fechaVencimiento = fechaVencimiento;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }
}
