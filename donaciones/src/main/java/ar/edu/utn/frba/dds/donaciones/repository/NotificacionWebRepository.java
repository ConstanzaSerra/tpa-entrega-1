package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.NotificacionWeb;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;

public class NotificacionWebRepository {
  private final EntityManager entityManager;

  public NotificacionWebRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public NotificacionWebRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public NotificacionWeb guardar(NotificacionWeb notificacion) {
    enTransaccion(() -> {
      if (notificacion.getId() == null) {
        entityManager.persist(notificacion);
      } else {
        entityManager.merge(notificacion);
      }
    });
    return notificacion;
  }

  public List<NotificacionWeb> buscarPorEntidad(Long entidadId) {
    return entityManager
        .createQuery(
            "SELECT n FROM NotificacionWeb n WHERE n.entidadId = :entidadId ORDER BY n.fechaHora DESC",
            NotificacionWeb.class)
        .setParameter("entidadId", entidadId)
        .getResultList();
  }

  public List<NotificacionWeb> buscarPorDonante(Long donanteId) {
    return entityManager
        .createQuery(
            "SELECT n FROM NotificacionWeb n WHERE n.donanteId = :donanteId ORDER BY n.fechaHora DESC",
            NotificacionWeb.class)
        .setParameter("donanteId", donanteId)
        .getResultList();
  }

  public List<NotificacionWeb> obtenerTodas() {
    return entityManager
        .createQuery("SELECT n FROM NotificacionWeb n ORDER BY n.fechaHora DESC", NotificacionWeb.class)
        .getResultList();
  }

  private void enTransaccion(Runnable accion) {
    if (entityManager.getTransaction().isActive()) {
      accion.run();
      return;
    }
    entityManager.getTransaction().begin();
    try {
      accion.run();
      entityManager.getTransaction().commit();
    } catch (RuntimeException e) {
      entityManager.getTransaction().rollback();
      throw e;
    }
  }
}
