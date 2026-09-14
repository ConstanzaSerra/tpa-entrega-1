package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Se persiste porque es la unica duenia de los Bienes: sin ella, Bien quedaria
 * huerfano en el modelo relacional. Ademas deja rastro de lo que el donante
 * cargo antes de que segmentar() lo convierta en Donaciones, que es informacion
 * que no se puede recalcular a partir de las donaciones resultantes.
 */
@Entity
@Table(name = "carga_de_donacion")
public class CargaDeDonacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "descripcion_general")
  private String descripcionGeneral;

  @Column(name = "fecha")
  private LocalDate fecha;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private PersonaDonante donante;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "carga_id")
  private List<Bien> bienes = new ArrayList<>();

  protected CargaDeDonacion() {}

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
    Map<ClaveSegmentacion, Integer> cantidadPorClave = new HashMap<>();

    for (Bien bien : bienes) {
      LocalDate fechaVencimiento = (bien instanceof BienPercibible)
          ? ((BienPercibible) bien).getFechaVencimiento()
          : null;

      ClaveSegmentacion clave = new ClaveSegmentacion(
          bien.getSubcategoria(),
          fechaVencimiento,
          bien.getUnidadDeMedida()
      );
      cantidadPorClave.merge(clave, bien.getCantidad(), Integer::sum);
    }

    return cantidadPorClave.entrySet().stream()
        .map(entry -> new Donacion(
            entry.getKey().subcategoria(),
            entry.getValue(),
            entry.getKey().unidadMedida(),
            this.donante,
            this.fecha
        ))
        .collect(Collectors.toList());
  }

  private record ClaveSegmentacion(Subcategoria subcategoria, LocalDate fechaVencimiento, String unidadMedida) {}

  public Long getId() {
    return id;
  }

  public String getDescripcionGeneral() {
    return descripcionGeneral;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public PersonaDonante getDonante() {
    return donante;
  }

  public List<Bien> getBienes() {
    return bienes;
  }
}
