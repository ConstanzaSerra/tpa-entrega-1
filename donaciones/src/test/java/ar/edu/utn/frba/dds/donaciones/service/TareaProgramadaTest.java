package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;
import ar.edu.utn.frba.dds.donaciones.service.*;

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
    SubcategoriaRepository repoSubcategorias = new SubcategoriaRepository();
    PropuestaRepository repoPropuestas = new PropuestaRepository();
    ProcesadorMatchmaking procesador = new ProcesadorMatchmaking();

    // Subcategoria es una entidad compartida: se guarda por si misma antes de
    // que la referencien la donacion y la necesidad. No se cascadea desde
    // Donacion a proposito, para no duplicar la misma subcategoria por donacion.
    Subcategoria fideos = new Subcategoria("Fideos", Categoria.ALIMENTOS, false, true);
    repoSubcategorias.guardar(fideos);

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
