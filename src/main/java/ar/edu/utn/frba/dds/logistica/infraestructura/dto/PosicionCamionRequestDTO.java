package ar.edu.utn.frba.dds.logistica.infraestructura.dto;

import java.time.Instant;

public class PosicionCamionRequestDTO {
    public double latitud;
    public double longitud;
    // Opcional: la app movil del conductor reporta geolocalizacion, la velocidad
    // solo llega si el dispositivo la provee
    public Double velocidad;
    public Instant timestamp;

    public PosicionCamionRequestDTO() {}

    public PosicionCamionRequestDTO(double latitud, double longitud, Double velocidad, Instant timestamp) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.velocidad = velocidad;
        this.timestamp = timestamp;
    }
}
