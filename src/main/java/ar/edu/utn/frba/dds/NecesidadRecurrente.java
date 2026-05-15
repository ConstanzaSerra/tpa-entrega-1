package ar.edu.utn.frba.dds;

public class NecesidadRecurrente extends Necesidad{
  private Integer cantidadRequerida;
  private Integer cantidadRecibida;

  public NecesidadRecurrente(Subcategoria subcategoria, String descripcion, Integer cantidadRequerida, Integer cantidadRecibida) {
    super(subcategoria, descripcion);
    this.cantidadRequerida = cantidadRequerida;
    this.cantidadRecibida = cantidadRecibida;
  }

  @Override
  public Boolean estaSatisfecha() { //TODO - implementar
    return null;
  }

  @Override
  public void registrarRecepcion(Integer cantidad) {

  }
}
