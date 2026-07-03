package ar.edu.utn.frba.dds.donaciones.exceptions;

public class PeriodoConsumoException extends RuntimeException {
  public PeriodoConsumoException(String message){ super(message);}

  public PeriodoConsumoException(String message, Throwable cause) {
    super(message, cause);
  }
}
