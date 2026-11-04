package ar.edu.utn.frba.dds.donaciones.notificaciones;

import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.Notificable;
import ar.edu.utn.frba.dds.donaciones.domain.NotificacionWeb;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.repository.NotificacionWebRepository;

import java.time.LocalDateTime;

/**
 * Segundo observador del GestorDeNotificaciones: además del envío por Email/SMS/WhatsApp,
 * persiste cada notificación en la bandeja web para que la Entidad Beneficiaria y la
 * Persona Donante puedan visualizarlas en la plataforma (Requerimiento 11 - Entrega 4).
 */
public class RegistradorNotificacionesWeb implements ObservadorDeNotificaciones {

  private final NotificacionWebRepository repository;

  public RegistradorNotificacionesWeb(NotificacionWebRepository repository) {
    this.repository = repository;
  }

  @Override
  public void notificar(EventoDeNotificacion evento) {
    for (Notificable destinatario : evento.getDestinatarios()) {
      Long entidadId = null;
      Long donanteId = null;
      if (destinatario instanceof EntidadBeneficiaria eb) {
        entidadId = eb.getId();
      } else if (destinatario instanceof PersonaDonante pd) {
        donanteId = pd.getId();
      }
      MedioDeContacto medio = destinatario.medioDePreferencia();
      String contacto = medio != null ? medio.getValor() : "";
      NotificacionWeb notif = new NotificacionWeb(
          evento.getMensaje(),
          LocalDateTime.now(),
          contacto,
          entidadId,
          donanteId
      );
      repository.guardar(notif);
    }
  }
}
