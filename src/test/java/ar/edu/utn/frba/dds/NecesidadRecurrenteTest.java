package ar.edu.utn.frba.dds;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.frba.dds.exceptions.DescripcionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NecesidadRecurrenteTest {

  private Subcategoria fideos;
  private PeriodoConsumo periodoConsumo;

  @BeforeEach
  void setUp() {
    fideos = new Subcategoria("Fideos", Categoria.ALIMENTOS, false, true);
    periodoConsumo = new PeriodoConsumo(10, "Mayo", 4);
  }

  @Test
  void seSatisfaceCuandoAlcanzaElObjetivoDelPeriodo() {
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        fideos, "Fideos mensuales", periodoConsumo);

    necesidad.registrarRecepcion(6);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void reiniciarPeriodoVuelveLaCantidadRecibidaACero() {
    PeriodoConsumo periodo = new PeriodoConsumo(10, "Mayo", 10);
    NecesidadRecurrente necesidad = new NecesidadRecurrente(fideos, "Fideos mensuales", periodo);

    necesidad.reiniciarPeriodo();

    assertFalse(necesidad.estaSatisfecha());

    assertEquals(0, necesidad.getPeriodoConsumo().getCantidadRecibida());
  }

  @Test
  void noPermitePeriodoVacio() {

    assertThrows(DescripcionException.class, () ->
        new PeriodoConsumo(10, " ", 0)
    );
  }
}
