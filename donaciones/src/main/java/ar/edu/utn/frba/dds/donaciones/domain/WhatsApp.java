package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.adaptadores.WhatsappSender;

public class WhatsApp extends MedioDeContacto {

  public WhatsApp(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje(String mensaje) {
    WhatsappSender.enviar(this.getValor(), mensaje);
  }
}
