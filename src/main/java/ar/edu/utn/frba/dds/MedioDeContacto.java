package ar.edu.utn.frba.dds;

public abstract class MedioDeContacto {
  public String valor;

  public MedioDeContacto(String valor) {
    this.valor = valor;
  }

  public String getValor() {
    return valor;
  }

  public abstract void enviarMensaje(); //TODO - creé este metodo abstracto porque todas las subclases lo usan

}
