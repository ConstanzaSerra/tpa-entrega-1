package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("SIMPLE")
public class BienSimple extends Bien {

  protected BienSimple() {}

  public BienSimple(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida) {
    super(descripcion, foto, subcategoria, cantidad, unidadDeMedida);
  }
}
