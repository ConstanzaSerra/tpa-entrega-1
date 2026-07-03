package ar.edu.utn.frba.dds.contacto;

import java.util.regex.Pattern;

public class Email extends MedioDeContacto {
  private static final Pattern FORMATO_EMAIL = Pattern.compile(
      "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
  );

  public Email(String valor) {
    super(validar(valor));
  }

  @Override
  public void enviarMensaje(Notificacion notificacion) {
    System.out.println("Enviando email a " + getValor() + ": " + notificacion.getMensaje());
  }

  private static String validar(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("El email no puede estar vacio");
    }

    String emailNormalizado = valor.trim();
    if (!FORMATO_EMAIL.matcher(emailNormalizado).matches()) {
      throw new IllegalArgumentException("El email no tiene un formato valido");
    }

    return emailNormalizado;
  }
}
