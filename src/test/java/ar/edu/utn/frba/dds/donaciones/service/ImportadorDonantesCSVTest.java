package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;
import ar.edu.utn.frba.dds.donaciones.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportadorDonantesCSVTest {

  private ImportadorDonantesCSV importador;

  @BeforeEach
  void setUp() {
    importador = new ImportadorDonantesCSV();
    DonanteRepository.getInstancia().limpiar();
  }

  @Test
  void importarArchivoValidoCreaLosDonantesEnElRepositorio() {
    importador.importar("src/test/resources/donantes_test.csv");

    List<PersonaDonante> donantes = DonanteRepository.getInstancia().obtenerTodos();
    assertFalse(donantes.isEmpty());
  }

  @Test
  void importarNoCreaDuplicadosSiElEmailYaExiste() {
    importador.importar("src/test/resources/donantes_test.csv");
    int cantidadPrimeraImportacion = DonanteRepository.getInstancia().cantidadDonantes();

    importador.importar("src/test/resources/donantes_test.csv");
    int cantidadSegundaImportacion = DonanteRepository.getInstancia().cantidadDonantes();

    assertEquals(cantidadPrimeraImportacion, cantidadSegundaImportacion);
  }

  @Test
  void importarCreaBienPersonaHumanaYPersonaJuridica() {
    importador.importar("src/test/resources/donantes_test.csv");

    List<PersonaDonante> donantes = DonanteRepository.getInstancia().obtenerTodos();

    assertTrue(donantes.stream().anyMatch(d -> d instanceof PersonaHumana));
    assertTrue(donantes.stream().anyMatch(d -> d instanceof PersonaJuridica));
  }
}