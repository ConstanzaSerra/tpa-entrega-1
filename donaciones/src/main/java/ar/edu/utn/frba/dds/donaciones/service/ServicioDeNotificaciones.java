package ar.edu.utn.frba.dds.donaciones.service;

/**
 * Placeholder del envio de notificaciones.
 * Por ahora solo registra en consola la intencion de notificar.
 * En la tarea #6 se reemplaza por el mecanismo real:
 * Observer (para reaccionar a los eventos) + Strategy por medio (Email/SMS/WhatsApp).
 */
public class ServicioDeNotificaciones {

  public void notificar(String destinatario, String mensaje) {
    System.out.println("[NOTIFICACION] -> " + destinatario + " | " + mensaje);
  }
}
