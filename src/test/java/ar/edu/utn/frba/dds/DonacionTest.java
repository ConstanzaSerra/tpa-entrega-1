package ar.edu.utn.frba.dds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class DonacionTest {

  private Donacion donacion;
  private EntidadBeneficiaria entidad;

  @BeforeEach
  void setUp() {
    Subcategoria sillas = new Subcategoria("Sillas", Categoria.MOBILIARIO, true, false);
    PersonaDonante donante = mock(PersonaDonante.class);
    entidad = mock(EntidadBeneficiaria.class);
    donacion = new Donacion(sillas, 5, "unidades", donante, LocalDate.now());
  }

  @Test
  void avanzarEstadoDesdeEnDepositoLanzaExcepcion() {
    assertThrows(IllegalStateException.class, () -> donacion.avanzarEstado());
  }

  @Test
  void avanzarEstadoDesdeEntregadaLanzaExcepcion() {
    donacion.asignarEntidad(entidad);
    donacion.avanzarEstado(); // LISTA_PARA_ENTREGAR
    donacion.avanzarEstado(); // EN_TRASLADO
    donacion.avanzarEstado(); // ENTREGADA

    assertThrows(IllegalStateException.class, () -> donacion.avanzarEstado());
  }

  @Test
  void avanzarEstadoDesdeVencidaLanzaExcepcion() {
    donacion.marcarVencida();

    assertThrows(IllegalStateException.class, () -> donacion.avanzarEstado());
  }

  @Test
  void asignarEntidadCuandoNoEstaEnDepositoLanzaExcepcion() {
    donacion.asignarEntidad(entidad);

    assertThrows(IllegalStateException.class, () -> donacion.asignarEntidad(entidad));
  }

  @Test
  void marcarEntregaFallidaCuandoNoEstaEnTrasladoLanzaExcepcion() {
    assertThrows(IllegalStateException.class, () -> donacion.marcarEntregaFallida("Nadie en casa"));
  }

  @Test
  void marcarVencidaCuandoYaEstaEntregadaLanzaExcepcion() {
    donacion.asignarEntidad(entidad);
    donacion.avanzarEstado(); // LISTA_PARA_ENTREGAR
    donacion.avanzarEstado(); // EN_TRASLADO
    donacion.avanzarEstado(); // ENTREGADA

    assertThrows(IllegalStateException.class, () -> donacion.marcarVencida());
  }

  @Test
  void marcarVencidaCuandoYaEstaVencidaLanzaExcepcion() {
    donacion.marcarVencida();

    assertThrows(IllegalStateException.class, () -> donacion.marcarVencida());
  }
}