package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.Usuario;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
  private final EntityManager entityManager;

  public UsuarioRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public UsuarioRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public Usuario guardar(Usuario usuario) {
    enTransaccion(() -> {
      if (usuario.getId() == null) {
        entityManager.persist(usuario);
      } else {
        entityManager.merge(usuario);
      }
    });
    return usuario;
  }

  public Optional<Usuario> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(Usuario.class, id));
  }

  public Optional<Usuario> buscarPorEmail(String email) {
    if (email == null) {
      return Optional.empty();
    }
    List<Usuario> encontrados = entityManager
        .createQuery("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)", Usuario.class)
        .setParameter("email", email.trim())
        .getResultList();
    return encontrados.stream().findFirst();
  }

  public List<Usuario> obtenerTodos() {
    return entityManager.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
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
