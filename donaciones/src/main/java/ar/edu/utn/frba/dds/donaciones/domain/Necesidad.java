package ar.edu.utn.frba.dds.donaciones.domain;

public abstract class Necesidad {
  private Long id;
  private Subcategoria subcategoria;
  private String descripcion;

  public Necesidad(Subcategoria subcategoria, String descripcion) {
    this.subcategoria = subcategoria;
    this.descripcion = descripcion;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public String getDescripcion() {return descripcion;}

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public abstract Boolean estaSatisfecha();

  public abstract void registrarRecepcion(Integer cantidad);
}
