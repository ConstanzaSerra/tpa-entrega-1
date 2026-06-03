package ar.edu.utn.frba.dds;

public class Subcategoria {
  private String nombre;
  private Categoria categoria;

  public Subcategoria(String nombre, Categoria categoria) {
    this.nombre = nombre;
    this.categoria = categoria;
  }

  public String getNombre() {
    return nombre;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public boolean requiereEstado() {
    return categoria.requiereEstado();
  }

  public boolean esPerecedero() {
    return categoria.esPerecedero();
  }
}
