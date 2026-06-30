package ar.edu.utn.frba.dds.logistica.repository;

import ar.edu.utn.frba.dds.logistica.domain.Camion;
import java.util.List;
import java.util.Optional;

public interface CamionRepository {
    Camion guardar(Camion camion);
    Optional<Camion> buscarPorId(Long id);
    List<Camion> buscarTodos();
    Camion actualizar(Camion camion);
    void eliminar(Long id);
}
