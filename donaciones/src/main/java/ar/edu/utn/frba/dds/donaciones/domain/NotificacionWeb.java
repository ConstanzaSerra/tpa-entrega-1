package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Notificación persistida en el sistema para ser consultada en la bandeja web
 * por la Entidad Beneficiaria o Persona Donante (Requerimiento 11 - Entrega 4).
 */
@Entity
@Table(name = "notificacion_web")
public class NotificacionWeb {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "mensaje", nullable = false, length = 1000)
  private String mensaje;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @Column(name = "leida", nullable = false)
  private boolean leida = false;

  @Column(name = "destinatario_contacto")
  private String destinatarioContacto;

  @Column(name = "entidad_id")
  private Long entidadId;

  @Column(name = "donante_id")
  private Long donanteId;

  protected NotificacionWeb() {}

  public NotificacionWeb(String mensaje, LocalDateTime fechaHora,
                         String destinatarioContacto, Long entidadId, Long donanteId) {
    this.mensaje = mensaje;
    this.fechaHora = fechaHora != null ? fechaHora : LocalDateTime.now();
    this.destinatarioContacto = destinatarioContacto;
    this.entidadId = entidadId;
    this.donanteId = donanteId;
  }

  public void marcarLeida() {
    this.leida = true;
  }

  public Long getId() {
    return id;
  }

  public String getMensaje() {
    return mensaje;
  }

  public LocalDateTime getFechaHora() {
    return fechaHora;
  }

  public String getFechaHoraFormateada() {
    return fechaHora != null
        ? fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        : "";
  }

  public boolean isLeida() {
    return leida;
  }

  public String getDestinatarioContacto() {
    return destinatarioContacto;
  }

  public Long getEntidadId() {
    return entidadId;
  }

  public Long getDonanteId() {
    return donanteId;
  }
}
