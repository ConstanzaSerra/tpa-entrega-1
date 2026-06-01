package ar.edu.utn.frba.dds;

public class WhatsApp extends MedioDeContacto {

  public WhatsApp(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje() {
    System.out.println("Enviando whatsapp a " + getValor());
  }
}
