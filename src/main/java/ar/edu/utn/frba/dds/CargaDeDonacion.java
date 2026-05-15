package ar.edu.utn.frba.dds;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CargaDeDonacion {
  private String descripcionGeneral;
  private LocalDate fecha;
  private PersonaDonante donante;
  private List<Bien> bienes;

  public CargaDeDonacion(String descripcionGeneral, LocalDate fecha, PersonaDonante donante, List<Bien> bienes) {
    this.descripcionGeneral = descripcionGeneral;
    this.fecha = fecha;
    this.donante = donante;
    this.bienes = bienes;
  }

  public void agregarBien(Bien bien) {
    this.bienes.add(bien);
  }

  public List<Donacion> segmentar() {
    return new ArrayList<Donacion>(); //TODO - implementar
  }
}
