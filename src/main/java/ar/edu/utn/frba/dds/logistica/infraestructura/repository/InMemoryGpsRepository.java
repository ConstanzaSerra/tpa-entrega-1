package ar.edu.utn.frba.dds.logistica.infraestructura.repository;

import ar.edu.utn.frba.dds.logistica.dominio.PosicionCamion;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGpsRepository implements GpsRepository {
    private final Map<Long, PosicionCamion> posiciones = new ConcurrentHashMap<>();

    @Override
    public void guardarPosicion(PosicionCamion posicion) {
        posiciones.put(posicion.getCamionId(), posicion);
    }

    @Override
    public Optional<PosicionCamion> obtenerUltimaPosicion(Long camionId) {
        return Optional.ofNullable(posiciones.get(camionId));
    }

    @Override
    public Map<Long, PosicionCamion> obtenerTodasLasPosiciones() {
        return Collections.unmodifiableMap(posiciones);
    }
}
