package ar.edu.utn.frba.dds.donaciones.dto;

import java.time.LocalDateTime;

public class EventoEntregaConfirmadaDTO {
  public Long entregaId;
  public Long donacionId;
  public String patenteCamion;
  public LocalDateTime fechaHora;

  public EventoEntregaConfirmadaDTO() {
  }
}
