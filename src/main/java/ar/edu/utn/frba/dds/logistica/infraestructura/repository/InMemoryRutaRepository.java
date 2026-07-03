package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Ruta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRutaRepository implements RutaRepository {
    private final List<Ruta> rutas = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Ruta guardar(Ruta ruta) {
        if (ruta.getId() == null) {
            ruta.setId(idGenerator.getAndIncrement());
            rutas.add(ruta);
        }
        return ruta;
    }

    @Override
    public Optional<Ruta> buscarPorId(Long id) {
        return rutas.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Ruta> buscarTodas() {
        return new ArrayList<>(rutas);
    }
}
