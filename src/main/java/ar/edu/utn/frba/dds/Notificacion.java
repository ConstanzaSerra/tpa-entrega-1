package ar.edu.utn.frba.dds;

public class Notificacion {
  private PersonaDonante destinatario;
  private String mensaje;
  private MedioDeContacto medio;
  private Boolean completada;

  public void enviar(){} //TODO - implementar
  public void marcarCompletada(){
    this.completada = true;
  }
}
