package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.Categoria;
import ar.edu.utn.frba.dds.donaciones.domain.Subcategoria;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Subcategoria es una entidad compartida (donaciones, bienes y
 * necesidades la referencian) y por lo tanto necesita guardarse por si misma.
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
    enTransaccion(() -> {
      if (subcategoria.getId() == null) {
        entityManager.persist(subcategoria);
      } else {
        entityManager.merge(subcategoria);
      }
    });
  }

  public Subcategoria obtenerOCrear(String nombre, Categoria categoria, boolean requiereEstado, boolean esPerecedero) {
    return buscarPorNombre(nombre).orElseGet(() -> {
      Subcategoria nueva = new Subcategoria(nombre, categoria, requiereEstado, esPerecedero);
      guardar(nueva);
      return nueva;
    });
  }

  public Optional<Subcategoria> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(Subcategoria.class, id));
  }

  public Optional<Subcategoria> buscarPorNombre(String nombre) {
    return entityManager
        .createQuery("SELECT s FROM Subcategoria s WHERE LOWER(s.nombre) = LOWER(:nombre)", Subcategoria.class)
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
