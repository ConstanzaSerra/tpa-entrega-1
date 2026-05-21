package ar.edu.utn.frba.dds;

import java.time.LocalDate;

public class Donacion {
  private Subcategoria subcategoria;
  private Integer cantidad;
  private EstadoDonacion estado;
  private LocalDate fechaRegistro;
  private PersonaDonante donante;
  private EntidadBeneficiaria entidadAsignada;
  private String justificacionFallida;

  public Donacion(Subcategoria subcategoria, Integer cantidad, EstadoDonacion estado, LocalDate fechaRegistro,
                  PersonaDonante donante) {
    this.subcategoria = subcategoria;
    this.cantidad = cantidad;
    this.estado = estado;
    this.fechaRegistro = fechaRegistro;
    this.donante = donante;
  }

  public void avanzarEstado() {
    //TODO - implementar
  }

  public void asignarEntidad(EntidadBeneficiaria setEntidadAsignada) {
    this.entidadAsignada = setEntidadAsignada;
    //TODO - yo llamararía al metodo setEntidadAsignada
  }

  public void marcarEntregaFallida(String justificacionFallida) {
    this.justificacionFallida = justificacionFallida;
    //TODO - yo llamaría al método setJustificacionFallida
  }

  public void marcarVencida() {
    //TODO - implementar
  }

  public EstadoDonacion getEstado() {
    return estado;
  }

  public Subcategoria getSubcategoria() {
    return this.subcategoria;
  }

  public Integer getCantidad() {
    return this.cantidad;
  }
}
