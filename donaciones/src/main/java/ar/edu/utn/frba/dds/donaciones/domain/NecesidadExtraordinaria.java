package ar.edu.utn.frba.dds.donaciones.domain;

public class NecesidadExtraordinaria extends Necesidad {
  private Integer cantidadRequerida;
  private Integer cantidadRecibida;

  public NecesidadExtraordinaria(Subcategoria subcategoria, String descripcion, Integer cantidadRequerida, Integer cantidadRecibida) {
    super(subcategoria, descripcion);
    validarCantidadRequerida(cantidadRequerida);
    validarCantidadNoNegativa(cantidadRecibida);

    this.cantidadRequerida = cantidadRequerida;
    this.cantidadRecibida = cantidadRecibida;
  }

  @Override
  public Boolean estaSatisfecha() {
    return cantidadRecibida >= cantidadRequerida;
  }

  @Override
  public void registrarRecepcion(Integer cantidad) {
    validarCantidadNoNegativa(cantidad);
    this.cantidadRecibida += cantidad;
  }

  private void validarCantidadRequerida(Integer cantidad) {
    if (cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
    }
  }

  private void validarCantidadNoNegativa(Integer cantidad) {
    if (cantidad == null || cantidad < 0) {
      throw new IllegalArgumentException("La cantidad no puede ser negativa");
    }
  }

  public Integer getCantidadRequerida() {
    return cantidadRequerida;
  }

  public Integer getCantidadRecibida() {
    return cantidadRecibida;
  }
}
