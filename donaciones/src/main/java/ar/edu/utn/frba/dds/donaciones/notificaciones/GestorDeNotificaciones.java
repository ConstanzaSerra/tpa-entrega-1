package ar.edu.utn.frba.dds.donaciones.notificaciones;

import java.util.ArrayList;
import java.util.List;

/**
 * Sujeto (subject) del patron Observer.
 * Los productores publican eventos; el gestor los reparte a todos los observadores suscriptos.
 * Desacopla "ocurrio un evento" de "como se envia la notificacion".
 */
public class GestorDeNotificaciones {
  private final List<ObservadorDeNotificaciones> observadores = new ArrayList<>();

  public void suscribir(ObservadorDeNotificaciones observador) {
    this.observadores.add(observador);
  }

  public void publicar(EventoDeNotificacion evento) {
    for (ObservadorDeNotificaciones observador : observadores) {
      observador.notificar(evento);
    }
  }
}
