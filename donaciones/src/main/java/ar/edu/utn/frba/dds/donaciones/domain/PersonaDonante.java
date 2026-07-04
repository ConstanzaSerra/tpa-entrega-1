package ar.edu.utn.frba.dds.donaciones.domain;

import java.util.List;

public abstract class PersonaDonante {
  private Long id;
  private List<MedioDeContacto> medioDeContactos;
  private MedioDeContacto medioDeContactoPredeterminado;

  public PersonaDonante(List<MedioDeContacto> medioDeContactos, MedioDeContacto medioDeContactoPredeterminado) {
    this.medioDeContactos = medioDeContactos;
    this.medioDeContactoPredeterminado = medioDeContactoPredeterminado;
  }

  public void agregarMedio(MedioDeContacto medio){
    this.medioDeContactos.add(medio);
  }

  public void setPredeterminado(MedioDeContacto medio) {
    this.medioDeContactoPredeterminado = medio;
  }

  public MedioDeContacto getMedioDeContactoPredeterminado() {
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
