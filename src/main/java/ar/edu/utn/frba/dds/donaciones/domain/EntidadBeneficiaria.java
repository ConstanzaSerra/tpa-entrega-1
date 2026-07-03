package ar.edu.utn.frba.dds.donaciones.domain;

import java.util.List;

public class EntidadBeneficiaria {
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
}
