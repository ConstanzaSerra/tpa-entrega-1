package ar.edu.utn.frba.dds.necesidades;

import ar.edu.utn.frba.dds.bienes.Subcategoria;

public abstract class Necesidad {
  private Subcategoria subcategoria;
  private String descripcion;

  public Necesidad(Subcategoria subcategoria, String descripcion) {
    this.subcategoria = subcategoria;
    this.descripcion = descripcion;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public abstract Boolean estaSatisfecha();

  public abstract void registrarRecepcion(Integer cantidad);
}
