package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaHumana;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaJuridica;
import ar.edu.utn.frba.dds.donaciones.domain.Telefono;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Procesa filas de donantes ya parseadas: crea los nuevos y actualiza los existentes
 * (deduplicando por email). El parseo del archivo es responsabilidad de LectorDonantesCSV;
 * esta clase solo conoce el dominio y el repositorio, que recibe por constructor.
 */
public class ImportadorDonantesCSV {

  private final DonanteRepository repo;
  private final LectorDonantesCSV lector;

  public ImportadorDonantesCSV(DonanteRepository repo, LectorDonantesCSV lector) {
    this.repo = repo;
    this.lector = lector;
  }

  public List<PersonaDonante> importar(String pathArchivo) {
    return procesar(lector.leerArchivo(pathArchivo));
  }

  public List<PersonaDonante> procesar(List<FilaDonanteCSV> filas) {
    List<PersonaDonante> procesados = new ArrayList<>();

    for (FilaDonanteCSV fila : filas) {
      Optional<PersonaDonante> existente = repo.buscarPorEmail(fila.email());

      if (existente.isPresent()) {
        procesados.add(actualizar(existente.get(), fila));
      } else {
        PersonaDonante nuevo = crear(fila);
        if (nuevo != null) {
          repo.guardar(nuevo);
          procesados.add(nuevo);
        }
      }
    }

    return procesados;
  }

  private PersonaDonante actualizar(PersonaDonante donante, FilaDonanteCSV fila) {
    donante.actualizarEmail(fila.email());
    donante.actualizarTelefono(fila.telefono());

    if (donante instanceof PersonaHumana humana) {
      String[] partes = fila.nombre().split(" ", 2);
      humana.setNombre(partes[0]);
      humana.setApellido(partes.length > 1 ? partes[1] : "");
    } else if (donante instanceof PersonaJuridica juridica) {
      juridica.setRazonSocial(fila.nombre());
    }

    return donante;
  }

  private PersonaDonante crear(FilaDonanteCSV fila) {
    Email email = new Email(fila.email());
    Telefono tel = new Telefono(fila.telefono());
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, tel));

    return switch (fila.tipo()) {
      case "HUMANA"   -> crearHumana(fila, medios, email);
      case "JURIDICA" -> crearJuridica(fila, medios, email);
      default -> {
        System.err.println("Tipo desconocido en fila: " + fila.tipo());
        yield null;
      }
    };
  }

  private PersonaHumana crearHumana(FilaDonanteCSV fila, List<MedioDeContacto> medios, Email email) {
    String[] partes = fila.nombre().split(" ", 2);
    String nombre   = partes[0];
    String apellido = partes.length > 1 ? partes[1] : "";
    Integer dni     = Integer.parseInt(fila.documento());

    return new PersonaHumana(
        medios, email,
        nombre, apellido,
        null,   // edad: no viene en el CSV
        dni,
        null,   // género: no viene en el CSV
        null    // dirección: no viene en el CSV
    );
  }

  private PersonaJuridica crearJuridica(FilaDonanteCSV fila, List<MedioDeContacto> medios, Email email) {
    return new PersonaJuridica(
        medios, email,
        fila.nombre(),
        null,           // TipoJuridico: no viene en el CSV
        null,           // rubro: no viene en el CSV
        new ArrayList<>()
    );
  }
}
