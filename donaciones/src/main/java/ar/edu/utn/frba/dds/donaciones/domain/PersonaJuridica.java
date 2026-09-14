package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persona_juridica")
@PrimaryKeyJoinColumn(name = "persona_donante_id")
public class PersonaJuridica extends PersonaDonante {

  @Column(name = "razon_social", nullable = false)
  private String razonSocial;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo")
  private TipoJuridico tipo;

  @Column(name = "rubro")
  private String rubro;

  @ElementCollection
  @CollectionTable(name = "representante", joinColumns = @JoinColumn(name = "persona_juridica_id"))
  private List<Representante> representantes = new ArrayList<>();

  protected PersonaJuridica() {}

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

  public void setTipo(TipoJuridico tipo) {
    this.tipo = tipo;
  }

  public void setRubro(String rubro) {
    this.rubro = rubro;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public TipoJuridico getTipo() {
    return tipo;
  }

  public String getRubro() {
    return rubro;
  }

  public List<Representante> getRepresentantes() {
    return representantes;
  }
}
