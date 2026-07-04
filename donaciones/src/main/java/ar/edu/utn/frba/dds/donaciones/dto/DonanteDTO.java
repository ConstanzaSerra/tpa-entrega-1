package ar.edu.utn.frba.dds.donaciones.dto;

/**
 * DTO de personas donantes. Es plano y usa el campo 'tipo' como discriminador
 * entre HUMANA y JURIDICA; segun el tipo se completan unos u otros campos.
 */
public class DonanteDTO {
  public Long id;
  public String tipo;          // "HUMANA" | "JURIDICA"
  public String email;
  public String telefono;

  // Solo persona humana
  public String nombre;
  public String apellido;
  public Integer edad;
  public Integer dni;
  public String genero;
  public String direccion;

  // Solo persona juridica
  public String razonSocial;
  public String tipoJuridico;  // GUBERNAMENTAL | ONG | EMPRESA | INSTITUCION
  public String rubro;

  // Fecha ISO (yyyy-MM-dd) de ultima interaccion. Opcional al crear; util para import/testing.
  public String ultimaInteraccion;

  public DonanteDTO() {
  }
}
