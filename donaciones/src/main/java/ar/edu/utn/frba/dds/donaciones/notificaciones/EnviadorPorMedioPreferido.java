package ar.edu.utn.frba.dds.donaciones.notificaciones;

import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.Notificable;

/**
 * Observador concreto: al recibir un evento, envia el mensaje a cada destinatario
 * por su medio de preferencia (Email/SMS/WhatsApp). El "como" lo resuelve el medio (Strategy).
 */
public class EnviadorPorMedioPreferido implements ObservadorDeNotificaciones {

  @Override
  public void notificar(EventoDeNotificacion evento) {
    for (Notificable destinatario : evento.getDestinatarios()) {
      MedioDeContacto medio = destinatario.medioDePreferencia();
      if (medio != null) {
        medio.enviarMensaje(evento.getMensaje());
      }
    }
  }
}
