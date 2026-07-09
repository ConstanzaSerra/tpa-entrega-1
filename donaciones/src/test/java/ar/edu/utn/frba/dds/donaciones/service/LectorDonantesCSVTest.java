package ar.edu.utn.frba.dds.donaciones.service;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de parseo puro: entra texto CSV, salen filas. Sin archivos (salvo el caso de
 * integración con recursos), sin repositorio, sin dominio.
 */
class LectorDonantesCSVTest {

  private static final String ENCABEZADO =
      "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n";

  private final LectorDonantesCSV lector = new LectorDonantesCSV();

  private List<FilaDonanteCSV> leer(String contenido) {
    return lector.leer(new StringReader(contenido));
  }

  @Test
  void parseaLosCamposDeUnaFila() {
    List<FilaDonanteCSV> filas = leer(ENCABEZADO
        + "HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,+54 11 5555-5555\n");

    assertEquals(1, filas.size());
    FilaDonanteCSV fila = filas.get(0);
    assertEquals("HUMANA", fila.tipo());
    assertEquals("DNI", fila.tipoDocumento());
    assertEquals("12345678", fila.documento());
    assertEquals("Ana Pérez", fila.nombre());
    assertEquals("ana@mail.com", fila.email());
    assertEquals("+54 11 5555-5555", fila.telefono());
  }

  @Test
  void salteaElEncabezadoYLasLineasVacias() {
    List<FilaDonanteCSV> filas = leer(ENCABEZADO
        + "HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,111\n"
        + "\n"
        + "JURIDICA,CUIT,30-12345678-9,Arcos Plateados S.A.,contacto@empresa.com,222\n");

    assertEquals(2, filas.size());
    assertEquals("HUMANA", filas.get(0).tipo());
    assertEquals("JURIDICA", filas.get(1).tipo());
  }

  @Test
  void soportaComasDentroDeCamposEntreComillas() {
    // Caso que el split(",") a mano rompia: la razon social contiene una coma.
    List<FilaDonanteCSV> filas = leer(ENCABEZADO
        + "JURIDICA,CUIT,30-12345678-9,\"Arcos, Plateados S.A.\",contacto@empresa.com,222\n");

    assertEquals(1, filas.size());
    assertEquals("Arcos, Plateados S.A.", filas.get(0).nombre());
    assertEquals("contacto@empresa.com", filas.get(0).email());
  }

  @Test
  void recortaLosEspaciosAlrededorDeLosCampos() {
    List<FilaDonanteCSV> filas = leer(ENCABEZADO
        + "HUMANA , DNI , 12345678 , Ana Pérez , ana@mail.com , 111\n");

    assertEquals("HUMANA", filas.get(0).tipo());
    assertEquals("ana@mail.com", filas.get(0).email());
  }

  @Test
  void leerArchivoLeeElCsvDeRecursos() {
    List<FilaDonanteCSV> filas = lector.leerArchivo("src/test/resources/donantes_test.csv");

    assertEquals(2, filas.size());
    assertEquals("ana@mail.com", filas.get(0).email());
    assertEquals("contacto@empresa.com", filas.get(1).email());
  }
}
