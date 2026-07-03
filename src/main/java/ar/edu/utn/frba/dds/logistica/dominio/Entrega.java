package ar.edu.utn.frba.dds.logistica.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Entrega {
  private Long id;
  private Long donacionId;
  private Long entidadBeneficiariaId;
  private EstadoEntrega estado;
  private Camion camionQueEntrego;
  private LocalDateTime fechaHoraEntrega;
  private List<String> fotosUrl;

  public Entrega() {
    this.estado = EstadoEntrega.PENDIENTE;
    this.fotosUrl = new ArrayList<>();
  }

  public Entrega(Long donacionId, Long entidadBeneficiariaId) {
    this();
    this.donacionId = donacionId;
    this.entidadBeneficiariaId = entidadBeneficiariaId;
  }

  public void iniciarTraslado() {
    if (estado != EstadoEntrega.PENDIENTE) {
      throw new IllegalStateException("Solo se puede iniciar traslado desde PENDIENTE. Estado: " + estado);
    }
    this.estado = EstadoEntrega.EN_TRASLADO;
  }

  public void confirmar(Camion camion, LocalDateTime fechaHora) {
    if (estado != EstadoEntrega.EN_TRASLADO) {
      throw new IllegalStateException("Solo se puede confirmar una entrega EN_TRASLADO. Estado: " + estado);
    }
    this.estado = EstadoEntrega.ENTREGADA;
    this.camionQueEntrego = camion;
    this.fechaHoraEntrega = fechaHora;
  }

  public void rechazar() {
    if (estado != EstadoEntrega.EN_TRASLADO) {
      throw new IllegalStateException("Solo se puede rechazar una entrega EN_TRASLADO. Estado: " + estado);
    }
    this.estado = EstadoEntrega.NO_RECIBIDA;
  }

  public void retornarADeposito() {
    if (estado != EstadoEntrega.NO_RECIBIDA) {
      throw new IllegalStateException("Solo se puede retornar al depósito desde NO_RECIBIDA. Estado: " + estado);
    }
    this.estado = EstadoEntrega.PENDIENTE;
    this.camionQueEntrego = null;
    this.fechaHoraEntrega = null;
  }

  public void agregarFoto(String url) {
    this.fotosUrl.add(url);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getDonacionId() { return donacionId; }
  public void setDonacionId(Long donacionId) { this.donacionId = donacionId; }
  public Long getEntidadBeneficiariaId() { return entidadBeneficiariaId; }
  public void setEntidadBeneficiariaId(Long entidadBeneficiariaId) { this.entidadBeneficiariaId = entidadBeneficiariaId; }
  public EstadoEntrega getEstado() { return estado; }
  public Camion getCamionQueEntrego() { return camionQueEntrego; }
  public LocalDateTime getFechaHoraEntrega() { return fechaHoraEntrega; }
  public List<String> getFotosUrl() { return fotosUrl; }
}
