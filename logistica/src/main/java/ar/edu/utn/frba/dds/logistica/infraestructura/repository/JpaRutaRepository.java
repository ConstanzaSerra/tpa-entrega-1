package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.persistencia.EntityManagerProvider;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class JpaRutaRepository implements RutaRepository {
    private final EntityManager entityManager;

    public JpaRutaRepository() {
        this.entityManager = EntityManagerProvider.getEntityManager();
    }

    public JpaRutaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Ruta guardar(Ruta ruta) {
        enTransaccion(() -> {
            if (ruta.getId() == null) {
                entityManager.persist(ruta);
            } else {
                entityManager.merge(ruta);
            }
        });
        return ruta;
    }

    @Override
    public Optional<Ruta> buscarPorId(Long id) {
        return Optional.ofNullable(entityManager.find(Ruta.class, id));
    }

    @Override
    public List<Ruta> buscarTodas() {
        return entityManager.createQuery("SELECT r FROM Ruta r", Ruta.class).getResultList();
    }

    @Override
    public void eliminar(Long id) {
        Ruta r = entityManager.find(Ruta.class, id);
        if (r != null) {
            enTransaccion(() -> entityManager.remove(r));
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
