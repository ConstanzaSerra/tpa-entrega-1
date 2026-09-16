package ar.edu.utn.frba.dds.logistica.dominio;

import java.time.Instant;

/**
 * Posición reportada por la app móvil del conductor mientras la ruta está activa
 * (alternativa elegida para el monitoreo en tiempo real — ver entrega2-plan-diseño.md).
 */
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class PosicionCamion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  private Long camionId;
  private double latitud;
  private double longitud;
  private Double velocidad; // null si el dispositivo no la reporta
  private Instant timestamp;
  
  public PosicionCamion() {}

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
