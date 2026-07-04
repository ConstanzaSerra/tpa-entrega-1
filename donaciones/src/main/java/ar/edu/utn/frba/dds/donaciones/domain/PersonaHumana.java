package ar.edu.utn.frba.dds.donaciones.domain;

import java.util.List;

public class PersonaHumana extends PersonaDonante {
  private String nombre;
  private String apellido;
  private Integer edad;
  private Integer dni;
  private String genero;
  private String direccion;

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
