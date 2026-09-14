package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "entidad_beneficiaria")
public class EntidadBeneficiaria implements Notificable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "razon_social", nullable = false)
  private String razonSocial;

  @Column(name = "direccion")
  private String direccion;

  @Column(name = "telefono")
  private String telefono;

  // Lista de valores simples: @ElementCollection, no una entidad aparte.
  @ElementCollection
  @CollectionTable(name = "email_representante", joinColumns = @JoinColumn(name = "entidad_id"))
  @Column(name = "email")
  private List<String> emailsRepresentantes = new ArrayList<>();

  // Composicion: las necesidades no viven fuera de su entidad, por eso cascade
  // total y orphanRemoval (eliminarNecesidad debe borrarla tambien en la base).
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "entidad_id")
  private List<Necesidad> necesidades = new ArrayList<>();

  protected EntidadBeneficiaria() {}

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

  public Optional<Necesidad> buscarNecesidad(Long id) {
    return necesidades.stream().filter(n -> id.equals(n.getId())).findFirst();
  }

  public boolean eliminarNecesidad(Long id) {
    return necesidades.removeIf(n -> id.equals(n.getId()));
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
