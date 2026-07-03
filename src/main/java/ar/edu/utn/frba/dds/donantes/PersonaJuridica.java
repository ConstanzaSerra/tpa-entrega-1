package ar.edu.utn.frba.dds.donantes;

import ar.edu.utn.frba.dds.contacto.MedioDeContacto;

import java.util.List;

public class PersonaJuridica extends PersonaDonante {
  private String razonSocial;
  private TipoJuridico tipo;
  private String rubro;
  private List<Representante> representantes;

  public PersonaJuridica(List<MedioDeContacto> medioDeContactos, MedioDeContacto medioDeContactoPredeterminado,
                         String razonSocial, TipoJuridico tipo, String rubro, List<Representante> representantes) {
    super(medioDeContactos, medioDeContactoPredeterminado);
    this.razonSocial = razonSocial;
    this.tipo = tipo;
    this.rubro = rubro;
    this.representantes = representantes;
  }

  public void agregarRepresentante(Representante representante) {
    this.representantes.add(representante);
  }

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }
}
