package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlanificacionScheduler {
    private final ScheduledExecutorService scheduler;
    private final PlanificadorRutasService planificadorService;

    public PlanificacionScheduler(PlanificadorRutasService planificadorService) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.planificadorService = planificadorService;
    }

    // Modo demo/frecuencia: corre cada N minutos en vez de calendarizado.
    public void iniciar(long initialDelay, long period, TimeUnit unit) {
        System.out.println("Iniciando scheduler de planificacion. Frecuencia: " + period + " " + unit);
        agendar(unit.toSeconds(initialDelay), unit.toSeconds(period));
    }

    public void iniciarDiario(LocalTime horaEjecucion) {
        long delayInicialSegundos = segundosHastaProximaEjecucion(LocalDateTime.now(), horaEjecucion);
        System.out.println("Iniciando scheduler de planificacion diaria a las " + horaEjecucion
                + " (proxima ejecucion en " + delayInicialSegundos / 60 + " minutos).");
        agendar(delayInicialSegundos, Duration.ofDays(1).toSeconds());
    }

    // si la hora de hoy ya paso, la proxima ejecucion es mañana.
    public static long segundosHastaProximaEjecucion(LocalDateTime ahora, LocalTime horaEjecucion) {
        LocalDateTime proxima = ahora.toLocalDate().atTime(horaEjecucion);
        if (!proxima.isAfter(ahora)) {
            proxima = proxima.plusDays(1);
        }
        return Duration.between(ahora, proxima).toSeconds();
    }

    private void agendar(long initialDelaySegundos, long periodSegundos) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                planificadorService.planificar();
            } catch (Exception e) {
                System.err.println("Error ejecutando planificacion programada: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelaySegundos, periodSegundos, TimeUnit.SECONDS);
    }

    public void detener() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
