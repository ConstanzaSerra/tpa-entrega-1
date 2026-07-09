package ar.edu.utn.frba.dds.donaciones.adaptadores;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Adaptador de infraestructura para el envio real de mensajes de WhatsApp via Twilio.
 * Configuracion por variables de entorno:
 *   TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN (los mismos que SMS),
 *   TWILIO_WHATSAPP_FROM (numero emisor habilitado para WhatsApp, ej el del sandbox +14155238886)
 * WhatsApp usa el mismo SDK que SMS, con el prefijo "whatsapp:" en los numeros.
 * Si no esta configurado, cae a modo simulado (imprime en consola).
 */
public class WhatsappSender {

  private WhatsappSender() {
  }

  public static void enviar(String destino, String cuerpo) {
    String sid = System.getenv("TWILIO_ACCOUNT_SID");
    String token = System.getenv("TWILIO_AUTH_TOKEN");
    String from = System.getenv("TWILIO_WHATSAPP_FROM");

    if (sid == null || token == null || from == null) {
      System.out.println("[WhatsApp simulado - Twilio no configurado] a " + destino + ": " + cuerpo);
      return;
    }

    try {
      Twilio.init(sid, token);
      Message.creator(
          new PhoneNumber("whatsapp:" + destino),
          new PhoneNumber("whatsapp:" + from),
          cuerpo).create();
      System.out.println("[WhatsApp enviado] a " + destino);
    } catch (Exception e) {
      System.err.println("[WhatsApp error] no se pudo enviar a " + destino + ": " + e.getMessage());
    }
  }
}
