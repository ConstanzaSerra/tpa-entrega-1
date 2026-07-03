<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/BienConEstado.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.bienes;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/bienes/BienConEstado.java

public class BienConEstado extends Bien {
  private Boolean esNuevo;

  public BienConEstado(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida, Boolean esNuevo) {
    super(descripcion, foto, subcategoria, cantidad, unidadDeMedida);
    this.esNuevo = esNuevo;
  }

  public Boolean esNuevo(){ // TODO - no se si es correcto el nombre del método (no llego yo a entender el nombre en el diagrama de clases)
    return this.esNuevo;
  }
}
