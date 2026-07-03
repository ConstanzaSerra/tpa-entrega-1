package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Entrega;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryEntregaRepository implements EntregaRepository {
    private final List<Entrega> entregas = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Entrega guardar(Entrega entrega) {
        if (entrega.getId() == null) {
            entrega.setId(idGenerator.getAndIncrement());
            entregas.add(entrega);
        }
        return entrega;
    }

    @Override
    public Optional<Entrega> buscarPorId(Long id) {
        return entregas.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Entrega> buscarTodas() {
        return new ArrayList<>(entregas);
    }
}
