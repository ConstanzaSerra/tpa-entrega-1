package ar.edu.utn.frba.dds.entidadesBeneficiarias;

import ar.edu.utn.frba.dds.contacto.MedioDeContacto;
import ar.edu.utn.frba.dds.necesidades.Necesidad;

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

  public MedioDeContacto getMedioDeContacto() {
    return medioDeContacto;
  }

  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    this.medioDeContacto = medioDeContacto;
  }

  public String getRazonSocial() {
    return razonSocial;
  }
}
