<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Necesidad.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.necesidades;

import ar.edu.utn.frba.dds.bienes.Subcategoria;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/necesidades/Necesidad.java

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

<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Necesidad.java
  public String getDescripcion() {return descripcion;}

========
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/necesidades/Necesidad.java
  public abstract Boolean estaSatisfecha();

  public abstract void registrarRecepcion(Integer cantidad);
}
