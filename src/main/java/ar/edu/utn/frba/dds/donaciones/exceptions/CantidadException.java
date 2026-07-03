package ar.edu.utn.frba.dds.donaciones.exceptions;

public class CantidadException extends RuntimeException {
  public CantidadException(String message) {
    super(message);
  }

  public CantidadException(String message, Throwable cause) {
    super(message, cause);
  }
}