package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;

/**
 * Entidad, no valor: tiene nombre propio, se comparte entre bienes, donaciones
 * y necesidades, y alguien administra su alta y baja. Segun el criterio de la
 * Clase 14, si necesito darle nombre a algo, es una entidad.
 */
@Entity
@Table(name = "subcategoria")
public class Subcategoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre", nullable = false)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(name = "categoria", nullable = false)
  private Categoria categoria;

  @Column(name = "requiere_estado")
  private Boolean requiereEstado;

  @Column(name = "es_perecedero")
  private Boolean esPeredecedero;

  protected Subcategoria() {}

  public Subcategoria(String nombre, Categoria categoria, Boolean requiereEstado, Boolean esPeredecedero) {
    this.nombre = nombre;
    this.categoria = categoria;
    this.requiereEstado = requiereEstado;
    this.esPeredecedero = esPeredecedero;
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public Categoria getCategoria() {
    return categoria;
  }
}
