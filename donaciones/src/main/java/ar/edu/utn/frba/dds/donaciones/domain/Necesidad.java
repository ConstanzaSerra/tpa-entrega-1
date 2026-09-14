package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;

/**
 * Jerarquia mapeada como SINGLE_TABLE.
 *
 * Criterios (Clase 13):
 * - Distribucion de atributos: las hijas suman pocos campos (dos enteros una,
 *   un embebible de tres columnas la otra). La tabla unica no se ensancha tanto
 *   como para justificar el JOIN permanente que impone JOINED.
 * - Consultas polimorficas: EntidadBeneficiaria guarda List<Necesidad> y le
 *   pide estaSatisfecha() sin distinguir el tipo; SINGLE_TABLE las resuelve con
 *   un SELECT sin JOIN ni UNION.
 * - Nullabilidad: es el costo asumido. cantidad_requerida deberia ser NOT NULL
 *   en NecesidadExtraordinaria, pero SINGLE_TABLE obliga a que los campos no
 *   comunes sean nullables, asi que esa regla queda unicamente en el
 *   constructor (validarCantidadRequerida) y no en la base.
 */
@Entity
@Table(name = "necesidad")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_necesidad", discriminatorType = DiscriminatorType.STRING)
public abstract class Necesidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private Subcategoria subcategoria;

  @Column(name = "descripcion")
  private String descripcion;

  protected Necesidad() {}

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

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public abstract Boolean estaSatisfecha();

  public abstract void registrarRecepcion(Integer cantidad);
}
