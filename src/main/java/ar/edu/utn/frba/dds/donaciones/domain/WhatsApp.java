package ar.edu.utn.frba.dds.donaciones.domain;

public class WhatsApp extends MedioDeContacto {

  public WhatsApp(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje() {
    System.out.println("Enviando whatsapp a " + this.getValor());
  }
}
