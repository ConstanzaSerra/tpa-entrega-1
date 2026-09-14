package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class EntidadRepository {
  private final EntityManager entityManager;

  public EntidadRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public EntidadRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public void guardar(EntidadBeneficiaria entidad) {
    enTransaccion(() -> entityManager.persist(entidad));
  }

  public Optional<EntidadBeneficiaria> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(EntidadBeneficiaria.class, id));
  }

  public List<EntidadBeneficiaria> obtenerTodas() {
    return entityManager
        .createQuery("SELECT e FROM EntidadBeneficiaria e", EntidadBeneficiaria.class)
        .getResultList();
  }

  public boolean eliminar(Long id) {
    EntidadBeneficiaria entidad = entityManager.find(EntidadBeneficiaria.class, id);
    if (entidad == null) {
      return false;
    }
    enTransaccion(() -> entityManager.remove(entidad));
    return true;
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
