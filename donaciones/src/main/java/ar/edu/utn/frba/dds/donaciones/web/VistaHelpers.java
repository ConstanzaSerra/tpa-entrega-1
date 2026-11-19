package ar.edu.utn.frba.dds.donaciones.web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers de presentacion para las plantillas: traducen los valores tal como los guarda
 * el dominio (enums, fechas ISO, numeros) a como los lee una persona.
 *
 *   {{estadoTexto estado}}  ASIGNACION_REALIZADA      ->  Asignada
 *   {{estadoClase estado}}  ASIGNACION_REALIZADA      ->  estado-asignada   (color del badge)
 *   {{legible categoria}}   ALIMENTOS                 ->  Alimentos
 *   {{fecha valor}}         2026-11-17T11:02:37.418   ->  17/11/2026 11:02
 *   {{numero total}}        10512                     ->  10.512
 *
 * La logica vive en metodos estaticos puros (testeables sin Handlebars) y register()
 * solo los conecta con el motor. Asi el formato no se repite en cada controlador.
 */
public final class VistaHelpers {

  private static final Locale ES_AR = new Locale("es", "AR");

  private static final Map<String, String> TEXTO_ESTADO = Map.ofEntries(
      // Donacion
      Map.entry("EN_DEPOSITO", "En depósito"),
      Map.entry("ASIGNACION_REALIZADA", "Asignada"),
      Map.entry("LISTA_PARA_ENTREGAR", "Lista para entregar"),
      Map.entry("EN_TRASLADO", "En camino"),
      Map.entry("ENTREGADA", "Entregada"),
      Map.entry("ENTREGA_FALLIDA", "Entrega fallida"),
      Map.entry("VENCIDA", "Vencida"),
      // Ruta y entrega (servicio de logistica)
      Map.entry("PLANIFICADA", "Planificada"),
      Map.entry("EN_CURSO", "En curso"),
      Map.entry("COMPLETADA", "Completada"),
      Map.entry("PENDIENTE", "Pendiente"),
      Map.entry("NO_RECIBIDA", "No recibida"));

  private static final Map<String, String> CLASE_ESTADO = Map.ofEntries(
      Map.entry("EN_DEPOSITO", "estado-deposito"),
      Map.entry("PLANIFICADA", "estado-deposito"),
      Map.entry("PENDIENTE", "estado-deposito"),
      Map.entry("ASIGNACION_REALIZADA", "estado-asignada"),
      Map.entry("LISTA_PARA_ENTREGAR", "estado-asignada"),
      Map.entry("EN_TRASLADO", "estado-camino"),
      Map.entry("EN_CURSO", "estado-camino"),
      Map.entry("ENTREGADA", "estado-entregada"),
      Map.entry("COMPLETADA", "estado-entregada"),
      Map.entry("ENTREGA_FALLIDA", "estado-fallida"),
      Map.entry("NO_RECIBIDA", "estado-fallida"),
      Map.entry("VENCIDA", "estado-vencida"));

  private static final Pattern FECHA_ISO =
      Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2}).*)?$");

  private VistaHelpers() {}

  public static void register(Handlebars handlebars) {
    handlebars.registerHelper("estadoTexto", (Helper<Object>) (valor, options) -> estadoTexto(texto(valor)));
    handlebars.registerHelper("estadoClase", (Helper<Object>) (valor, options) -> estadoClase(texto(valor)));
    handlebars.registerHelper("legible", (Helper<Object>) (valor, options) -> legible(texto(valor)));
    handlebars.registerHelper("fecha", (Helper<Object>) (valor, options) -> fecha(texto(valor)));
    handlebars.registerHelper("numero", (Helper<Object>) (valor, options) -> numero(valor));
  }

  /** Solo valores simples: si el helper recibe un mapa u otro objeto (dato faltante), no muestra nada. */
  private static String texto(Object valor) {
    if (valor instanceof CharSequence || valor instanceof Enum || valor instanceof Number
        || valor instanceof java.time.temporal.Temporal) {
      return valor.toString();
    }
    return "";
  }

  public static String estadoTexto(String estado) {
    if (estado == null || estado.isBlank()) {
      return "";
    }
    return TEXTO_ESTADO.getOrDefault(estado.trim(), legible(estado));
  }

  public static String estadoClase(String estado) {
    if (estado == null) {
      return "estado-otro";
    }
    return CLASE_ESTADO.getOrDefault(estado.trim(), "estado-otro");
  }

  /** ALIMENTOS -> Alimentos, CONSTRUCCION_Y_HOGAR -> Construccion y hogar. Respeta textos ya legibles. */
  public static String legible(String valor) {
    if (valor == null || valor.isBlank()) {
      return "";
    }
    String limpio = valor.trim();
    if (!limpio.equals(limpio.toUpperCase(ES_AR))) {
      return limpio;
    }
    String minusculas = limpio.replace('_', ' ').toLowerCase(ES_AR);
    return Character.toUpperCase(minusculas.charAt(0)) + minusculas.substring(1);
  }

  /** 2026-11-17 -> 17/11/2026 ; 2026-11-17T11:02:37.418 -> 17/11/2026 11:02. Otros textos quedan igual. */
  public static String fecha(String valor) {
    if (valor == null || valor.isBlank()) {
      return "";
    }
    Matcher m = FECHA_ISO.matcher(valor.trim());
    if (!m.matches()) {
      return valor;
    }
    String dia = m.group(3) + "/" + m.group(2) + "/" + m.group(1);
    return m.group(4) == null ? dia : dia + " " + m.group(4) + ":" + m.group(5);
  }

  /** 10512 -> 10.512 ; 42.0 -> 42 ; 3.25 -> 3,3. Lo que no es numero queda igual. */
  public static String numero(Object valor) {
    BigDecimal n;
    if (valor instanceof Number) {
      n = new BigDecimal(valor.toString());
    } else if (valor instanceof CharSequence) {
      try {
        n = new BigDecimal(valor.toString().trim());
      } catch (NumberFormatException e) {
        return valor.toString();
      }
    } else {
      return "";
    }
    DecimalFormat formato = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(ES_AR));
    return formato.format(n.setScale(1, RoundingMode.HALF_UP));
  }
}
