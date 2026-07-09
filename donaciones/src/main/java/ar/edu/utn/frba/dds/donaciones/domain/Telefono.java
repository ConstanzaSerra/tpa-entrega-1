package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.adaptadores.SmsSender;

public class Telefono extends MedioDeContacto {
  public Telefono(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje(String mensaje) {
    SmsSender.enviar(this.getValor(), mensaje);
  }
}
