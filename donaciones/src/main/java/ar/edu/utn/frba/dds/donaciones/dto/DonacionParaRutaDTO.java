package ar.edu.utn.frba.dds.donaciones.dto;

/**
 * DTO del contrato con el microservicio de Logistica.
 * Representa una donacion lista para ser incluida en la planificacion de rutas.
 * Los nombres de los campos deben coincidir con los que espera Logistica.
 */
public class DonacionParaRutaDTO {
  public Long donacionId;
  public Long entidadBeneficiariaId;
  public String direccionDestino;

  public DonacionParaRutaDTO() {
  }

  public DonacionParaRutaDTO(Long donacionId, Long entidadBeneficiariaId, String direccionDestino) {
    this.donacionId = donacionId;
    this.entidadBeneficiariaId = entidadBeneficiariaId;
    this.direccionDestino = direccionDestino;
  }
}
