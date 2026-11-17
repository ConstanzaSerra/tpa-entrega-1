package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaHumana;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaJuridica;
import ar.edu.utn.frba.dds.donaciones.domain.RolUsuario;
import ar.edu.utn.frba.dds.donaciones.domain.Telefono;
import ar.edu.utn.frba.dds.donaciones.domain.Usuario;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.UsuarioRepository;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

  /**
   * Requerimiento 17 (Entrega 4): Importación masiva desde CSV (>10.000 filas)
   * otorgando un Usuario de acceso a cada persona donante importada.
   * Optimizado en memoria y una única transacción para procesar +10.000 filas en segundos.
   */
  public ResultadoImportacion importarMasivoConUsuarios(Reader reader, UsuarioRepository usuarioRepository) {
    List<FilaDonanteCSV> filas = lector.leer(reader);

    Map<String, PersonaDonante> donantesPorEmail = new HashMap<>();
    for (PersonaDonante d : repo.obtenerTodos()) {
      if (d.getMedioDeContactoPredeterminado() != null && d.getMedioDeContactoPredeterminado().getValor() != null) {
        donantesPorEmail.put(d.getMedioDeContactoPredeterminado().getValor().trim().toLowerCase(), d);
      }
    }

    Set<String> emailsConUsuario = new HashSet<>();
    if (usuarioRepository != null) {
      for (Usuario u : usuarioRepository.obtenerTodos()) {
        if (u.getEmail() != null) {
          emailsConUsuario.add(u.getEmail().trim().toLowerCase());
        }
      }
    }

    int[] contadores = new int[3]; // [0]=creados, [1]=actualizados, [2]=usuariosOtorgados

    List<ErrorDeFila> errores = new ArrayList<>();

    repo.ejecutarEnTransaccion(() -> {
      for (int indice = 0; indice < filas.size(); indice++) {
        FilaDonanteCSV fila = filas.get(indice);
        // +2 porque el archivo arranca con el encabezado y las lineas se
        // cuentan desde 1: asi el numero coincide con lo que ve la persona
        // al abrir el CSV en una planilla.
        int linea = indice + 2;
        String referencia = referenciaDe(fila);

        try {
          if (fila.email() == null || fila.email().isBlank()) {
            errores.add(new ErrorDeFila(linea, referencia, "Falta el email, que es el dato con el que se identifica a la persona donante"));
            continue;
          }
          String emailKey = fila.email().trim().toLowerCase();
          PersonaDonante donante = donantesPorEmail.get(emailKey);

          if (donante != null) {
            actualizar(donante, fila);
            contadores[1]++;
          } else {
            donante = crear(fila);
            if (donante == null) {
              errores.add(new ErrorDeFila(linea, referencia,
                  "Tipo de persona desconocido: '" + fila.tipo() + "'. Se espera HUMANA o JURIDICA"));
              continue;
            }
            repo.guardar(donante);
            donantesPorEmail.put(emailKey, donante);
            contadores[0]++;
          }

          if (donante != null && usuarioRepository != null && !emailsConUsuario.contains(emailKey)) {
            String passInicial = (fila.documento() != null && !fila.documento().isBlank())
                ? fila.documento().trim()
                : "donatrack123";
            Usuario nuevoUsuario = new Usuario(
                emailKey,
                passInicial,
                fila.nombre(),
                RolUsuario.DONANTE,
                donante,
                null
            );
            usuarioRepository.guardar(nuevoUsuario);
            emailsConUsuario.add(emailKey);
            contadores[2]++;
          }

        } catch (RuntimeException e) {
          // Una fila invalida no puede tumbar la importacion entera: se anota
          // con su numero de linea y el proceso sigue con las demas.
          errores.add(new ErrorDeFila(linea, referencia, mensajeDe(e)));
        }
      }
    });

    return new ResultadoImportacion(
        filas.size(), contadores[0], contadores[1], contadores[2], List.copyOf(errores));
  }

  /** Algo con que ubicar la fila en el archivo, ademas del numero de linea. */
  private String referenciaDe(FilaDonanteCSV fila) {
    if (fila.email() != null && !fila.email().isBlank()) {
      return fila.email().trim();
    }
    if (fila.nombre() != null && !fila.nombre().isBlank()) {
      return fila.nombre().trim();
    }
    return "(fila sin email ni nombre)";
  }

  private String mensajeDe(RuntimeException e) {
    String mensaje = e.getMessage();
    return (mensaje == null || mensaje.isBlank()) ? e.getClass().getSimpleName() : mensaje;
  }

  /** Una fila que no se pudo importar, con lo necesario para encontrarla y corregirla. */
  public record ErrorDeFila(int linea, String referencia, String motivo) {}

  public record ResultadoImportacion(int filasLeidas, int donantesCreados, int donantesActualizados,
                                     int usuariosOtorgados, List<ErrorDeFila> errores) {
    public int filasConError() {
      return errores.size();
    }

    public boolean tuvoErrores() {
      return !errores.isEmpty();
    }

    public int filasImportadas() {
      return donantesCreados + donantesActualizados;
    }
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
    String soloDigitos = fila.documento() != null ? fila.documento().replaceAll("[^0-9]", "") : "0";
    Integer dni     = soloDigitos.isEmpty() ? 0 : Integer.parseInt(soloDigitos.substring(0, Math.min(soloDigitos.length(), 9)));

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
