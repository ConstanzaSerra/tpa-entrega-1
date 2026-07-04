package ar.edu.utn.frba.dds.donaciones.domain;

import java.util.List;

public class EntidadBeneficiaria implements Notificable {
  private Long id;
  private String razonSocial;
  private String direccion;
  private String telefono;
  private List<String> emailsRepresentantes;
  private List<Necesidad> necesidades;

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

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public void setEmailsRepresentantes(List<String> emailsRepresentantes) {
    this.emailsRepresentantes = emailsRepresentantes;
  }

  @Override
  public MedioDeContacto medioDePreferencia() {
    if (emailsRepresentantes != null && !emailsRepresentantes.isEmpty()) {
      return new Email(emailsRepresentantes.get(0));
    }
    if (telefono != null && !telefono.isBlank()) {
      return new Telefono(telefono);
    }
    return null;
  }
}
