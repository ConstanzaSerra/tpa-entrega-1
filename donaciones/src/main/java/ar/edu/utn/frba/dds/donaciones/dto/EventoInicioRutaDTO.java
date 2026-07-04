package ar.edu.utn.frba.dds.donaciones.dto;

import java.util.List;

public class EventoInicioRutaDTO {
  public Long rutaId;
  public String patenteCamion;
  public String linkMapa;
  public List<EntregaAfectadaDTO> entregasAfectadas;

  public EventoInicioRutaDTO() {
  }
}
