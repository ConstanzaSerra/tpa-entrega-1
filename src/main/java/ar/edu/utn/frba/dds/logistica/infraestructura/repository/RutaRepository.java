package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import java.util.List;
import java.util.Optional;

public interface RutaRepository {
    Ruta guardar(Ruta ruta);
    Optional<Ruta> buscarPorId(Long id);
    List<Ruta> buscarTodas();
}
