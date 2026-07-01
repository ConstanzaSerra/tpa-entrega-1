package ar.edu.utn.frba.dds;

import java.util.List;

public class PropuestaMatchmaking {
  private Donacion donacion;
  private List<EntidadBeneficiaria> entidadesSugeridas;
  private boolean esCoincidenciaExacta;

  public PropuestaMatchmaking(Donacion donacion, List<EntidadBeneficiaria> entidadesSugeridas, boolean esCoincidenciaExacta) {
    this.donacion = donacion;
    this.entidadesSugeridas = entidadesSugeridas;
    this.esCoincidenciaExacta = esCoincidenciaExacta;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public List<EntidadBeneficiaria> getEntidadesSugeridas() {
    return entidadesSugeridas;
  }

  public boolean isEsCoincidenciaExacta() {
    return esCoincidenciaExacta;
  }
}
