package ar.edu.utn.frba.dds.donaciones.domain;

public abstract class MedioDeContacto {
  private String valor;  // private, no public

  public MedioDeContacto(String valor) {
    this.valor = valor;
  }

  public String getValor() {
    return valor;
  }

  public void setValor(String valor) {  // esto es lo que faltaba
    this.valor = valor;
  }

  public abstract void enviarMensaje(String mensaje);
}