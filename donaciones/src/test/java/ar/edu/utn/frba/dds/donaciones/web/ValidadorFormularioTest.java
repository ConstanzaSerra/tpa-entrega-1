package ar.edu.utn.frba.dds.donaciones.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorFormularioTest {

  @Test
  void unTelefonoConLetrasNoEsValido() {
    assertFalse(ValidadorFormulario.esTelefonoValido("S"));
    assertFalse(ValidadorFormulario.esTelefonoValido("11 4444-555S"));
  }

  @Test
  void aceptaLosFormatosHabitualesDeTelefono() {
    assertTrue(ValidadorFormulario.esTelefonoValido("+54 11 4444-5555"));
    assertTrue(ValidadorFormulario.esTelefonoValido("(011) 4444-5555"));
    assertTrue(ValidadorFormulario.esTelefonoValido("1144445555"));
  }

  @Test
  void unTelefonoDemasiadoCortoOLargoNoEsValido() {
    assertFalse(ValidadorFormulario.esTelefonoValido("12345"));
    assertFalse(ValidadorFormulario.esTelefonoValido("1234567890123456"));
    assertFalse(ValidadorFormulario.esTelefonoValido("+-----"));
  }

  @Test
  void juntaTodosLosErroresALaVezUnoPorCampo() {
    ValidadorFormulario v = new ValidadorFormulario()
        .obligatorio("razonSocial", "", "la razón social")
        .obligatorio("direccion", " ", "la dirección")
        .obligatorio("telefono", "S", "el teléfono").telefono("telefono", "S")
        .obligatorio("email", "sin-arroba", "el correo").email("email", "sin-arroba")
        .obligatorio("password", "123", "la contraseña").longitudMinima("password", "123", 6, "la contraseña");

    assertTrue(v.hayErrores());
    assertEquals(List.of("razonSocial", "direccion", "telefono", "email", "password"), List.copyOf(v.errores().keySet()));
    assertEquals("La contraseña tiene que tener al menos 6 caracteres.", v.errores().get("password"));
  }

  @Test
  void elPrimerErrorDeUnCampoEsElQueSeMuestra() {
    ValidadorFormulario v = new ValidadorFormulario()
        .obligatorio("telefono", "", "el teléfono")
        .telefono("telefono", "");
    assertEquals("Completá el teléfono.", v.errores().get("telefono"));
  }

  @Test
  void unFormularioCorrectoNoTieneErrores() {
    ValidadorFormulario v = new ValidadorFormulario()
        .obligatorio("razonSocial", "Comedor Esperanza", "la razón social")
        .telefono("telefono", "+54 11 4444-5555")
        .email("email", "contacto@esperanza.org")
        .longitudMinima("password", "segura123", 6, "la contraseña")
        .enteroEntre("dni", "32000111", 1_000_000, 99_999_999, "Ingresá un DNI válido.");
    assertFalse(v.hayErrores());
  }

  @Test
  void unDniConLetrasOFueraDeRangoNoEsValido() {
    assertTrue(new ValidadorFormulario().enteroEntre("dni", "32abc", 1_000_000, 99_999_999, "DNI inválido").hayErrores());
    assertTrue(new ValidadorFormulario().enteroEntre("dni", "12", 1_000_000, 99_999_999, "DNI inválido").hayErrores());
  }
}
