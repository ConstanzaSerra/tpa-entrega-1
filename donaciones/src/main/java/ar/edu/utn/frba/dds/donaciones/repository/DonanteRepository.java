package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Se conserva getInstancia() para no romper a los controllers que ya lo usan,
 * pero ahora tambien hay constructor publico con EntityManager inyectado, que
 * es lo que usan los tests.
 */
public class DonanteRepository {

  private static DonanteRepository instancia;
  private final EntityManager entityManager;

  public DonanteRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public static synchronized DonanteRepository getInstancia() {
    if (instancia == null) {
      instancia = new DonanteRepository(EntityManagerProvider.getEntityManager());
    }
    return instancia;
  }

  // Permite que los tests reemplacen la instancia global por una apuntada a HSQLDB.
  public static synchronized void setInstancia(DonanteRepository nueva) {
    instancia = nueva;
  }

  public void guardar(PersonaDonante donante) {
    enTransaccion(() -> entityManager.persist(donante));
  }

  public Optional<PersonaDonante> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(PersonaDonante.class, id));
  }

  // Consulta polimorfica: entra por PersonaDonante y el medio predeterminado se
  // filtra por tipo con TYPE(), sin importar si el donante es humano o juridico.
  public Optional<PersonaDonante> buscarPorEmail(String email) {
    List<PersonaDonante> encontrados = entityManager
        .createQuery(
            "SELECT d FROM PersonaDonante d "
                + "JOIN d.medioDeContactoPredeterminado m "
                + "WHERE TYPE(m) = Email AND LOWER(m.valor) = LOWER(:email)",
            PersonaDonante.class)
        .setParameter("email", email)
        .getResultList();
    return encontrados.stream().findFirst();
  }

  public List<PersonaDonante> obtenerTodos() {
    return entityManager
        .createQuery("SELECT d FROM PersonaDonante d", PersonaDonante.class)
        .getResultList();
  }

  public boolean eliminar(Long id) {
    PersonaDonante donante = entityManager.find(PersonaDonante.class, id);
    if (donante == null) {
      return false;
    }
    enTransaccion(() -> entityManager.remove(donante));
    return true;
  }

  public int cantidadDonantes() {
    return entityManager
        .createQuery("SELECT COUNT(d) FROM PersonaDonante d", Long.class)
        .getSingleResult()
        .intValue();
  }

  // Solo para tests: permite resetear el estado entre pruebas
  public void limpiar() {
    enTransaccion(() -> entityManager.createQuery("DELETE FROM PersonaDonante").executeUpdate());
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
