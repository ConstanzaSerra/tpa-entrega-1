package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCamionRepository implements CamionRepository {
    private final List<Camion> camiones = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Camion guardar(Camion camion) {
        camion.setId(idGenerator.getAndIncrement());
        camiones.add(camion);
        return camion;
    }

    @Override
    public Optional<Camion> buscarPorId(Long id) {
        return camiones.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Camion> buscarTodos() {
        return new ArrayList<>(camiones);
    }

    @Override
    public Camion actualizar(Camion camionActualizado) {
        Optional<Camion> camionExistente = buscarPorId(camionActualizado.getId());
        if (camionExistente.isPresent()) {
            Camion c = camionExistente.get();
            c.setPatente(camionActualizado.getPatente());
            c.setVolumenM3(camionActualizado.getVolumenM3());
            c.setAlturaM(camionActualizado.getAlturaM());
            c.setCapacidadKg(camionActualizado.getCapacidadKg());
            return c;
        }
        throw new IllegalArgumentException("Camion no encontrado con ID: " + camionActualizado.getId());
    }

    @Override
    public void eliminar(Long id) {
        camiones.removeIf(c -> c.getId().equals(id));
    }
}
