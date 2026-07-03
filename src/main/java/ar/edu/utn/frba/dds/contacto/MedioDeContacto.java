package ar.edu.utn.frba.dds.contacto;

public abstract class MedioDeContacto {
  private String valor;

  public MedioDeContacto(String valor) {
    this.valor = valor;
  }

  public String getValor() {
    return valor;
  }

  public void setValor(String valor) {
    this.valor = valor;
  }

  public abstract void enviarMensaje(Notificacion notificacion);
}
