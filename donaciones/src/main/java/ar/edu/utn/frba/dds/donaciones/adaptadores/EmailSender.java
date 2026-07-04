package ar.edu.utn.frba.dds.donaciones.adaptadores;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Adaptador de infraestructura para el envio real de emails por SMTP.
 * La configuracion se lee de variables de entorno:
 *   SMTP_HOST, SMTP_PORT (default 587), SMTP_USER, SMTP_PASSWORD, SMTP_FROM (default = SMTP_USER)
 * Si no esta configurado, cae a modo simulado (imprime en consola) para no romper el arranque
 * ni exigir credenciales en desarrollo. La dependencia con Jakarta Mail queda aislada aca.
 */
public class EmailSender {

  private static final String ASUNTO = "DonaTrack - Notificación";

  private EmailSender() {
  }

  public static void enviar(String destino, String cuerpo) {
    String host = System.getenv("SMTP_HOST");
    String usuario = System.getenv("SMTP_USER");
    String password = System.getenv("SMTP_PASSWORD");
    String puerto = envOrDefault("SMTP_PORT", "587");
    String from = envOrDefault("SMTP_FROM", usuario != null ? usuario : "no-reply@donatrack.org");

    if (host == null || usuario == null || password == null) {
      System.out.println("[EMAIL simulado - SMTP no configurado] a " + destino + ": " + cuerpo);
      return;
    }

    try {
      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", puerto);

      Session session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(usuario, password);
        }
      });

      Message mensaje = new MimeMessage(session);
      mensaje.setFrom(new InternetAddress(from));
      mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
      mensaje.setSubject(ASUNTO);
      mensaje.setText(cuerpo);

      Transport.send(mensaje);
      System.out.println("[EMAIL enviado] a " + destino);
    } catch (Exception e) {
      System.err.println("[EMAIL error] no se pudo enviar a " + destino + ": " + e.getMessage());
    }
  }

  private static String envOrDefault(String clave, String porDefecto) {
    String valor = System.getenv(clave);
    return valor != null ? valor : porDefecto;
  }
}
