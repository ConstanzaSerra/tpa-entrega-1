package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donacion")
public class Donacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private Subcategoria subcategoria;

  @Column(name = "cantidad", nullable = false)
  private int cantidad;

  @Column(name = "unidad_medida")
  private String unidadMedida;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false)
  private EstadoDonacion estado;

  @Column(name = "fecha_registro")
  private LocalDate fechaRegistro;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private PersonaDonante donante;

  // Nullable a proposito: la donacion existe en deposito antes de tener entidad
  // asignada, y vuelve a quedar sin entidad tras una entrega fallida.
  @ManyToOne
  @JoinColumn(name = "entidad_asignada_id")
  private EntidadBeneficiaria entidadAsignada;

  @Column(name = "justificacion_fallida")
  private String justificacionFallida;

  // El historial es una lista de valores, no de entidades: se persiste como
  // coleccion de embebibles en su propia tabla, vinculada por donacion_id.
  @ElementCollection
  @CollectionTable(name = "cambio_de_estado", joinColumns = @JoinColumn(name = "donacion_id"))
  @OrderColumn(name = "orden")
  private List<CambioDeEstado> historial = new ArrayList<>();

  protected Donacion() {}

  public Donacion(Subcategoria subcategoria, int cantidad, String unidadMedida,
                  PersonaDonante donante, LocalDate fechaRegistro) {
    this.subcategoria = subcategoria;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
    this.donante = donante;
    this.fechaRegistro = fechaRegistro;
    this.estado = EstadoDonacion.EN_DEPOSITO;
    registrarCambio();
  }

  // Anota en el historial el estado actual. Se llama desde cada transicion,
  // por lo que la trazabilidad no se puede saltear.
  private void registrarCambio() {
    this.historial.add(new CambioDeEstado(this.estado, LocalDateTime.now()));
  }

  // Avanza al proximo estado de la linea de entrega, si corresponde.
  public void avanzarEstado() {
    EstadoDonacion siguiente = calcularSiguiente();
    if (siguiente == null) {
      throw new IllegalStateException("La donación no puede avanzar de estado desde: " + estado);
    }
    aplicarAvance(siguiente);
  }

  // Avanza solo si el proximo estado natural coincide con el objetivo pedido.
  // Lo usa la integracion con Logistica, que indica a que estado quiere pasar.
  public void avanzarHacia(EstadoDonacion objetivo) {
    EstadoDonacion siguiente = calcularSiguiente();
    if (siguiente == null) {
      throw new IllegalStateException("La donación no puede avanzar de estado desde: " + estado);
    }
    if (siguiente != objetivo) {
      throw new IllegalStateException(
          "Desde " + estado + " la donación solo puede pasar a " + siguiente + ", no a " + objetivo);
    }
    aplicarAvance(siguiente);
  }

  private EstadoDonacion calcularSiguiente() {
    switch (estado) {
      case ASIGNACION_REALIZADA: return EstadoDonacion.LISTA_PARA_ENTREGAR;
      case LISTA_PARA_ENTREGAR:  return EstadoDonacion.EN_TRASLADO;
      case EN_TRASLADO:          return EstadoDonacion.ENTREGADA;
      case ENTREGA_FALLIDA:      return EstadoDonacion.EN_DEPOSITO; // reintento: vuelve al deposito
      default:                   return null; // EN_DEPOSITO, ENTREGADA, VENCIDA no avanzan por esta via
    }
  }

  private void aplicarAvance(EstadoDonacion siguiente) {
    if (estado == EstadoDonacion.ENTREGA_FALLIDA && siguiente == EstadoDonacion.EN_DEPOSITO) {
      entidadAsignada = null;
      justificacionFallida = null;
    }
    estado = siguiente;
    registrarCambio();
  }

  public void asignarEntidad(EntidadBeneficiaria entidadBeneficiaria) {
    if (estado != EstadoDonacion.EN_DEPOSITO) {
      throw new IllegalStateException(
          "Solo se puede asignar una entidad cuando la donación está EN_DEPOSITO. Estado actual: " + estado);
    }
    this.entidadAsignada = entidadBeneficiaria;
    this.estado = EstadoDonacion.ASIGNACION_REALIZADA;
    registrarCambio();
  }

  public void marcarEntregaFallida(String justificacion) {
    if (estado != EstadoDonacion.EN_TRASLADO) {
      throw new IllegalStateException(
          "Solo se puede marcar como entrega fallida cuando la donación está EN_TRASLADO. Estado actual: " + estado);
    }
    this.justificacionFallida = justificacion;
    this.estado = EstadoDonacion.ENTREGA_FALLIDA;
    registrarCambio();
  }

  public void marcarVencida() {
    if (estado == EstadoDonacion.ENTREGADA || estado == EstadoDonacion.VENCIDA) {
      throw new IllegalStateException(
          "No se puede marcar como vencida una donación que ya está en estado: " + estado);
    }
    this.estado = EstadoDonacion.VENCIDA;
    registrarCambio();
  }

  public List<CambioDeEstado> getHistorial() {
    return new ArrayList<>(historial);
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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setCantidad(int cantidad) {
    this.cantidad = cantidad;
  }

  public void setUnidadMedida(String unidadMedida) {
    this.unidadMedida = unidadMedida;
  }

  public void setSubcategoria(Subcategoria subcategoria) {
    this.subcategoria = subcategoria;
  }
}
