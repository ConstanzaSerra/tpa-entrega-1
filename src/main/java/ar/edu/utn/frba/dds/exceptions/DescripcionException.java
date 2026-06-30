package ar.edu.utn.frba.dds.exceptions;

public class DescripcionException extends RuntimeException {
  public DescripcionException(String message) {
    super(message);
  }

  public DescripcionException(String message, Throwable cause) {
    super(message, cause);
  }
}
