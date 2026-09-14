package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;

/**
 * Jerarquia mapeada como SINGLE_TABLE.
 *
 * Criterios (Clase 13):
 * - Distribucion de atributos: las tres hijas comparten el unico atributo
 *   (valor) y no agregan ninguno propio; lo que cambia es el comportamiento de
 *   enviarMensaje(). La tabla unica no tiene entonces ni una columna nullable.
 * - Consultas polimorficas: PersonaDonante guarda List<MedioDeContacto> y le
 *   pide enviarMensaje() sin saber el tipo, asi que la columna discriminadora
 *   es imprescindible para que Hibernate reconstruya la subclase correcta.
 *
 * Por que es entidad y no un @Embeddable: hay herencia, y el apunte es
 * explicito en que no es posible embeber herencias.
 */
@Entity
@Table(name = "medio_de_contacto")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_medio", discriminatorType = DiscriminatorType.STRING)
public abstract class MedioDeContacto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "valor", nullable = false)
  private String valor;

  protected MedioDeContacto() {}

  public MedioDeContacto(String valor) {
    this.valor = valor;
  }

  public Long getId() {
    return id;
  }

  public String getValor() {
    return valor;
  }

  public void setValor(String valor) {
    this.valor = valor;
  }

  public abstract void enviarMensaje(String mensaje);
}
