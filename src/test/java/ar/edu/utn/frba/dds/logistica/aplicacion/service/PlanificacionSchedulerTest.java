package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanificacionSchedulerTest {

    // Si la hora de baja carga todavia no paso, la proxima ejecucion es hoy
    @Test
    void testHoraFutura_EjecutaHoy() {
        LocalDateTime ahora = LocalDateTime.of(2026, 7, 2, 0, 0);
        long segundos = PlanificacionScheduler.segundosHastaProximaEjecucion(ahora, LocalTime.of(2, 0));
        assertEquals(2 * 3600, segundos);
    }

    // Si la hora ya paso, la proxima ejecucion es maniana a esa hora
    @Test
    void testHoraPasada_EjecutaManiana() {
        LocalDateTime ahora = LocalDateTime.of(2026, 7, 2, 10, 0);
        long segundos = PlanificacionScheduler.segundosHastaProximaEjecucion(ahora, LocalTime.of(2, 0));
        assertEquals(16 * 3600, segundos);
    }

    // Justo a la hora de ejecucion, la proxima es en 24 horas (no en 0 segundos)
    @Test
    void testHoraExacta_EjecutaEn24Horas() {
        LocalDateTime ahora = LocalDateTime.of(2026, 7, 2, 2, 0);
        long segundos = PlanificacionScheduler.segundosHastaProximaEjecucion(ahora, LocalTime.of(2, 0));
        assertEquals(24 * 3600, segundos);
    }
}
