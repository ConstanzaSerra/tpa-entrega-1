<<<<<<<< HEAD:src/main/java/ar/edu/utn/frba/dds/donaciones/domain/PersonaDonante.java
package ar.edu.utn.frba.dds.donaciones.domain;
========
package ar.edu.utn.frba.dds.donantes;

import ar.edu.utn.frba.dds.contacto.Email;
import ar.edu.utn.frba.dds.contacto.MedioDeContacto;
import ar.edu.utn.frba.dds.contacto.Telefono;
>>>>>>>> temp:src/main/java/ar/edu/utn/frba/dds/donantes/PersonaDonante.java

import java.util.List;

public abstract class PersonaDonante {
  private List<MedioDeContacto> medioDeContactos;
  private MedioDeContacto medioDeContactoPredeterminado;

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

  public void actualizarEmail(String nuevoEmail) {
    Email validado = new Email(nuevoEmail);
    medioDeContactos.replaceAll(m -> m instanceof Email ? validado : m);
    if (medioDeContactoPredeterminado instanceof Email) {
      medioDeContactoPredeterminado = validado;
    }
  }

  public void actualizarTelefono(String nuevoTelefono) {
    medioDeContactos.stream()
        .filter(m -> m instanceof Telefono)
        .findFirst()
        .ifPresent(m -> m.setValor(nuevoTelefono));
  }
}
