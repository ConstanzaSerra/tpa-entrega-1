package ar.edu.utn.frba.dds.donaciones.domain;

public class Telefono extends MedioDeContacto {
  public Telefono(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje(String mensaje) {
    System.out.println("Enviando SMS a " + this.getValor() + ": " + mensaje);
  }
}
