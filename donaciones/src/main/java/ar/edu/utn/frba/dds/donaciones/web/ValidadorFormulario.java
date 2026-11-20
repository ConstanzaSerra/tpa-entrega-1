package ar.edu.utn.frba.dds.donaciones.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Valida los datos de un formulario y junta TODOS los errores, uno por campo, en vez de
 * cortar en el primero. Asi la pantalla puede mostrarlos juntos, cada uno debajo de su campo.
 *
 * La validacion del navegador (required, pattern) es solo una ayuda: se puede saltear
 * enviando el formulario a mano, por eso el servidor valida de nuevo con esta clase.
 *
 *   ValidadorFormulario v = new ValidadorFormulario()
 *       .obligatorio("razonSocial", razonSocial, "la razón social")
 *       .telefono("telefono", telefono);
 *   if (v.hayErrores()) { ... v.errores() ... }
 */
public final class ValidadorFormulario {

  /** Digitos, espacios, guiones y parentesis, con un + opcional al principio: +54 11 4444-5555, (011) 4444-5555. */
  private static final Pattern CARACTERES_TELEFONO = Pattern.compile("^\\+?\\(?[0-9][0-9 ()\\-]*$");
  /** Mismo criterio que la clase de dominio Email. */
  private static final Pattern FORMATO_EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final int MIN_DIGITOS_TELEFONO = 6;
  private static final int MAX_DIGITOS_TELEFONO = 15;

  private final Map<String, String> errores = new LinkedHashMap<>();

  public ValidadorFormulario obligatorio(String campo, String valor, String nombreVisible) {
    if (valor == null || valor.isBlank()) {
      agregar(campo, "Completá " + nombreVisible + ".");
    }
    return this;
  }

  /** Si el campo vino vacio no agrega error: para eso esta obligatorio(). */
  public ValidadorFormulario email(String campo, String valor) {
    if (tieneTexto(valor) && !FORMATO_EMAIL.matcher(valor.trim()).matches()) {
      agregar(campo, "Ingresá un correo válido, por ejemplo nombre@dominio.org.");
    }
    return this;
  }

  public ValidadorFormulario telefono(String campo, String valor) {
    if (tieneTexto(valor) && !esTelefonoValido(valor)) {
      agregar(campo, "Ingresá un teléfono válido: solo números, espacios, guiones o paréntesis "
          + "(entre " + MIN_DIGITOS_TELEFONO + " y " + MAX_DIGITOS_TELEFONO + " dígitos), por ejemplo +54 11 4444-5555.");
    }
    return this;
  }

  public ValidadorFormulario longitudMinima(String campo, String valor, int minimo, String nombreVisible) {
    if (tieneTexto(valor) && valor.length() < minimo) {
      agregar(campo, capitalizar(nombreVisible) + " tiene que tener al menos " + minimo + " caracteres.");
    }
    return this;
  }

  public ValidadorFormulario enteroEntre(String campo, String valor, long minimo, long maximo, String mensaje) {
    if (!tieneTexto(valor)) {
      return this;
    }
    try {
      long numero = Long.parseLong(valor.trim());
      if (numero < minimo || numero > maximo) {
        agregar(campo, mensaje);
      }
    } catch (NumberFormatException e) {
      agregar(campo, mensaje);
    }
    return this;
  }

  /** Para reglas que no son de formato (ej. "ya existe una cuenta con este correo"). */
  public ValidadorFormulario agregar(String campo, String mensaje) {
    errores.putIfAbsent(campo, mensaje);
    return this;
  }

  public boolean hayErrores() {
    return !errores.isEmpty();
  }

  public Map<String, String> errores() {
    return Collections.unmodifiableMap(errores);
  }

  /** Todos los errores en un solo texto, para pantallas que no los muestran campo por campo. */
  public String resumen() {
    return String.join(" ", errores.values());
  }

  public static boolean esTelefonoValido(String valor) {
    if (!tieneTexto(valor) || !CARACTERES_TELEFONO.matcher(valor.trim()).matches()) {
      return false;
    }
    long digitos = valor.chars().filter(Character::isDigit).count();
    return digitos >= MIN_DIGITOS_TELEFONO && digitos <= MAX_DIGITOS_TELEFONO;
  }

  private static boolean tieneTexto(String valor) {
    return valor != null && !valor.isBlank();
  }

  private static String capitalizar(String texto) {
    return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
  }
}
