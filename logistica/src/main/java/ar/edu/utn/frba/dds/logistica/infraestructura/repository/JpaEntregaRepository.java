package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import ar.edu.utn.frba.dds.logistica.persistencia.EntityManagerProvider;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class JpaEntregaRepository implements EntregaRepository {
    private final EntityManager entityManager;

    public JpaEntregaRepository() {
        this.entityManager = EntityManagerProvider.getEntityManager();
    }

    public JpaEntregaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Entrega guardar(Entrega entrega) {
        enTransaccion(() -> {
            if (entrega.getId() == null) {
                entityManager.persist(entrega);
            } else {
                entityManager.merge(entrega);
            }
        });
        return entrega;
    }

    @Override
    public Optional<Entrega> buscarPorId(Long id) {
        return Optional.ofNullable(entityManager.find(Entrega.class, id));
    }

    @Override
    public List<Entrega> buscarTodas() {
        return entityManager.createQuery("SELECT e FROM Entrega e", Entrega.class).getResultList();
    }

    @Override
    public List<Entrega> buscarPorDonacionId(Long donacionId) {
        return entityManager.createQuery("SELECT e FROM Entrega e WHERE e.donacionId = :donacionId", Entrega.class)
                .setParameter("donacionId", donacionId)
                .getResultList();
    }

    @Override
    public List<Entrega> buscarPorEstado(EstadoEntrega estado) {
        return entityManager.createQuery("SELECT e FROM Entrega e WHERE e.estado = :estado", Entrega.class)
                .setParameter("estado", estado)
                .getResultList();
    }

    @Override
    public void eliminar(Long id) {
        Entrega e = entityManager.find(Entrega.class, id);
        if (e != null) {
            enTransaccion(() -> entityManager.remove(e));
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
