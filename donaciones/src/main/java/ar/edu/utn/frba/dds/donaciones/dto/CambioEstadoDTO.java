package ar.edu.utn.frba.dds.donaciones.dto;

public class CambioEstadoDTO {
  public String estado;
  public String fechaHora;

  public CambioEstadoDTO() {
  }

  public CambioEstadoDTO(String estado, String fechaHora) {
    this.estado = estado;
    this.fechaHora = fechaHora;
  }
}
