package ar.edu.utn.frba.dds.necesidades;

import ar.edu.utn.frba.dds.bienes.Subcategoria;

public class NecesidadRecurrente extends Necesidad {
  private Integer cantidadObjetivoPorPeriodo;
  private String periodoDescripcion;
  private Integer cantidadRecibidaEnPeriodo;

  public NecesidadRecurrente(Subcategoria subcategoria, String descripcion, Integer cantidadObjetivoPorPeriodo, String periodoDescripcion, Integer cantidadRecibidaEnPeriodo) {
    super(subcategoria, descripcion);
    validarCantidadObjetivo(cantidadObjetivoPorPeriodo);
    validarPeriodo(periodoDescripcion);
    validarCantidadNoNegativa(cantidadRecibidaEnPeriodo);

    this.cantidadObjetivoPorPeriodo = cantidadObjetivoPorPeriodo;
    this.periodoDescripcion = periodoDescripcion.trim();
    this.cantidadRecibidaEnPeriodo = cantidadRecibidaEnPeriodo;
  }

  @Override
  public Boolean estaSatisfecha() {
    return cantidadRecibidaEnPeriodo >= cantidadObjetivoPorPeriodo;
  }

  @Override
  public void registrarRecepcion(Integer cantidad) {
    validarCantidadNoNegativa(cantidad);
    this.cantidadRecibidaEnPeriodo += cantidad;
  }

  public void reiniciarPeriodo() {
    this.cantidadRecibidaEnPeriodo = 0;
  }

  private void validarCantidadObjetivo(Integer cantidad) {
    if (cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad objetivo debe ser mayor a cero");
    }
  }

  private void validarCantidadNoNegativa(Integer cantidad) {
    if (cantidad == null || cantidad < 0) {
      throw new IllegalArgumentException("La cantidad no puede ser negativa");
    }
  }

  private void validarPeriodo(String periodoDescripcion) {
    if (periodoDescripcion == null || periodoDescripcion.isBlank()) {
      throw new IllegalArgumentException("La descripcion del periodo no puede estar vacia");
    }
  }
}
