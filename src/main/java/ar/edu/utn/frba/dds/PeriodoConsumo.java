package ar.edu.utn.frba.dds;

import ar.edu.utn.frba.dds.exceptions.CantidadException;
import ar.edu.utn.frba.dds.exceptions.DescripcionException;

public class PeriodoConsumo {
  private Integer cantidadObjetivo;
  private String descripcion;
  private Integer cantidadRecibida;

  public PeriodoConsumo(Integer cantidadObjetivo, String descripcion, Integer cantidadRecibida) {
    validarCantidad(cantidadObjetivo);
    validarPeriodo(descripcion);
    validarCantidad(cantidadRecibida);

    this.cantidadObjetivo = cantidadObjetivo;
    this.descripcion = descripcion;
    this.cantidadRecibida = cantidadRecibida;
  }

  public Boolean estaSatisfecha(){
   return this.cantidadRecibida >= this.cantidadObjetivo;
  }

  public void registrarCantidad(Integer cantidad){
    validarCantidad(cantidad);

    // control tope del período
    int faltanteParaSatisfecho = this.cantidadObjetivo - this.cantidadRecibida;

    if (faltanteParaSatisfecho > 0) {
      int aAceptar = Math.min(cantidad, faltanteParaSatisfecho);
      this.cantidadRecibida += aAceptar;
    }

  }

  public void validarCantidad(Integer cantidad){
    if (cantidad == null || cantidad < 0){
      throw new CantidadException("La cantidad objetivo debe ser mayor a cero");
    }
  }

  public void validarPeriodo(String periodoDescripcion){
    if ( periodoDescripcion == null || periodoDescripcion.trim().isEmpty()){
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
