package ar.edu.utn.frba.dds.logistica.domain;

public class Camion {
  private Long id;
  private String patente;
  private double volumenM3;
  private double alturaM;
  private double capacidadKg;

  public Camion() {}

  public Camion(String patente, double volumenM3, double alturaM, double capacidadKg) {
    this.patente = patente;
    this.volumenM3 = volumenM3;
    this.alturaM = alturaM;
    this.capacidadKg = capacidadKg;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPatente() { return patente; }
  public void setPatente(String patente) { this.patente = patente; }
  public double getVolumenM3() { return volumenM3; }
  public void setVolumenM3(double volumenM3) { this.volumenM3 = volumenM3; }
  public double getAlturaM() { return alturaM; }
  public void setAlturaM(double alturaM) { this.alturaM = alturaM; }
  public double getCapacidadKg() { return capacidadKg; }
  public void setCapacidadKg(double capacidadKg) { this.capacidadKg = capacidadKg; }
}
