package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persona_donante")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PersonaDonante implements Notificable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "donante_id")
  private List<MedioDeContacto> medioDeContactos = new ArrayList<>();

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "medio_predeterminado_id")
  private MedioDeContacto medioDeContactoPredeterminado;

  @Column(name = "ultima_interaccion")
  private LocalDate ultimaInteraccion = LocalDate.now();

  protected PersonaDonante() {}

  public PersonaDonante(List<MedioDeContacto> medioDeContactos, MedioDeContacto medioDeContactoPredeterminado) {
    this.medioDeContactos = medioDeContactos;
    this.medioDeContactoPredeterminado = medioDeContactoPredeterminado;
  }

  public void agregarMedio(MedioDeContacto medio) {
    this.medioDeContactos.add(medio);
  }

  public void setPredeterminado(MedioDeContacto medio) {
    this.medioDeContactoPredeterminado = medio;
  }

  public MedioDeContacto getMedioDeContactoPredeterminado() {
    return medioDeContactoPredeterminado;
  }

  @Override
  public MedioDeContacto medioDePreferencia() {
    return medioDeContactoPredeterminado;
  }

  public List<MedioDeContacto> getMedioDeContactos() {
    return medioDeContactos;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public LocalDate getUltimaInteraccion() {
    return ultimaInteraccion;
  }

  public void setUltimaInteraccion(LocalDate ultimaInteraccion) {
    this.ultimaInteraccion = ultimaInteraccion;
  }

  // Marca que el donante interactuo con la plataforma hoy.
  public void registrarInteraccion() {
    this.ultimaInteraccion = LocalDate.now();
  }

  public void actualizarEmail(String nuevoEmail) {
    medioDeContactos.stream()
        .filter(m -> m instanceof Email)
        .findFirst()
        .ifPresent(m -> m.setValor(nuevoEmail));
  }

  public void actualizarTelefono(String nuevoTelefono) {
    medioDeContactos.stream()
        .filter(m -> m instanceof Telefono)
        .findFirst()
        .ifPresent(m -> m.setValor(nuevoTelefono));
  }
}
