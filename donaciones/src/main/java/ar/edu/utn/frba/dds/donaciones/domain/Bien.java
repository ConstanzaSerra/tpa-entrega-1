package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;

/**
 * Jerarquia mapeada como SINGLE_TABLE.
 *
 * Criterios (Clase 13):
 * - Distribucion de atributos: las hijas agregan a lo sumo un atributo cada una
 *   (esNuevo, fechaVencimiento), asi que la tabla unica queda angosta.
 * - Nullabilidad: ninguno de esos atributos necesita NOT NULL, que es
 *   justamente lo que SINGLE_TABLE exige de los campos no comunes.
 * - Consultas polimorficas: CargaDeDonacion trabaja con List<Bien> sin
 *   distinguir el tipo, con lo cual son necesarias y aca salen sin JOIN.
 */
@Entity
@Table(name = "bien")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_bien", discriminatorType = DiscriminatorType.STRING)
public abstract class Bien {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "descripcion")
  private String descripcion;

  @Column(name = "foto")
  private String foto;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private Subcategoria subcategoria;

  @Column(name = "cantidad")
  private Integer cantidad;

  @Column(name = "unidad_de_medida")
  private String unidadDeMedida;

  protected Bien() {}

  public Bien(String descripcion, String foto, Subcategoria subcategoria, Integer cantidad, String unidadDeMedida) {
    this.descripcion = descripcion;
    this.foto = foto;
    this.subcategoria = subcategoria;
    this.cantidad = cantidad;
    this.unidadDeMedida = unidadDeMedida;
  }

  public Long getId() {
    return id;
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

  public String getUnidadDeMedida() {
    return unidadDeMedida;
  }
}
