package ar.edu.utn.frba.dds.donaciones.domain;

public class Telefono extends MedioDeContacto {
  public Telefono(String valor) {
    super(valor);
  }

  @Override
  public void enviarMensaje() {
    System.out.println("Enviando mensaje a " + this.getValor());
  }
}
