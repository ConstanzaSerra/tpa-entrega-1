<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/service/ImportadorDonantesCSV.java
package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;
========
package ar.edu.utn.frba.dds.donantes;

import ar.edu.utn.frba.dds.contacto.Email;
import ar.edu.utn.frba.dds.contacto.MedioDeContacto;
import ar.edu.utn.frba.dds.contacto.Telefono;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/donantes/ImportadorDonantesCSV.java

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportadorDonantesCSV {

  private static final String SEPARADOR = ",";
  private static final int COL_TIPO     = 0;
  private static final int COL_TIPO_DOC = 1;
  private static final int COL_DOCUMENTO = 2;
  private static final int COL_NOMBRE   = 3;
  private static final int COL_EMAIL    = 4;
  private static final int COL_TELEFONO = 5;

  public List<PersonaDonante> importar(String pathArchivo) {
    List<PersonaDonante> procesados = new ArrayList<>();
    DonanteRepository repo = DonanteRepository.getInstancia();

    try (BufferedReader reader = new BufferedReader(new FileReader(pathArchivo))) {
      String linea;
      boolean primeraLinea = true;

      while ((linea = reader.readLine()) != null) {
        if (primeraLinea) {
          primeraLinea = false;
          continue;
        }
        if (linea.isBlank()) continue;

        String[] campos = linea.split(SEPARADOR);
        String email    = campos[COL_EMAIL].trim();

        Optional<PersonaDonante> existente = repo.buscarPorEmail(email);

        if (existente.isPresent()) {
          PersonaDonante donante = existente.get();
          String nuevoNombre = campos[COL_NOMBRE].trim();
          String nuevoTelefono = campos[COL_TELEFONO].trim();

          donante.actualizarEmail(campos[COL_EMAIL].trim());
          donante.actualizarTelefono(nuevoTelefono);

          if (donante instanceof PersonaHumana humana) {
            String[] partes = nuevoNombre.split(" ", 2);
            humana.setNombre(partes[0]);
            humana.setApellido(partes.length > 1 ? partes[1] : "");
          } else if (donante instanceof PersonaJuridica juridica) {
            juridica.setRazonSocial(nuevoNombre);
          }

          procesados.add(donante);
        } else {
          PersonaDonante nuevo = parsearLinea(campos);
          if (nuevo != null) {
            repo.guardar(nuevo);
            procesados.add(nuevo);
          }
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Error al leer el archivo CSV: " + pathArchivo, e);
    }

    return procesados;
  }

  private PersonaDonante parsearLinea(String[] campos) {
    String tipo = campos[COL_TIPO].trim();

    Email email     = new Email(campos[COL_EMAIL].trim());
    Telefono tel    = new Telefono(campos[COL_TELEFONO].trim());
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, tel));

    return switch (tipo) {
      case "HUMANA"   -> parsearHumana(campos, medios, email);
      case "JURIDICA" -> parsearJuridica(campos, medios, email);
      default -> {
        System.err.println("Tipo desconocido en línea: " + tipo);
        yield null;
      }
    };
  }

  private PersonaHumana parsearHumana(String[] campos, List<MedioDeContacto> medios, Email email) {
    String nombreCompleto = campos[COL_NOMBRE].trim();
    String[] partes       = nombreCompleto.split(" ", 2);
    String nombre         = partes[0];
    String apellido       = partes.length > 1 ? partes[1] : "";
    Integer dni           = Integer.parseInt(campos[COL_DOCUMENTO].trim());

    return new PersonaHumana(
        medios, email,
        nombre, apellido,
        null,   // edad: no viene en el CSV
        dni,
        null,   // género: no viene en el CSV
        null    // dirección: no viene en el CSV
    );
  }

  private PersonaJuridica parsearJuridica(String[] campos, List<MedioDeContacto> medios, Email email) {
    String razonSocial = campos[COL_NOMBRE].trim();

    return new PersonaJuridica(
        medios, email,
        razonSocial,
        null,           // TipoJuridico: no viene en el CSV
        null,           // rubro: no viene en el CSV
        new ArrayList<>()
    );
  }
}
