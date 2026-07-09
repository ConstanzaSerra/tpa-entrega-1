package ar.edu.utn.frba.dds.donaciones.adaptadores;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Adaptador de infraestructura para el envio real de SMS via Twilio.
 * Configuracion por variables de entorno:
 *   TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM (numero emisor en formato E.164, ej +14155551234)
 * Si no esta configurado, cae a modo simulado (imprime en consola). La dependencia con Twilio queda aislada aca.
 * Nota: el numero destino debe estar en formato E.164 (ej +5491122334455).
 */
public class SmsSender {

  private SmsSender() {
  }

  public static void enviar(String destino, String cuerpo) {
    String sid = System.getenv("TWILIO_ACCOUNT_SID");
    String token = System.getenv("TWILIO_AUTH_TOKEN");
    String from = System.getenv("TWILIO_FROM");

    if (sid == null || token == null || from == null) {
      System.out.println("[SMS simulado - Twilio no configurado] a " + destino + ": " + cuerpo);
      return;
    }

    try {
      Twilio.init(sid, token);
      Message.creator(new PhoneNumber(destino), new PhoneNumber(from), cuerpo).create();
      System.out.println("[SMS enviado] a " + destino);
    } catch (Exception e) {
      System.err.println("[SMS error] no se pudo enviar a " + destino + ": " + e.getMessage());
    }
  }
}
