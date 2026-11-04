package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.Bien;
import ar.edu.utn.frba.dds.donaciones.domain.CargaDeDonacion;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;

public class CargaDonacionRepository {
  private final EntityManager entityManager;

  public CargaDonacionRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public CargaDonacionRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public void guardar(CargaDeDonacion carga) {
    enTransaccion(() -> {
      if (carga.getBienes() != null) {
        for (Bien b : carga.getBienes()) {
          if (b.getSubcategoria() != null && b.getSubcategoria().getId() == null) {
            entityManager.persist(b.getSubcategoria());
          }
        }
      }
      if (carga.getId() == null) {
        entityManager.persist(carga);
      } else {
        entityManager.merge(carga);
      }
    });
  }

  public List<CargaDeDonacion> obtenerTodas() {
    return entityManager
        .createQuery("SELECT c FROM CargaDeDonacion c ORDER BY c.id DESC", CargaDeDonacion.class)
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
