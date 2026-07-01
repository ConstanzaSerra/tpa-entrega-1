package ar.edu.utn.frba.dds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TareaProgramadaTest {

  @Test
  public void testTareaProgramadaTest() throws InterruptedException {
    DonacionRepository repoDonaciones = new DonacionRepository();
    EntidadRepository repoEntidades = new EntidadRepository();
    PropuestaRepository repoPropuestas = new PropuestaRepository();
    ProcesadorMatchmaking procesador = new ProcesadorMatchmaking();

    Subcategoria fideos = new Subcategoria("Fideos",Categoria.ALIMENTOS, false, true);
    Donacion donacionPendiente = new Donacion(fideos, 50, "Kg", null, LocalDate.now());
    repoDonaciones.guardar(donacionPendiente);

    NecesidadExtraordinaria n = new NecesidadExtraordinaria(fideos, "",2,1);
    EntidadBeneficiaria comedor = new EntidadBeneficiaria("Comedor", "Dir", "123", new ArrayList<>(), new ArrayList<>(List.of(n)));
    repoEntidades.guardar(comedor);

    // Creacion tarea
    TareaNocturnaMatchmaking tarea = new TareaNocturnaMatchmaking(
        repoDonaciones, repoEntidades, repoPropuestas, procesador
    );

    // La tarea ejecuta cada 1 segundo para hacer el test
    tarea.iniciarSchedulerParaTest(1);

    // Tiene una espera 1.5 segundos
    Thread.sleep(1500);

    tarea.detener();

    List<PropuestaMatchmaking> propuestasGeneradas = repoPropuestas.obtenerTodas();

    assertFalse(propuestasGeneradas.isEmpty(), "El scheduler debería haber generado al menos una propuesta");
    assertEquals(2, propuestasGeneradas.size(), "Debería haber  1 o más propuestas en la bandeja del admin");
    assertEquals(fideos, propuestasGeneradas.get(0).getDonacion().getSubcategoria());
  }
}
