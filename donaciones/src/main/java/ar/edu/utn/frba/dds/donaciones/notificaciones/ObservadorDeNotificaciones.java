package ar.edu.utn.frba.dds.donaciones.notificaciones;

/**
 * Observer: reacciona cuando se publica un evento de notificacion.
 */
public interface ObservadorDeNotificaciones {
  void notificar(EventoDeNotificacion evento);
}
