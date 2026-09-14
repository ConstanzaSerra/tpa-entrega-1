package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

/**
 * Registro inmutable de una transicion de estado de una donacion.
 * Guarda el estado al que entro y el momento en que ocurrio.
 *
 * Valor, no entidad (Clase 14): no hace falta identificar univocamente un
 * cambio de estado, interesa el par (estado, fechaHora) dentro del historial de
 * su donacion. Se persiste con @ElementCollection desde Donacion.
 *
 * Nota: los campos dejaron de ser final porque Hibernate necesita escribirlos
 * por reflection al reconstruir el objeto desde la base.
 */
@Embeddable
public class CambioDeEstado {

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false)
  private EstadoDonacion estado;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  protected CambioDeEstado() {}

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
