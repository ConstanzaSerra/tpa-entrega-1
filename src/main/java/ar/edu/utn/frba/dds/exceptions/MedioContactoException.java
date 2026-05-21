package ar.edu.utn.frba.dds.exceptions;

public class MedioContactoException extends RuntimeException {

  public MedioContactoException(String message) {
    super(message);
  }

  public MedioContactoException(String message, Throwable cause) {
    super(message, cause);
  }
}
