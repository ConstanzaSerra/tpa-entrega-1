package ar.edu.utn.frba.dds;

import ar.edu.utn.frba.dds.bienes.Categoria;
import ar.edu.utn.frba.dds.bienes.Subcategoria;
import ar.edu.utn.frba.dds.necesidades.NecesidadRecurrente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NecesidadRecurrenteTest {

  private Subcategoria fideos;

  @BeforeEach
  void setUp() {
    fideos = new Subcategoria("Fideos", Categoria.ALIMENTOS);
  }

  @Test
  void seSatisfaceCuandoAlcanzaElObjetivoDelPeriodo() {
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        fideos, "Fideos mensuales", 10, "Mayo", 4
    );

    necesidad.registrarRecepcion(6);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void reiniciarPeriodoVuelveLaCantidadRecibidaACero() {
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        fideos, "Fideos mensuales", 10, "Mayo", 10
    );

    necesidad.reiniciarPeriodo();

    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void noPermitePeriodoVacio() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadRecurrente(
        fideos, "Fideos mensuales", 10, " ", 0
    ));
  }

  @Test
  void noPermiteCantidadObjetivoCero() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadRecurrente(
        fideos, "Fideos mensuales", 0, "Mayo", 0
    ));
  }

  @Test
  void noPermiteCantidadObjetivoNegativa() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadRecurrente(
        fideos, "Fideos mensuales", -5, "Mayo", 0
    ));
  }

  @Test
  void noPermiteCantidadRecibidaInicialNegativa() {
    assertThrows(IllegalArgumentException.class, () -> new NecesidadRecurrente(
        fideos, "Fideos mensuales", 10, "Mayo", -1
    ));
  }

  @Test
  void noPermiteRegistrarRecepcionNegativa() {
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        fideos, "Fideos mensuales", 10, "Mayo", 0
    );

    assertThrows(IllegalArgumentException.class, () -> necesidad.registrarRecepcion(-1));
  }
}
