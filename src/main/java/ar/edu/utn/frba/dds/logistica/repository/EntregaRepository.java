package ar.edu.utn.frba.dds.logistica.repository;

import ar.edu.utn.frba.dds.logistica.domain.Entrega;
import java.util.List;
import java.util.Optional;

public interface EntregaRepository {
    Entrega guardar(Entrega entrega);
    Optional<Entrega> buscarPorId(Long id);
    List<Entrega> buscarTodas();
}
