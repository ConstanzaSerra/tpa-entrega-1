package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.adaptadores.SmsSender;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("TELEFONO")
public class Telefono extends MedioDeContacto {

  protected Telefono() {}

  public Telefono(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje(String mensaje) {
    SmsSender.enviar(this.getValor(), mensaje);
  }
}
