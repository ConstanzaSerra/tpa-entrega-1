package ar.edu.utn.frba.dds.logistica.dominio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

@Entity
public class Ruta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne
  private Camion camion;
  
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "ruta_id")
  private List<ParadaDeRuta> paradas;
  
  @Enumerated(EnumType.STRING)
  private EstadoRuta estado;
  
  private String linkMapa;

  public Ruta() {
    this.estado = EstadoRuta.PLANIFICADA;
    this.paradas = new ArrayList<>();
  }

  public Ruta(Camion camion) {
    this();
    this.camion = camion;
  }

  /**
   * Un camion hace muchas rutas a lo largo del tiempo (historial y rutas planificadas en cola),
   * pero solo puede estar recorriendo una a la vez. El camion no conoce sus rutas (la
   * navegabilidad es Ruta -> Camion), por eso quien inicia la ruta pasa las demas rutas
   * conocidas y la ruta verifica que su camion no este ocupado.
   */
  public List<Entrega> iniciar(Collection<Ruta> otrasRutas) {
    if (camion != null && otrasRutas != null) {
      boolean camionOcupado = otrasRutas.stream()
          .filter(otra -> otra != this && (id == null || !id.equals(otra.getId())))
          .anyMatch(otra -> otra.getEstado() == EstadoRuta.EN_CURSO && otra.usaElCamion(camion));
      if (camionOcupado) {
        throw new IllegalStateException("El camion " + camion.getPatente() + " ya tiene otra ruta en curso");
      }
    }
    return iniciar();
  }

  private boolean usaElCamion(Camion otro) {
    if (camion == null || otro == null) {
      return false;
    }
    if (camion == otro) {
      return true;
    }
    return camion.getId() != null && camion.getId().equals(otro.getId());
  }

  public List<Entrega> iniciar() {
    if (estado != EstadoRuta.PLANIFICADA) {
      throw new IllegalStateException("Solo se puede iniciar una ruta PLANIFICADA. Estado: " + estado);
    }
    this.estado = EstadoRuta.EN_CURSO;
    List<Entrega> entregasIniciadas = new ArrayList<>();
    for (ParadaDeRuta parada : this.paradas) {
        for (Entrega entrega : parada.getEntregas()) {
            entrega.iniciarTraslado();
            entregasIniciadas.add(entrega);
        }
    }
    return entregasIniciadas;
  }

  public void completar() {
    if (estado != EstadoRuta.EN_CURSO) {
      throw new IllegalStateException("Solo se puede completar una ruta EN_CURSO. Estado: " + estado);
    }
    this.estado = EstadoRuta.COMPLETADA;
  }

  /**
   * Cada ParadaDeRuta pertenece a una Ruta especifica (composicion Ruta 1 *-- * ParadaDeRuta)
   * porque modela la visita puntual de ese recorrido con las entregas de ese viaje.
   * Dentro de una misma ruta, si se agrega dos veces la misma parada o una segunda parada
   * para la misma entidad beneficiaria, se unifican sus entregas en la parada existente
   * para no persistir dos veces la misma parada en la misma ruta.
   */
  public void agregarParada(ParadaDeRuta parada) {
    if (parada == null || this.paradas.contains(parada)) {
      return;
    }
    if (parada.getEntidadBeneficiariaId() != null) {
      for (ParadaDeRuta existente : this.paradas) {
        if (parada.getEntidadBeneficiariaId().equals(existente.getEntidadBeneficiariaId())) {
          for (Entrega entrega : parada.getEntregas()) {
            existente.agregarEntrega(entrega);
          }
          return;
        }
      }
    }
    this.paradas.add(parada);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Camion getCamion() { return camion; }
  public void setCamion(Camion camion) { this.camion = camion; }
  public List<ParadaDeRuta> getParadas() { return paradas; }
  public void setParadas(List<ParadaDeRuta> paradas) { this.paradas = paradas; }
  public EstadoRuta getEstado() { return estado; }
  public String getLinkMapa() { return linkMapa; }
  public void setLinkMapa(String linkMapa) { this.linkMapa = linkMapa; }
}
