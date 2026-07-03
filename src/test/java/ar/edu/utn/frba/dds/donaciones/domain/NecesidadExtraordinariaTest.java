package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;
import ar.edu.utn.frba.dds.donaciones.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NecesidadExtraordinariaTest {

  private Subcategoria medicamentos;

  @BeforeEach
  void setUp() {
    medicamentos = new Subcategoria("Medicamentos", Categoria.ALIMENTOS, false, false);
  }

  @Test
  void seSatisfaceCuandoAlcanzaLaCantidadRequerida() {
    NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", 10, 4
    );

    necesidad.registrarRecepcion(6);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void noEstaSatisfechaSiNoAlcanzaLaCantidadRequerida() {
    NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", 10, 4
    );

    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void permiteCrearUnaNecesidadYaSatisfecha() {
    NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", 10, 10
    );

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void noPermiteCantidadesNegativas() {
    NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", 10, 0
    );

    assertThrows(IllegalArgumentException.class, () -> necesidad.registrarRecepcion(-1));
  }
}
