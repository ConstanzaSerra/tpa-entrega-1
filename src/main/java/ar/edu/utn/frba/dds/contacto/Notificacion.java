package ar.edu.utn.frba.dds.contacto;

import ar.edu.utn.frba.dds.donantes.PersonaDonante;
import ar.edu.utn.frba.dds.exceptions.MedioContactoException;

public class Notificacion {
  private final PersonaDonante destinatario;
  private final String mensaje;
  private MedioDeContacto medio;
  private Boolean completada;

  public Notificacion(PersonaDonante destinatario, String mensaje, MedioDeContacto medio) {
    this.destinatario = destinatario;
    this.mensaje = mensaje;
    this.medio = medio;
    this.completada = false;
  }

  public Notificacion(String mensaje, MedioDeContacto medio) {
    this.destinatario = null;
    this.mensaje = mensaje;
    this.medio = medio;
    this.completada = false;
  }

  public void agregarMedioContacto(MedioDeContacto medioDeContactoPredeterminado) {
    this.medio = medioDeContactoPredeterminado;
  }

  public void enviar() {
    if (this.medio != null) {
      this.medio.enviarMensaje(this);
      this.marcarCompletada();
    } else {
      throw new MedioContactoException("Agregue un medio de contacto predeterminado");
    }
  }

  public void marcarCompletada() {
    this.completada = true;
  }

  public String getMensaje() {
    return mensaje;
  }

  public Boolean getCompletada() {
    return completada;
  }
}
