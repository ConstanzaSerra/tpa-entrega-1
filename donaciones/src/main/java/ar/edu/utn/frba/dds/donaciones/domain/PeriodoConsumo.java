package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.exceptions.CantidadException;
import ar.edu.utn.frba.dds.donaciones.exceptions.DescripcionException;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Valor embebido en NecesidadRecurrente (Clase 14): no tiene identidad propia,
 * no se lo consulta ni se lo referencia por fuera de su necesidad, y lo que
 * importa son sus atributos. Como es un unico valor (no una lista), sus
 * columnas van directamente en la tabla que lo embebe, sin JOIN.
 */
@Embeddable
public class PeriodoConsumo {

  @Column(name = "periodo_cantidad_objetivo")
  private Integer cantidadObjetivo;

  @Column(name = "periodo_descripcion")
  private String descripcion;

  @Column(name = "periodo_cantidad_recibida")
  private Integer cantidadRecibida;

  protected PeriodoConsumo() {}

  public PeriodoConsumo(Integer cantidadObjetivo, String descripcion, Integer cantidadRecibida) {
    validarCantidad(cantidadObjetivo);
    validarPeriodo(descripcion);
    validarCantidad(cantidadRecibida);

    this.cantidadObjetivo = cantidadObjetivo;
    this.descripcion = descripcion;
    this.cantidadRecibida = cantidadRecibida;
  }

  public Boolean estaSatisfecha() {
    return this.cantidadRecibida >= this.cantidadObjetivo;
  }

  public void registrarCantidad(Integer cantidad) {
    validarCantidad(cantidad);

    // control tope del período
    int faltanteParaSatisfecho = this.cantidadObjetivo - this.cantidadRecibida;

    if (faltanteParaSatisfecho > 0) {
      int aAceptar = Math.min(cantidad, faltanteParaSatisfecho);
      this.cantidadRecibida += aAceptar;
    }
  }

  public void validarCantidad(Integer cantidad) {
    if (cantidad == null || cantidad < 0) {
      throw new CantidadException("La cantidad objetivo debe ser mayor a cero");
    }
  }

  public void validarPeriodo(String periodoDescripcion) {
    if (periodoDescripcion == null || periodoDescripcion.trim().isEmpty()) {
      throw new DescripcionException("La descripcion del período no puede estar vacía");
    }
  }

  public Integer getCantidadObjetivo() {
    return cantidadObjetivo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public Integer getCantidadRecibida() {
    return cantidadRecibida;
  }

  public void reiniciar() {
    this.cantidadRecibida = 0;
  }
}
