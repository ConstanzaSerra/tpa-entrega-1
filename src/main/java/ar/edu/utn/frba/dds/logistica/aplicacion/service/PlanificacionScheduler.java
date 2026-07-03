package ar.edu.utn.frba.dds.logistica.aplicacion.service;

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

    public void iniciar(long initialDelay, long period, TimeUnit unit) {
        System.out.println("Iniciando scheduler de planificacion. Frecuencia: " + period + " " + unit);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                planificadorService.planificar();
            } catch (Exception e) {
                System.err.println("Error ejecutando planificacion programada: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelay, period, unit);
    }

    public void detener() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
