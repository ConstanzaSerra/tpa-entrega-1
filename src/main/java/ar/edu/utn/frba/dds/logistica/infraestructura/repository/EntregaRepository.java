package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import java.util.List;
import java.util.Optional;

public interface EntregaRepository {
    Entrega guardar(Entrega entrega);
    Optional<Entrega> buscarPorId(Long id);
    List<Entrega> buscarTodas();
    List<Entrega> buscarPorDonacionId(Long donacionId);
    List<Entrega> buscarPorEstado(EstadoEntrega estado);
    void eliminar(Long id);
}
