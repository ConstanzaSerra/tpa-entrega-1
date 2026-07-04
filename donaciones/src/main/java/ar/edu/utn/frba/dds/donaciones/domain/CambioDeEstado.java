package ar.edu.utn.frba.dds.donaciones.domain;

import java.time.LocalDateTime;

/**
 * Registro inmutable de una transicion de estado de una donacion.
 * Guarda el estado al que entro y el momento en que ocurrio.
 */
public class CambioDeEstado {
  private final EstadoDonacion estado;
  private final LocalDateTime fechaHora;

  public CambioDeEstado(EstadoDonacion estado, LocalDateTime fechaHora) {
    this.estado = estado;
    this.fechaHora = fechaHora;
  }

  public EstadoDonacion getEstado() {
    return estado;
  }

  public LocalDateTime getFechaHora() {
    return fechaHora;
  }
}
