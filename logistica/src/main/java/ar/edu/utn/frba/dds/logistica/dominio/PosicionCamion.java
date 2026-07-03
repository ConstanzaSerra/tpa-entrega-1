package ar.edu.utn.frba.dds.logistica.dominio;

import java.time.Instant;

/**
 * Posición reportada por la app móvil del conductor mientras la ruta está activa
 * (alternativa elegida para el monitoreo en tiempo real — ver entrega2-plan-diseño.md).
 */

public class PosicionCamion {
  private final Long camionId;
  private final double latitud;
  private final double longitud;
  private final Double velocidad; // null si el dispositivo no la reporta
  private final Instant timestamp;

  public PosicionCamion(Long camionId, double latitud, double longitud, Double velocidad, Instant timestamp) {
    this.camionId = camionId;
    this.latitud = latitud;
    this.longitud = longitud;
    this.velocidad = velocidad;
    this.timestamp = timestamp;
  }

  public Long getCamionId() { return camionId; }
  public double getLatitud() { return latitud; }
  public double getLongitud() { return longitud; }
  public Double getVelocidad() { return velocidad; }
  public Instant getTimestamp() { return timestamp; }
}
