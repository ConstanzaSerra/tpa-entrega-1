package ar.edu.utn.frba.dds;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Subcategoria that)) return false;
    return Objects.equals(nombre, that.nombre) && categoria == that.categoria;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nombre, categoria);
  }
}
