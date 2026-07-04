package ar.edu.utn.frba.dds.donaciones.domain;

/**
 * Algo que puede ser notificado (persona donante, entidad beneficiaria, etc.).
 * Expone el medio de contacto por el que prefiere recibir la notificacion.
 */
public interface Notificable {
  MedioDeContacto medioDePreferencia();
}
