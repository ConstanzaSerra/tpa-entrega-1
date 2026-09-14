package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CON_ESTADO")
public class BienConEstado extends Bien {

  @Column(name = "es_nuevo")
  private Boolean esNuevo;

  protected BienConEstado() {}

  public BienConEstado(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida, Boolean esNuevo) {
    super(descripcion, foto, subcategoria, cantidad, unidadDeMedida);
    this.esNuevo = esNuevo;
  }

  public Boolean esNuevo() {
    return this.esNuevo;
  }
}
