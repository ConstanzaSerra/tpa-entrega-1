package ar.edu.utn.frba.dds;

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
