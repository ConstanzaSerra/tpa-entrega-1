package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TareaProgramadaTest {

  @Test
  public void testTareaProgramadaGeneraPropuestasCorrectamente() {

    DonacionRepository repoDonaciones = new DonacionRepository();
    EntidadRepository repoEntidades = new EntidadRepository();
    SubcategoriaRepository repoSubcategorias = new SubcategoriaRepository();
    PropuestaRepository repoPropuestas = new PropuestaRepository();

    List<AlgoritmoDonacion> algoritmosActivos = List.of(
        new CompatibilidadSemantica(),
        new PrioridadSubatendidos()
    );
    ProcesadorMatchmaking procesador = new ProcesadorMatchmaking(algoritmosActivos);

    Subcategoria fideos = new Subcategoria("Fideos", Categoria.ALIMENTOS, false, true);
    repoSubcategorias.guardar(fideos);

    Donacion donacionPendiente = new Donacion(fideos, 50, "Kg", null, LocalDate.now());
    repoDonaciones.guardar(donacionPendiente);

    NecesidadExtraordinaria n = new NecesidadExtraordinaria(fideos, "", 2, 1);
    EntidadBeneficiaria comedor = new EntidadBeneficiaria("Comedor", "Dir", "123", new ArrayList<>(), new ArrayList<>(List.of(n)));
    repoEntidades.guardar(comedor);

    TareaNocturnaMatchmaking tarea = new TareaNocturnaMatchmaking(
        repoDonaciones, repoEntidades, repoPropuestas, procesador
    );

    tarea.ejecutar();

    List<PropuestaMatchmaking> propuestasGeneradas = repoPropuestas.obtenerTodas();

    assertFalse(propuestasGeneradas.isEmpty(), "La tarea debería haber generado al menos una propuesta");

    assertEquals(1, propuestasGeneradas.size(), "Debería haber exactamente 1 propuesta en la base para la donación procesada");
    assertEquals(fideos, propuestasGeneradas.get(0).getDonacion().getSubcategoria());
  }
}