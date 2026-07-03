<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/EntidadBeneficiaria.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.entidadesBeneficiarias;

import ar.edu.utn.frba.dds.contacto.MedioDeContacto;
import ar.edu.utn.frba.dds.necesidades.Necesidad;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/entidadesBeneficiarias/EntidadBeneficiaria.java

import java.util.List;

public class EntidadBeneficiaria {
  private Long id;
  private String razonSocial;
  private String direccion;
  private String telefono;
  private List<String> emailsRepresentantes;
  private List<Necesidad> necesidades;
  private MedioDeContacto medioDeContacto;

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono,
                             List<String> emailsRepresentantes, List<Necesidad> necesidades) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.emailsRepresentantes = emailsRepresentantes;
    this.necesidades = necesidades;
  }

  public void agregarNecesidad(Necesidad necesidad) {
    this.necesidades.add(necesidad);
  }

  public List<Necesidad> getNecesidades() {
    return necesidades;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/EntidadBeneficiaria.java
  public String getRazonSocial() {
    return razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public List<String> getEmailsRepresentantes() {
    return emailsRepresentantes;
  }
========
  public MedioDeContacto getMedioDeContacto() {
    return medioDeContacto;
  }

  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    this.medioDeContacto = medioDeContacto;
  }

  public String getRazonSocial() {
    return razonSocial;
  }
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/entidadesBeneficiarias/EntidadBeneficiaria.java
}
