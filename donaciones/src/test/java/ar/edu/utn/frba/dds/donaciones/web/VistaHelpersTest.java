package ar.edu.utn.frba.dds.donaciones.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VistaHelpersTest {

  @Test
  void losEstadosSeMuestranConNombreLegible() {
    assertEquals("Asignada", VistaHelpers.estadoTexto("ASIGNACION_REALIZADA"));
    assertEquals("En camino", VistaHelpers.estadoTexto("EN_TRASLADO"));
    assertEquals("En curso", VistaHelpers.estadoTexto("EN_CURSO"));
    assertEquals("", VistaHelpers.estadoTexto(null));
  }

  @Test
  void unEstadoDesconocidoNoSeRompeYSeMuestraLegible() {
    assertEquals("Otro estado nuevo", VistaHelpers.estadoTexto("OTRO_ESTADO_NUEVO"));
    assertEquals("estado-otro", VistaHelpers.estadoClase("OTRO_ESTADO_NUEVO"));
  }

  @Test
  void cadaEstadoTieneSuColor() {
    assertEquals("estado-entregada", VistaHelpers.estadoClase("ENTREGADA"));
    assertEquals("estado-fallida", VistaHelpers.estadoClase("ENTREGA_FALLIDA"));
    assertEquals("estado-vencida", VistaHelpers.estadoClase("VENCIDA"));
  }

  @Test
  void lasCategoriasEnMayusculasSePasanATextoNormal() {
    assertEquals("Alimentos", VistaHelpers.legible("ALIMENTOS"));
    assertEquals("Emergencia sanitaria", VistaHelpers.legible("EMERGENCIA_SANITARIA"));
    assertEquals("Ya legible", VistaHelpers.legible("Ya legible"));
  }

  @Test
  void lasFechasIsoSeMuestranEnFormatoArgentino() {
    assertEquals("17/11/2026", VistaHelpers.fecha("2026-11-17"));
    assertEquals("17/11/2026 11:02", VistaHelpers.fecha("2026-11-17T11:02:37.418"));
    assertEquals("Reciente", VistaHelpers.fecha("Reciente"));
    assertEquals("", VistaHelpers.fecha(null));
  }

  @Test
  void losNumerosLlevanSeparadorDeMilesYSinDecimalesDeMas() {
    assertEquals("10.512", VistaHelpers.numero(10512));
    assertEquals("42", VistaHelpers.numero(42.0));
    assertEquals("3,3", VistaHelpers.numero(3.25));
    assertEquals("sin dato", VistaHelpers.numero("sin dato"));
  }
}
