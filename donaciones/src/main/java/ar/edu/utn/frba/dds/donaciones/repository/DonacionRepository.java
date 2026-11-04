package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Misma interfaz publica que la version en memoria; lo unico que cambio es de
 * donde salen los datos. El EntityManager se inyecta por constructor para que
 * los tests puedan pasarle el de jpa-extras (HSQLDB) y la app el de Postgres.
 */
public class DonacionRepository {
  private final EntityManager entityManager;

  public DonacionRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public DonacionRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public void guardar(Donacion donacion) {
    enTransaccion(() -> {
      if (donacion.getSubcategoria() != null && donacion.getSubcategoria().getId() == null) {
        entityManager.persist(donacion.getSubcategoria());
      }
      if (donacion.getId() == null) {
        entityManager.persist(donacion);
      } else {
        entityManager.merge(donacion);
      }
    });
  }

  public List<Donacion> buscarPorEstado(EstadoDonacion estado) {
    return entityManager
        .createQuery("SELECT d FROM Donacion d WHERE d.estado = :estado", Donacion.class)
        .setParameter("estado", estado)
        .getResultList();
  }

  public List<Donacion> buscarPorDonante(Long donanteId) {
    return entityManager
        .createQuery("SELECT d FROM Donacion d WHERE d.donante.id = :donanteId ORDER BY d.id DESC", Donacion.class)
        .setParameter("donanteId", donanteId)
        .getResultList();
  }

  public List<Donacion> buscarPorEntidadAsignada(Long entidadId) {
    return entityManager
        .createQuery("SELECT d FROM Donacion d WHERE d.entidadAsignada.id = :entidadId ORDER BY d.id DESC", Donacion.class)
        .setParameter("entidadId", entidadId)
        .getResultList();
  }

  public Optional<Donacion> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(Donacion.class, id));
  }

  public List<Donacion> obtenerDonacionesEnDeposito() {
    return buscarPorEstado(EstadoDonacion.EN_DEPOSITO);
  }

  public List<Donacion> obtenerTodas() {
    return entityManager
        .createQuery("SELECT d FROM Donacion d", Donacion.class)
        .getResultList();
  }

  public boolean eliminar(Long id) {
    Donacion donacion = entityManager.find(Donacion.class, id);
    if (donacion == null) {
      return false;
    }
    enTransaccion(() -> entityManager.remove(donacion));
    return true;
  }

  // Si ya hay una transaccion abierta (caso tipico en los tests de jpa-extras,
  // que corren todo dentro de una), no se abre otra.
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
