package ar.edu.utn.frba.dds.donaciones.dto;

/**
 * DTO completo de una donacion, para el CRUD.
 * Distinto del DonacionParaRutaDTO (que es la vista reducida que consume Logistica).
 */
public class DonacionDTO {
  public Long id;
  public String estado;             // solo respuesta
  public Integer cantidad;
  public String unidadMedida;
  public String fechaRegistro;      // solo respuesta (fecha ISO)
  public Long donanteId;            // pedido + respuesta
  public String subcategoriaNombre; // pedido + respuesta
  public String categoria;          // pedido + respuesta (ALIMENTOS | MOBILIARIO | VESTIMENTA)
  public Long entidadAsignadaId;    // solo respuesta

  public DonacionDTO() {
  }
}
