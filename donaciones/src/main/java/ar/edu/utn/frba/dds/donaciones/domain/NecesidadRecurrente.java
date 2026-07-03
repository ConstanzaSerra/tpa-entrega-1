package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.exceptions.PeriodoConsumoException;

public class NecesidadRecurrente extends Necesidad {
  private PeriodoConsumo periodoConsumo;

  public NecesidadRecurrente(Subcategoria subcategoria, String descripcion, PeriodoConsumo periodoConsumo) {
    super(subcategoria, descripcion);
    if (periodoConsumo == null) {
      throw new PeriodoConsumoException("Agregue un periodo de consumo de consumo");
    }

    this.periodoConsumo = periodoConsumo;
  }

  @Override
  public Boolean estaSatisfecha() {
    return periodoConsumo.estaSatisfecha();
  }

  @Override
  public void registrarRecepcion(Integer cantidad) {
    periodoConsumo.registrarCantidad(cantidad);
  }

  public void reiniciarPeriodo() {
    periodoConsumo.reiniciar();
  }

  public PeriodoConsumo getPeriodoConsumo() {
    return periodoConsumo;
  }
}
