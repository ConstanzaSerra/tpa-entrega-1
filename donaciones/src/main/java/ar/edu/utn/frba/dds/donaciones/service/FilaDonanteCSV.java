package ar.edu.utn.frba.dds.donaciones.service;

/**
 * Una fila del CSV de donantes ya parseada, todavia sin interpretar como dominio.
 * Es el contrato entre el LectorDonantesCSV (parseo) y el ImportadorDonantesCSV (negocio).
 */
public record FilaDonanteCSV(
    String tipo,
    String tipoDocumento,
    String documento,
    String nombre,
    String email,
    String telefono) {
}
