package ar.edu.utn.frba.dds;

import java.time.LocalDate;

public class Donacion {
  private Subcategoria subcategoria;
  private int cantidad;
  private String unidadMedida;
  private EstadoDonacion estado;
  private LocalDate fechaRegistro;
  private PersonaDonante donante;
  private EntidadBeneficiaria entidadAsignada;
  private String justificacionFallida;

  public Donacion(Subcategoria subcategoria, int cantidad, String unidadMedida,
                  PersonaDonante donante, LocalDate fechaRegistro) {
    this.subcategoria = subcategoria;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
    this.donante = donante;
    this.fechaRegistro = fechaRegistro;
    this.estado = EstadoDonacion.EN_DEPOSITO;
  }

  public void avanzarEstado() {
    switch (estado) {
      case EN_DEPOSITO:
        throw new IllegalStateException(
            "Para avanzar desde EN_DEPOSITO debe usarse asignarEntidad(EntidadBeneficiaria).");
      case ASIGNACION_REALIZADA:
        estado = EstadoDonacion.LISTA_PARA_ENTREGAR;
        break;

      case LISTA_PARA_ENTREGAR:
        estado = EstadoDonacion.EN_TRASLADO;
        break;

      case EN_TRASLADO:
        estado = EstadoDonacion.ENTREGADA;
        break;

      case ENTREGA_FALLIDA:
        // Vuelve al depósito para reintentar el proceso
        estado = EstadoDonacion.EN_DEPOSITO;
        entidadAsignada = null;
        justificacionFallida = null;
        break;

      case ENTREGADA:
        throw new IllegalStateException("La donación ya fue entregada y no puede avanzar de estado.");

      case VENCIDA:
        throw new IllegalStateException("La donación está vencida y no puede avanzar de estado.");

      default:
        throw new IllegalStateException("Estado desconocido: " + estado);
    }
  }

  public void asignarEntidad(EntidadBeneficiaria entidadBeneficiaria) {
    if (estado != EstadoDonacion.EN_DEPOSITO) {
      throw new IllegalStateException(
          "Solo se puede asignar una entidad cuando la donación está EN_DEPOSITO. Estado actual: " + estado);
    }
    this.entidadAsignada = entidadBeneficiaria;
    this.estado = EstadoDonacion.ASIGNACION_REALIZADA;
  }

  public void marcarEntregaFallida(String justificacion) {
    if (estado != EstadoDonacion.EN_TRASLADO) {
      throw new IllegalStateException(
          "Solo se puede marcar como entrega fallida cuando la donación está EN_TRASLADO. Estado actual: " + estado);
    }
    this.justificacionFallida = justificacion;
    this.estado = EstadoDonacion.ENTREGA_FALLIDA;
  }

  public void marcarVencida() {
    if (estado == EstadoDonacion.ENTREGADA || estado == EstadoDonacion.VENCIDA) {
      throw new IllegalStateException(
          "No se puede marcar como vencida una donación que ya está en estado: " + estado);
    }
    this.estado = EstadoDonacion.VENCIDA;
  }

  public EstadoDonacion getEstado() {
    return estado;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public int getCantidad() {
    return cantidad;
  }

  public String getUnidadMedida() {
    return unidadMedida;
  }

  public LocalDate getFechaRegistro() {
    return fechaRegistro;
  }

  public PersonaDonante getDonante() {
    return donante;
  }

  public EntidadBeneficiaria getEntidadAsignada() {
    return entidadAsignada;
  }

  public String getJustificacionFallida() {
    return justificacionFallida;
  }
}