package ar.edu.utn.frba.dds.donaciones.dto;

/**
 * DTO de necesidad material. Plano, con 'tipo' como discriminador
 * entre RECURRENTE y EXTRAORDINARIA.
 */
public class NecesidadDTO {
  public Long id;
  public String tipo;                // "RECURRENTE" | "EXTRAORDINARIA"
  public String subcategoriaNombre;
  public String categoria;           // ALIMENTOS | MOBILIARIO | VESTIMENTA
  public String descripcion;
  public Boolean satisfecha;         // solo respuesta

  // Solo recurrente
  public Integer cantidadObjetivo;
  public String periodoDescripcion;

  // Solo extraordinaria
  public Integer cantidadRequerida;

  // Comun a ambos
  public Integer cantidadRecibida;

  public NecesidadDTO() {
  }
}
