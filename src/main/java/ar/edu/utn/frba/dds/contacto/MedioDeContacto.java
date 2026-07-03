<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/MedioDeContacto.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.contacto;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/contacto/MedioDeContacto.java

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
