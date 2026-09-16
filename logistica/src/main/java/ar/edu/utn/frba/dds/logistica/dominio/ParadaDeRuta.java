package ar.edu.utn.frba.dds.logistica.dominio;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

@Entity
public class ParadaDeRuta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  private Long entidadBeneficiariaId;
  private String direccion;
  
  @OneToMany(cascade = CascadeType.ALL)
  private List<Entrega> entregas;

  public ParadaDeRuta() {
    this.entregas = new ArrayList<>();
  }

  public ParadaDeRuta(Long entidadBeneficiariaId, String direccion) {
    this();
    this.entidadBeneficiariaId = entidadBeneficiariaId;
    this.direccion = direccion;
  }

  public void agregarEntrega(Entrega entrega) {
    this.entregas.add(entrega);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getEntidadBeneficiariaId() { return entidadBeneficiariaId; }
  public void setEntidadBeneficiariaId(Long entidadBeneficiariaId) { this.entidadBeneficiariaId = entidadBeneficiariaId; }
  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
  public List<Entrega> getEntregas() { return entregas; }
  public void setEntregas(List<Entrega> entregas) { this.entregas = entregas; }
}
