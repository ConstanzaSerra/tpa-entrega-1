package ar.edu.utn.frba.dds;

public abstract class Bien {
  private String descripcion;
  private String foto;
  private Subcategoria subcategoria;
  private Integer cantidad;
  private String unidadDeMedida;

  public Bien(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida) {
    this.descripcion = descripcion;
    this.foto = foto;
    this.subcategoria = subcategoria;
    this.cantidad = cantidad;
    this.unidadDeMedida = unidadDeMedida;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public Integer getCantidad() {
    return cantidad;
  }
}
