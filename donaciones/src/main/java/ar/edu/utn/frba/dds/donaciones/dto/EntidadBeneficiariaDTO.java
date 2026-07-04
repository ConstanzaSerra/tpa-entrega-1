package ar.edu.utn.frba.dds.donaciones.dto;

import java.util.List;

public class EntidadBeneficiariaDTO {
  public Long id;
  public String razonSocial;
  public String direccion;
  public String telefono;
  public List<String> emailsRepresentantes;

  public EntidadBeneficiariaDTO() {
  }
}
