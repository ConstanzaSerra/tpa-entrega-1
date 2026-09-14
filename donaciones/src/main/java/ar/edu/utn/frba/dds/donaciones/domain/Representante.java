package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Valor, no entidad (Clase 14): un representante solo tiene sentido dentro de
 * la PersonaJuridica que lo declara, no hay ABM propio ni nadie lo referencia
 * desde afuera. Interesan sus atributos, no su identidad, asi que no lleva id.
 *
 * Se persiste como coleccion de embebibles (@ElementCollection en
 * PersonaJuridica). Contrapartida que marca el apunte: como los valores no
 * tienen id, al quitar uno se borran todos y se reinsertan.
 */
@Embeddable
public class Representante {

  @Column(name = "nombre")
  private String nombre;

  @Column(name = "apellido")
  private String apellido;

  @Column(name = "dni")
  private Integer dni;

  @Column(name = "email")
  private String email;

  protected Representante() {}

  public Representante(String nombre, String apellido, Integer dni, String email) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.dni = dni;
    this.email = email;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public Integer getDni() {
    return dni;
  }

  public String getEmail() {
    return email;
  }
}
