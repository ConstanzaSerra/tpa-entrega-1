package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.Notificacion;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class TareaDeInactividad {
  private final DonanteRepository donanteRepository;
  private final GestorDeNotificaciones gestorNotificaciones;
  private Long diasLimite;

  public TareaDeInactividad(DonanteRepository donanteRepository, GestorDeNotificaciones gestorNotificaciones) {
    this.donanteRepository = donanteRepository;
    this.gestorNotificaciones = gestorNotificaciones;
  }

  public void ejecutar() {

    diasLimite = 20L;

    List<PersonaDonante> donantesInactivos = donanteRepository.obtenerTodos();

    donantesInactivos = this.obtenerInactivos(diasLimite);

    // 3. Enviamos la notificación a cada uno
    for (PersonaDonante donante : donantesInactivos) {
      Notificacion notificacion = new Notificacion(donante,
          "Te extrañamos en DonaTrack. Notamos que hace 20 días no interactúas con la plataforma. ¡Tus donaciones siguen haciendo la diferencia!",
          donante.medioDePreferencia()
      );
      gestorNotificaciones.publicar(new EventoDeNotificacion(List.of(donante), notificacion.toString()));
    }
  }

  private List<PersonaDonante> obtenerInactivos(Long diasLimite) {

    List<PersonaDonante> donantes = donanteRepository.obtenerTodos();
    List<PersonaDonante> inactivos = new ArrayList<>();

    for (PersonaDonante donante : donantes) {

      long dias = ChronoUnit.DAYS.between(donante.getUltimaInteraccion(), LocalDate.now());
      if (dias > diasLimite) {
        inactivos.add(donante);
      }

    }
    return inactivos;
  }
}