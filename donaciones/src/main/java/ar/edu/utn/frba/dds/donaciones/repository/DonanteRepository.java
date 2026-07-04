package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonanteRepository {

  private static DonanteRepository instancia;
  private final List<PersonaDonante> donantes = new ArrayList<>();
  private Long proximoId = 1L;

  private DonanteRepository() {}

  public static DonanteRepository getInstancia() {
    if (instancia == null) {
      instancia = new DonanteRepository();
    }
    return instancia;
  }

  public void guardar(PersonaDonante donante) {
    if (donante.getId() == null) {
      donante.setId(proximoId++);
    }
    donantes.add(donante);
  }

  public Optional<PersonaDonante> buscarPorId(Long id) {
    return donantes.stream()
        .filter(d -> id.equals(d.getId()))
        .findFirst();
  }

  public Optional<PersonaDonante> buscarPorEmail(String email) {
    return donantes.stream()
        .filter(d -> d.getMedioDeContactoPredeterminado() instanceof Email e
            && e.getValor().equalsIgnoreCase(email))
        .findFirst();
  }

  public List<PersonaDonante> obtenerTodos() {
    return new ArrayList<>(donantes);
  }

  public boolean eliminar(Long id) {
    return donantes.removeIf(d -> id.equals(d.getId()));
  }

  public int cantidadDonantes() {
    return donantes.size();
  }

  // Solo para tests: permite resetear el estado entre pruebas
  public void limpiar() {
    donantes.clear();
  }
}
