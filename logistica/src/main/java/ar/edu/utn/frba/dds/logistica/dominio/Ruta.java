package ar.edu.utn.frba.dds.logistica.dominio;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
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

  public void agregarParada(ParadaDeRuta parada) {
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
