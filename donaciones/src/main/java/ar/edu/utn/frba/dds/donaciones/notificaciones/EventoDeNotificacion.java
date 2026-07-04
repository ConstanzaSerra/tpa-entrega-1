package ar.edu.utn.frba.dds.donaciones.notificaciones;

import ar.edu.utn.frba.dds.donaciones.domain.Notificable;

import java.util.List;

/**
 * Evento que representa "hay que notificar algo a alguien".
 * Lo publica el productor (un controller, el matchmaking, la tarea de inactividad, etc.)
 * y lo reciben los observadores.
 */
public class EventoDeNotificacion {
  private final List<Notificable> destinatarios;
  private final String mensaje;

  public EventoDeNotificacion(List<Notificable> destinatarios, String mensaje) {
    this.destinatarios = destinatarios;
    this.mensaje = mensaje;
  }

  public List<Notificable> getDestinatarios() {
    return destinatarios;
  }

  public String getMensaje() {
    return mensaje;
  }
}
