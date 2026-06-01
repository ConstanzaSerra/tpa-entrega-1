package ar.edu.utn.frba.dds;

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

  @Test
  void noPermiteCantidadRequeridaCero() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", 0, 0
    ));
  }

  @Test
  void noPermiteCantidadRequeridaNegativa() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", -10, 0
    ));
  }

  @Test
  void noPermiteCantidadRecibidaInicialNegativa() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadExtraordinaria(
        medicamentos, "Antibioticos", 10, -1
    ));
  }
}
