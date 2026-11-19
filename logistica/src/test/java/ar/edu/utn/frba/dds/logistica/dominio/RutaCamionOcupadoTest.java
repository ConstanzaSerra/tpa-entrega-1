package ar.edu.utn.frba.dds.logistica.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Un camion tiene muchas rutas a lo largo del tiempo, pero solo puede recorrer una a la vez
class RutaCamionOcupadoTest {

    private Camion camion;
    private Camion otroCamion;

    @BeforeEach
    void setUp() {
        camion = new Camion("AE-412-UT", 24, 3.2, 5000);
        camion.setId(1L);
        otroCamion = new Camion("AF-890-SO", 18, 2.8, 3500);
        otroCamion.setId(2L);
    }

    @Test
    void noSePuedeIniciarUnaRutaSiElCamionYaTieneOtraEnCurso() {
        Ruta enCurso = new Ruta(camion);
        enCurso.iniciar();
        Ruta nueva = new Ruta(camion);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> nueva.iniciar(List.of(enCurso, nueva)));
        assertEquals(EstadoRuta.PLANIFICADA, nueva.getEstado());
        assertEquals("El camion AE-412-UT ya tiene otra ruta en curso", error.getMessage());
    }

    @Test
    void elMismoCamionPuedeTenerVariasRutasPlanificadasEnCola() {
        Ruta planificada1 = new Ruta(camion);
        Ruta planificada2 = new Ruta(camion);

        assertDoesNotThrow(() -> planificada1.iniciar(List.of(planificada1, planificada2)));
        assertEquals(EstadoRuta.EN_CURSO, planificada1.getEstado());
    }

    @Test
    void unaRutaCompletadaNoOcupaAlCamion() {
        Ruta anterior = new Ruta(camion);
        anterior.iniciar();
        anterior.completar();
        Ruta nueva = new Ruta(camion);

        assertDoesNotThrow(() -> nueva.iniciar(List.of(anterior, nueva)));
        assertEquals(EstadoRuta.EN_CURSO, nueva.getEstado());
    }

    @Test
    void unaRutaEnCursoDeOtroCamionNoBloquea() {
        Ruta deOtroCamion = new Ruta(otroCamion);
        deOtroCamion.iniciar();
        Ruta nueva = new Ruta(camion);

        assertDoesNotThrow(() -> nueva.iniciar(List.of(deOtroCamion, nueva)));
    }

    @Test
    void elMismoCamionLeidoDeLaBaseComoOtraInstanciaSeReconocePorId() {
        Camion mismoCamionOtraInstancia = new Camion("AE-412-UT", 24, 3.2, 5000);
        mismoCamionOtraInstancia.setId(1L);
        Ruta enCurso = new Ruta(mismoCamionOtraInstancia);
        enCurso.iniciar();
        Ruta nueva = new Ruta(camion);

        assertThrows(IllegalStateException.class, () -> nueva.iniciar(List.of(enCurso)));
    }
}
