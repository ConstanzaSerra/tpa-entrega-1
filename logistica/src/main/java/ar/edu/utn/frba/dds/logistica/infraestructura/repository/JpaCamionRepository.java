package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.persistencia.EntityManagerProvider;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class JpaCamionRepository implements CamionRepository {
    private final EntityManager entityManager;

    public JpaCamionRepository() {
        this.entityManager = EntityManagerProvider.getEntityManager();
    }

    public JpaCamionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Camion guardar(Camion camion) {
        enTransaccion(() -> entityManager.persist(camion));
        return camion;
    }

    @Override
    public Optional<Camion> buscarPorId(Long id) {
        return Optional.ofNullable(entityManager.find(Camion.class, id));
    }

    @Override
    public List<Camion> buscarTodos() {
        return entityManager.createQuery("SELECT c FROM Camion c", Camion.class).getResultList();
    }

    @Override
    public Camion actualizar(Camion camion) {
        enTransaccion(() -> entityManager.merge(camion));
        return camion;
    }

    @Override
    public void eliminar(Long id) {
        Camion c = entityManager.find(Camion.class, id);
        if (c != null) {
            enTransaccion(() -> entityManager.remove(c));
        }
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
