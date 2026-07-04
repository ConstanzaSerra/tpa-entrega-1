package ar.edu.utn.frba.dds.donaciones.dto;

import java.util.List;

/**
 * Resultado del matchmaking: el ranking de entidades sugeridas para una donacion.
 */
public class PropuestaDTO {
  public Long donacionId;
  public boolean esCoincidenciaExacta; // true si las entidades salieron de ambos algoritmos
  public List<EntidadBeneficiariaDTO> entidadesSugeridas;

  public PropuestaDTO() {
  }
}
