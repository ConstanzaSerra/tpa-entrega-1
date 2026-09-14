package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.adaptadores.WhatsappSender;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("WHATSAPP")
public class WhatsApp extends MedioDeContacto {

  protected WhatsApp() {}

  public WhatsApp(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje(String mensaje) {
    WhatsappSender.enviar(this.getValor(), mensaje);
  }
}
