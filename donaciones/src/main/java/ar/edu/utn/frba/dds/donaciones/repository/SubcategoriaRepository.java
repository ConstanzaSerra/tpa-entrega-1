package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.Categoria;
import ar.edu.utn.frba.dds.donaciones.domain.Subcategoria;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Faltaba: Subcategoria es una entidad compartida (donaciones, bienes y
 * necesidades la referencian) y por lo tanto necesita guardarse por si misma.
 * No se cascadea su persistencia desde Donacion a proposito: si cada donacion
 * cascadeara el persist, la misma subcategoria se duplicaria una vez por
 * donacion, que es justo lo que el modelo relacional viene a evitar.
 */
public class SubcategoriaRepository {
  private final EntityManager entityManager;

  public SubcategoriaRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public SubcategoriaRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public void guardar(Subcategoria subcategoria) {
    enTransaccion(() -> entityManager.persist(subcategoria));
  }

  public Optional<Subcategoria> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(Subcategoria.class, id));
  }

  public Optional<Subcategoria> buscarPorNombre(String nombre) {
    return entityManager
        .createQuery("SELECT s FROM Subcategoria s WHERE s.nombre = :nombre", Subcategoria.class)
        .setParameter("nombre", nombre)
        .getResultList()
        .stream()
        .findFirst();
  }

  public List<Subcategoria> buscarPorCategoria(Categoria categoria) {
    return entityManager
        .createQuery("SELECT s FROM Subcategoria s WHERE s.categoria = :categoria", Subcategoria.class)
        .setParameter("categoria", categoria)
        .getResultList();
  }

  public List<Subcategoria> obtenerTodas() {
    return entityManager
        .createQuery("SELECT s FROM Subcategoria s", Subcategoria.class)
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
