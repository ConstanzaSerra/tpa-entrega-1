package ar.edu.utn.frba.dds;

public class NecesidadExtraordinaria extends Necesidad{
  private Integer cantidadObjetivoPorPeriodo;
  private String periodoDescripcion;
  private Integer cantidadRecibidaEnPeriodo;

  public NecesidadExtraordinaria(Subcategoria subcategoria, String descripcion, Integer cantidadObjetivoPorPeriodo, String periodoDescripcion, Integer cantidadRecibidaEnPeriodo) {
    super(subcategoria, descripcion);
    this.cantidadObjetivoPorPeriodo = cantidadObjetivoPorPeriodo;
    this.periodoDescripcion = periodoDescripcion;
    this.cantidadRecibidaEnPeriodo = cantidadRecibidaEnPeriodo;
  }

  @Override
  public Boolean estaSatisfecha() { //TODO - implementar
    return null;
  }

  public void registrarRecepcion(Integer cantidad){//TODO - implementar
  }

  public void reiniciarPeriodo(){//TODO - implementar
  }
}
