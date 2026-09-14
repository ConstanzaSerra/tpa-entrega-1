package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "persona_humana")
@PrimaryKeyJoinColumn(name = "persona_donante_id")
public class PersonaHumana extends PersonaDonante {

  @Column(name = "nombre", nullable = false)
  private String nombre;

  @Column(name = "apellido", nullable = false)
  private String apellido;

  @Column(name = "edad")
  private Integer edad;

  @Column(name = "dni", nullable = false)
  private Integer dni;

  @Column(name = "genero")
  private String genero;

  @Column(name = "direccion")
  private String direccion;

  protected PersonaHumana() {}

  public PersonaHumana(List<MedioDeContacto> medioDeContactos, MedioDeContacto medioDeContactoPredeterminado,
                       String nombre, String apellido, Integer edad, Integer dni, String genero, String direccion) {
    super(medioDeContactos, medioDeContactoPredeterminado);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.dni = dni;
    this.genero = genero;
    this.direccion = direccion;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public void setEdad(Integer edad) {
    this.edad = edad;
  }

  public void setDni(Integer dni) {
    this.dni = dni;
  }

  public void setGenero(String genero) {
    this.genero = genero;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public Integer getEdad() {
    return edad;
  }

  public Integer getDni() {
    return dni;
  }

  public String getGenero() {
    return genero;
  }

  public String getDireccion() {
    return direccion;
  }
}
