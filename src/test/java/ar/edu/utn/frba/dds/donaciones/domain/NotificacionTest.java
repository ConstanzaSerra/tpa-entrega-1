package ar.edu.utn.frba.dds.donaciones.domain;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;
import ar.edu.utn.frba.dds.donaciones.service.*;

import ar.edu.utn.frba.dds.contacto.MedioDeContacto;
import ar.edu.utn.frba.dds.contacto.Notificacion;
import ar.edu.utn.frba.dds.donantes.PersonaDonante;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class NotificacionTest {

  private Notificacion notificacion;
  private PersonaDonante personaDonante;
  private MedioDeContacto medioDeContacto;

  @BeforeEach
  void setUp() {

    this.personaDonante = mock(PersonaDonante.class);
    this.medioDeContacto = mock(MedioDeContacto.class);

    when(this.personaDonante.getMedioDeContactoPredeterminado()).thenReturn(this.medioDeContacto);

    this.notificacion = new Notificacion(this.personaDonante, "Donacion recibida", this.medioDeContacto);

  }

  @Test
  public void envioDeNotificacion() {
    this.notificacion.enviar();

    verify(this.medioDeContacto, times(1)).enviarMensaje(this.notificacion);

    assertTrue(this.notificacion.getCompletada());

  }

}
