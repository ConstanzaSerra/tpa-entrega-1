package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.PosicionCamion;
import ar.edu.utn.frba.dds.logistica.persistencia.EntityManagerProvider;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JpaGpsRepository implements GpsRepository {
    private final EntityManager entityManager;

    public JpaGpsRepository() {
        this.entityManager = EntityManagerProvider.getEntityManager();
    }

    public JpaGpsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void guardarPosicion(PosicionCamion posicion) {
        enTransaccion(() -> entityManager.persist(posicion));
    }

    @Override
    public Optional<PosicionCamion> obtenerUltimaPosicion(Long camionId) {
        List<PosicionCamion> posiciones = entityManager.createQuery(
                "SELECT p FROM PosicionCamion p WHERE p.camionId = :camionId ORDER BY p.timestamp DESC", 
                PosicionCamion.class)
                .setParameter("camionId", camionId)
                .setMaxResults(1)
                .getResultList();
        
        return posiciones.isEmpty() ? Optional.empty() : Optional.of(posiciones.get(0));
    }

    @Override
    public Map<Long, PosicionCamion> obtenerTodasLasPosiciones() {
        // Obtenemos la posicion mas reciente por camion
        List<PosicionCamion> posiciones = entityManager.createQuery(
            "SELECT p FROM PosicionCamion p WHERE p.timestamp = (SELECT MAX(p2.timestamp) FROM PosicionCamion p2 WHERE p2.camionId = p.camionId)",
            PosicionCamion.class
        ).getResultList();
        
        return posiciones.stream()
                .collect(Collectors.toMap(PosicionCamion::getCamionId, p -> p));
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
