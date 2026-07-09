package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

import ar.edu.utn.frba.dds.logistica.dominio.*;

import java.time.Instant;

public class DashboardCamionDTO {
    public Long camionId;
    public String patente;
    public PosicionDTO posicion;
    public RutaAvanceDTO ruta;

    public static class PosicionDTO {
        public double latitud;
        public double longitud;
        public Double velocidad;
        public Instant timestamp;
    }

    public static class RutaAvanceDTO {
        public Long rutaId;
        public int entregasTotal;
        public int entregasFinalizadas;
        public int avancePorcentaje;
    }

    public static DashboardCamionDTO build(Camion camion, PosicionCamion posicion, Ruta rutaActiva) {
        DashboardCamionDTO dto = new DashboardCamionDTO();
        dto.camionId = camion.getId();
        dto.patente = camion.getPatente();

        if (posicion != null) {
            PosicionDTO pos = new PosicionDTO();
            pos.latitud = posicion.getLatitud();
            pos.longitud = posicion.getLongitud();
            pos.velocidad = posicion.getVelocidad();
            pos.timestamp = posicion.getTimestamp();
            dto.posicion = pos;
        }

        if (rutaActiva != null) {
            long total = rutaActiva.getParadas().stream()
                    .mapToLong(p -> p.getEntregas().size())
                    .sum();
            long finalizadas = rutaActiva.getParadas().stream()
                    .flatMap(p -> p.getEntregas().stream())
                    .filter(e -> e.getEstado() == EstadoEntrega.ENTREGADA
                              || e.getEstado() == EstadoEntrega.NO_RECIBIDA)
                    .count();

            RutaAvanceDTO avance = new RutaAvanceDTO();
            avance.rutaId = rutaActiva.getId();
            avance.entregasTotal = (int) total;
            avance.entregasFinalizadas = (int) finalizadas;
            avance.avancePorcentaje = total == 0 ? 0 : (int) (finalizadas * 100 / total);
            dto.ruta = avance;
        }

        return dto;
    }
}
