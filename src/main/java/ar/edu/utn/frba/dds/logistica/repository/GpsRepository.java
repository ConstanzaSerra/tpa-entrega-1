package ar.edu.utn.frba.dds.logistica.repository;

import ar.edu.utn.frba.dds.logistica.domain.PosicionCamion;

import java.util.Map;
import java.util.Optional;

public interface GpsRepository {
    void guardarPosicion(PosicionCamion posicion);
    Optional<PosicionCamion> obtenerUltimaPosicion(Long camionId);
    Map<Long, PosicionCamion> obtenerTodasLasPosiciones();
}
