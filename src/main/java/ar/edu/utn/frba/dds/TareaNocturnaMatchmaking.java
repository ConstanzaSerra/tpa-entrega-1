package ar.edu.utn.frba.dds;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TareaNocturnaMatchmaking {
  private DonacionRepository repoDonaciones;
  private EntidadRepository repoEntidades;
  private PropuestaRepository repoPropuestas;
  private ProcesadorMatchmaking procesador;

  private ScheduledThreadPoolExecutor scheduler;

  public TareaNocturnaMatchmaking(DonacionRepository repoDonaciones,
                                  EntidadRepository repoEntidades,
                                  PropuestaRepository repoPropuestas,
                                  ProcesadorMatchmaking procesador) {
    this.repoDonaciones = repoDonaciones;
    this.repoEntidades = repoEntidades;
    this.repoPropuestas = repoPropuestas;
    this.procesador = procesador;
    this.scheduler = new ScheduledThreadPoolExecutor(1); // 1 hilo de ejecución
  }

  public void iniciarShedulerProduccion() {
    // Ejecuta el método ejecutar. Arranca ya mismo (0), y repite cada 1 día
    scheduler.scheduleAtFixedRate(this::ejecutar, 0, 1, TimeUnit.DAYS);
  }

  public void iniciarSchedulerParaTest(long segundos) {
    scheduler.scheduleAtFixedRate(this::ejecutar, 0, segundos, TimeUnit.SECONDS);
  }

  public void detener() {
    scheduler.shutdown();
  }

  public void ejecutar() {
    List<Donacion> pendientes = repoDonaciones.obtenerDonacionesEnDeposito();
    List<EntidadBeneficiaria> todasLasEntidades = repoEntidades.obtenerTodas();
    List<Donacion> historial = repoDonaciones.obtenerTodas();

    for (Donacion donacion : pendientes) {
      try {
        PropuestaMatchmaking fichaResultante = procesador.procesar(donacion, todasLasEntidades, historial);
        repoPropuestas.guardar(fichaResultante);
      } catch (Exception e) {
        System.err.println("Error en donación: " + e.getMessage());
      }
    }
  }
}