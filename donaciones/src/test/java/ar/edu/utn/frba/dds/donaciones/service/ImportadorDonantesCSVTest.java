package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaHumana;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaJuridica;
import ar.edu.utn.frba.dds.donaciones.domain.Telefono;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del procesador de dominio, desacoplados del filesystem y del singleton:
 * el repositorio es un mock y las filas se construyen en memoria.
 */
class ImportadorDonantesCSVTest {

  private DonanteRepository repo;
  private ImportadorDonantesCSV importador;

  @BeforeEach
  void setUp() {
    repo = mock(DonanteRepository.class);
    importador = new ImportadorDonantesCSV(repo, new LectorDonantesCSV());
  }

  private FilaDonanteCSV filaHumana() {
    return new FilaDonanteCSV("HUMANA", "DNI", "12345678",
        "Ana Pérez", "ana@mail.com", "+54 11 5555-5555");
  }

  private FilaDonanteCSV filaJuridica() {
    return new FilaDonanteCSV("JURIDICA", "CUIT", "30-12345678-9",
        "Arcos Plateados S.A.", "contacto@empresa.com", "+54 11 4444-4444");
  }

  @Test
  void procesarCreaPersonaHumanaYJuridicaYLasGuarda() {
    when(repo.buscarPorEmail(anyString())).thenReturn(Optional.empty());

    List<PersonaDonante> procesados = importador.procesar(List.of(filaHumana(), filaJuridica()));

    assertEquals(2, procesados.size());
    assertTrue(procesados.get(0) instanceof PersonaHumana);
    assertTrue(procesados.get(1) instanceof PersonaJuridica);
    verify(repo, times(2)).guardar(any());
  }

  @Test
  void procesarNoDuplicaSiElEmailYaExisteYActualizaSusDatos() {
    Email email = new Email("ana@mail.com");
    Telefono telefonoViejo = new Telefono("000");
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, telefonoViejo));
    PersonaHumana existente = new PersonaHumana(medios, email,
        "Anita", "Apellidoviejo", null, 12345678, null, null);
    when(repo.buscarPorEmail("ana@mail.com")).thenReturn(Optional.of(existente));

    List<PersonaDonante> procesados = importador.procesar(List.of(filaHumana()));

    verify(repo, never()).guardar(any());
    assertEquals(1, procesados.size());
    assertEquals("Ana", existente.getNombre());
    assertEquals("Pérez", existente.getApellido());
    assertEquals("+54 11 5555-5555", telefonoViejo.getValor());
  }

  @Test
  void procesarIgnoraLasFilasDeTipoDesconocido() {
    when(repo.buscarPorEmail(anyString())).thenReturn(Optional.empty());
    FilaDonanteCSV filaInvalida = new FilaDonanteCSV("MARCIANA", "DNI", "1",
        "Zork", "zork@mail.com", "111");

    List<PersonaDonante> procesados = importador.procesar(List.of(filaInvalida));

    assertTrue(procesados.isEmpty());
    verify(repo, never()).guardar(any());
  }

  @Test
  void importarLeeElArchivoYProcesaSusFilas() {
    // Integración lector + procesador con el CSV de recursos; el repo sigue mockeado.
    when(repo.buscarPorEmail(anyString())).thenReturn(Optional.empty());

    List<PersonaDonante> procesados = importador.importar("src/test/resources/donantes_test.csv");

    assertEquals(2, procesados.size());
    verify(repo, times(2)).guardar(any());
  }
}
