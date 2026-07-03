package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

import java.time.Instant;

public class PosicionCamionRequestDTO {
    public double latitud;
    public double longitud;
    public double velocidad;
    public Instant timestamp;

    public PosicionCamionRequestDTO() {}

    public PosicionCamionRequestDTO(double latitud, double longitud, double velocidad, Instant timestamp) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.velocidad = velocidad;
        this.timestamp = timestamp;
    }
}
