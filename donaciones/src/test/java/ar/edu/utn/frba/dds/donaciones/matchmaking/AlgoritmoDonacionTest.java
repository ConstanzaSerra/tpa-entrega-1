package ar.edu.utn.frba.dds.donaciones.matchmaking;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;
import ar.edu.utn.frba.dds.donaciones.service.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AlgoritmoDonacionTest {

  private PeriodoConsumo periodoConsumo;
  private List<EntidadBeneficiaria> entidades;
  private List<Donacion> historialDonaciones;

  private EntidadBeneficiaria rabanitoFeliz;
  private EntidadBeneficiaria caritas;
  private EntidadBeneficiaria hogarDulceHogar;

  private Subcategoria comidaEnlatada;
  private Subcategoria ropaAbrigo;

  private Necesidad necesidadComida;
  private Necesidad necesidadRopa;


  @BeforeEach
  public void setUp() {

    periodoConsumo = new PeriodoConsumo(10, "Mayo", 4);

    comidaEnlatada = new Subcategoria("Comida Enlatada", Categoria.VESTIMENTA, false,true);
    ropaAbrigo = new Subcategoria("Ropa de Abrigo", Categoria.VESTIMENTA, false, false);

    necesidadComida = new NecesidadRecurrente(comidaEnlatada, "Comida Enlatada", periodoConsumo);
    necesidadRopa = new NecesidadExtraordinaria(ropaAbrigo, "Ropa de Abrigo", 10, 0);

    rabanitoFeliz = new EntidadBeneficiaria("Comedor Rabanito Feliz", "Calle 1", "123", new ArrayList<>(), new ArrayList<>(List.of(necesidadComida)));
    caritas = new EntidadBeneficiaria("Cáritas", "Calle 2", "456", new ArrayList<>(), new ArrayList<>(List.of(necesidadComida, necesidadRopa)));
    hogarDulceHogar = new EntidadBeneficiaria("Hogar Dulce Hogar", "Calle 3", "789", new ArrayList<>(), new ArrayList<>(List.of(necesidadRopa)));

    entidades = List.of(rabanitoFeliz, caritas, hogarDulceHogar);
    historialDonaciones = new ArrayList<>();
  }

  @Test
  public void testCompatibilidadSemantica_DebeSugerirSoloEntidadesQueNecesitanElBien() {
    CompatibilidadSemantica algoritmo = new CompatibilidadSemantica();
    Donacion donacionArvejas = new Donacion(comidaEnlatada, 100, "U", null, LocalDate.now());

    List<EntidadBeneficiaria> sugeridas = algoritmo.calcularSugerencias(donacionArvejas, entidades, historialDonaciones);

    assertEquals(2, sugeridas.size());
    assertTrue(sugeridas.contains(rabanitoFeliz));
    assertTrue(sugeridas.contains(caritas));
    assertFalse(sugeridas.contains(hogarDulceHogar));
  }

  @Test
  public void testPrioridadSubAtendidos_DebeOrdenarSugeridasPoniendoPrimeroALasQueRecibieronMenos() {
    PrioridadSubatendidos algoritmo = new PrioridadSubatendidos();
    Donacion donacionGenerica = new Donacion(comidaEnlatada, 10, "U", null, LocalDate.now());

    Donacion d1 = new Donacion(comidaEnlatada, 5, "U", null, LocalDate.now());
    d1.asignarEntidad(caritas);

    Donacion d2 = new Donacion(comidaEnlatada, 5, "U", null, LocalDate.now());
    d2.asignarEntidad(caritas);

    Donacion d3 = new Donacion(comidaEnlatada, 5, "U", null, LocalDate.now());
    d3.asignarEntidad(rabanitoFeliz);

    historialDonaciones.addAll(List.of(d1, d2, d3));

    List<EntidadBeneficiaria> sugeridas = algoritmo.calcularSugerencias(donacionGenerica, entidades, historialDonaciones);

    assertEquals(3, sugeridas.size());
    assertEquals(hogarDulceHogar, sugeridas.get(0));
    assertEquals(rabanitoFeliz, sugeridas.get(1));
    assertEquals(caritas, sugeridas.get(2));
  }
}
