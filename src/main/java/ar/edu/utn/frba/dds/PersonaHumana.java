package ar.edu.utn.frba.dds;

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


}
