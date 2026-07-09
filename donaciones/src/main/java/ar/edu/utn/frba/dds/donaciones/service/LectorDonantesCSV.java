package ar.edu.utn.frba.dds.donaciones.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un CSV de donantes y lo convierte en filas neutras (FilaDonanteCSV).
 * No conoce el dominio ni la persistencia: solo parsea.
 * Delega el parseo en la biblioteca Apache Commons CSV, que resuelve casos que un
 * split(",") a mano no cubre: comas dentro de campos entre comillas, lineas vacias
 * y espacios alrededor de los valores.
 */
public class LectorDonantesCSV {

  private static final CSVFormat FORMATO = CSVFormat.DEFAULT.builder()
      .setHeader()                // toma la primera linea como encabezado...
      .setSkipHeaderRecord(true)  // ...y no la devuelve como dato
      .setIgnoreEmptyLines(true)
      .setTrim(true)
      .build();

  private static final int COL_TIPO      = 0;
  private static final int COL_TIPO_DOC  = 1;
  private static final int COL_DOCUMENTO = 2;
  private static final int COL_NOMBRE    = 3;
  private static final int COL_EMAIL     = 4;
  private static final int COL_TELEFONO  = 5;

  public List<FilaDonanteCSV> leer(Reader origen) {
    try (CSVParser parser = FORMATO.parse(origen)) {
      List<FilaDonanteCSV> filas = new ArrayList<>();
      for (CSVRecord registro : parser) {
        filas.add(new FilaDonanteCSV(
            registro.get(COL_TIPO),
            registro.get(COL_TIPO_DOC),
            registro.get(COL_DOCUMENTO),
            registro.get(COL_NOMBRE),
            registro.get(COL_EMAIL),
            registro.get(COL_TELEFONO)));
      }
      return filas;
    } catch (IOException e) {
      throw new RuntimeException("Error al leer el CSV de donantes", e);
    }
  }

  public List<FilaDonanteCSV> leerArchivo(String pathArchivo) {
    try (Reader reader = new FileReader(pathArchivo, StandardCharsets.UTF_8)) {
      return leer(reader);
    } catch (IOException e) {
      throw new RuntimeException("Error al abrir el archivo CSV: " + pathArchivo, e);
    }
  }
}
