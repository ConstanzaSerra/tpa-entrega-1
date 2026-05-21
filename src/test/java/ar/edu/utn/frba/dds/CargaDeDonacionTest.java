package ar.edu.utn.frba.dds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CargaDeDonacionTest {

  private PersonaDonante donante;
  private Subcategoria sillas;
  private Subcategoria fideos;

  @BeforeEach
  void setUp() {
    Email email = new Email("donante@mail.com");
    donante = new PersonaHumana(
        new ArrayList<>(List.of(email)), email, "Juann", "Perez", 30, 123456, "F", "Av. Siempreviva 742"
    );

    Categoria muebles   = Categoria.MOBILIARIO;
    Categoria alimentos = Categoria.ALIMENTOS;

    sillas = new Subcategoria("Sillas", muebles,   true,  false);
    fideos = new Subcategoria("Fideos", alimentos, false, true);
  }

  @Test
  void segmentarConUnSoloBienGeneraUnaDonacion() {
    Bien silla = new BienSimple("Silla de oficina", null, sillas, 6, "unidades");
    CargaDeDonacion carga = new CargaDeDonacion("Donación muebles", LocalDate.now(), donante,
        new ArrayList<>(List.of(silla)));

    List<Donacion> resultado = carga.segmentar();

    assertEquals(1, resultado.size());
    assertEquals(sillas, resultado.get(0).getSubcategoria());
    assertEquals(6, resultado.get(0).getCantidad());
  }

  @Test
  void segmentarAgrupaCorrectamentePorSubcategoria() {
    Bien silla1 = new BienSimple("Silla A", null, sillas, 6, "unidades");
    Bien silla2 = new BienSimple("Silla B", null, sillas, 4, "unidades");
    Bien mesa   = new BienSimple("Mesa",    null, fideos, 1, "unidades");

    CargaDeDonacion carga = new CargaDeDonacion("Donación mixta", LocalDate.now(), donante,
        new ArrayList<>(List.of(silla1, silla2, mesa)));

    List<Donacion> resultado = carga.segmentar();

    assertEquals(2, resultado.size());

    Donacion donacionSillas = resultado.stream()
        .filter(d -> d.getSubcategoria().equals(sillas))
        .findFirst().orElseThrow();
    assertEquals(10, donacionSillas.getCantidad());
  }

  @Test
  void segmentarSeparaPereciblesPorFechaDeVencimiento() {
    LocalDate fecha1 = LocalDate.of(2027, 1, 1);
    LocalDate fecha2 = LocalDate.of(2027, 6, 1);

    Bien fideos1 = new BienPercibible("Fideos lote A", null, fideos, 100, "paquetes", fecha1);
    Bien fideos2 = new BienPercibible("Fideos lote B", null, fideos, 50,  "paquetes", fecha2);

    CargaDeDonacion carga = new CargaDeDonacion("Donación pastas", LocalDate.now(), donante,
        new ArrayList<>(List.of(fideos1, fideos2)));

    List<Donacion> resultado = carga.segmentar();

    assertEquals(2, resultado.size());
  }

  @Test
  void segmentarAgrupaPereciblesConMismaFechaYSubcategoria() {
    LocalDate fecha = LocalDate.of(2027, 1, 1);

    Bien fideos1 = new BienPercibible("Fideos lote A", null, fideos, 100, "paquetes", fecha);
    Bien fideos2 = new BienPercibible("Fideos lote B", null, fideos, 50,  "paquetes", fecha);

    CargaDeDonacion carga = new CargaDeDonacion("Donación pastas", LocalDate.now(), donante,
        new ArrayList<>(List.of(fideos1, fideos2)));

    List<Donacion> resultado = carga.segmentar();

    assertEquals(1, resultado.size());
    assertEquals(150, resultado.get(0).getCantidad());
  }

  @Test
  void segmentarConListaVaciaNoGeneraDonaciones() {
    CargaDeDonacion carga = new CargaDeDonacion("Carga vacía", LocalDate.now(), donante,
        new ArrayList<>());

    List<Donacion> resultado = carga.segmentar();

    assertTrue(resultado.isEmpty());
  }
}