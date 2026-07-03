package ar.edu.utn.frba.dds.donaciones.domain;

public class Subcategoria {
  private String nombre;
  private Categoria categoria;
  private Boolean requiereEstado;
  private Boolean esPeredecedero;

  public Subcategoria(String nombre, Categoria categoria, Boolean requiereEstado, Boolean esPeredecedero) {
    this.nombre = nombre;
    this.categoria = categoria;
    this.requiereEstado = requiereEstado;
    this.esPeredecedero = esPeredecedero;
  }


  public String getNombre() {
    return nombre;
  }

  public Categoria getCategoria() {
    return categoria;
  }
}
