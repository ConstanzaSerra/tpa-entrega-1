package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Tarea calendarizada que detecta donantes inactivos (sin interaccion por mas de N dias)
 * y publica un evento de notificacion para incentivar una nueva donacion.
 * Reusa el GestorDeNotificaciones (Observer): solo publica, no sabe como se envia.
 */
public class TareaDeInactividad {
  private static final int DIAS_LIMITE_DEFAULT = 20;

  private final DonanteRepository donanteRepository;
  private final GestorDeNotificaciones gestor;
  private final int diasLimite;
  private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

  public TareaDeInactividad(DonanteRepository donanteRepository, GestorDeNotificaciones gestor) {
    this(donanteRepository, gestor, DIAS_LIMITE_DEFAULT);
  }

  public TareaDeInactividad(DonanteRepository donanteRepository, GestorDeNotificaciones gestor, int diasLimite) {
    this.donanteRepository = donanteRepository;
    this.gestor = gestor;
    this.diasLimite = diasLimite;
  }

  // Corre en horario de baja carga (una vez por dia).
  public void iniciarDiario() {
    scheduler.scheduleAtFixedRate(this::ejecutar, 0, 1, TimeUnit.DAYS);
  }

  public void detener() {
    scheduler.shutdown();
  }

  public void ejecutar() {
    LocalDate hoy = LocalDate.now();
    List<PersonaDonante> donantes = donanteRepository.obtenerTodos();
    for (PersonaDonante donante : donantes) {
      if (donante.getUltimaInteraccion() == null) {
        continue;
      }
      long dias = ChronoUnit.DAYS.between(donante.getUltimaInteraccion(), hoy);
      if (dias > diasLimite) {
        gestor.publicar(new EventoDeNotificacion(
            List.of(donante),
            "Hace " + dias + " días que no interactuás con DonaTrack. ¡Te esperamos para una nueva donación!"));
      }
    }
  }
}
