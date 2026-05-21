package ar.edu.utn.frba.dds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

  @Test
  void creaEmailValidoNormalizandoEspacios() {
    Email email = new Email(" persona@mail.com ");

    assertEquals("persona@mail.com", email.getValor());
  }

  @Test
  void noPermiteEmailConFormatoInvalido() {
    assertThrows(IllegalArgumentException.class, () -> new Email("persona-mail.com"));
  }

  @Test
  void noPermiteEmailVacio() {
    assertThrows(IllegalArgumentException.class, () -> new Email(" "));
  }
}
