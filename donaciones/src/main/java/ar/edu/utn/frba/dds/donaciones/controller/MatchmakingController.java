package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.PropuestaDTO;
import ar.edu.utn.frba.dds.donaciones.matchmaking.ProcesadorMatchmaking;
import ar.edu.utn.frba.dds.donaciones.matchmaking.PropuestaMatchmaking;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import ar.edu.utn.frba.dds.donaciones.repository.PropuestaRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expone el matchmaking (algoritmos de asignacion) por REST.
 * La logica de los algoritmos vive en el paquete matchmaking; aca solo se orquesta y expone.
 */
public class MatchmakingController {

  private final DonacionRepository donacionRepository;
  private final EntidadRepository entidadRepository;
  private final PropuestaRepository propuestaRepository;
  private final ProcesadorMatchmaking procesador;

  public MatchmakingController(DonacionRepository donacionRepository,
                               EntidadRepository entidadRepository,
                               PropuestaRepository propuestaRepository,
                               ProcesadorMatchmaking procesador) {
    this.donacionRepository = donacionRepository;
    this.entidadRepository = entidadRepository;
    this.propuestaRepository = propuestaRepository;
    this.procesador = procesador;
  }

  // POST /donaciones/{id}/matchmaking  (ejecucion a demanda)
  public void ejecutar(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    var oDonacion = donacionRepository.buscarPorId(id);
    if (oDonacion.isEmpty()) {
      ctx.status(404).json(Map.of("error", "No existe la donacion " + id));
      return;
    }

    List<EntidadBeneficiaria> entidades = entidadRepository.obtenerTodas();
    List<Donacion> historial = donacionRepository.obtenerTodas();

    PropuestaMatchmaking propuesta = procesador.procesar(oDonacion.get(), entidades, historial);
    propuestaRepository.guardar(propuesta);

    ctx.json(aDTO(propuesta));
  }

  // GET /donaciones/{id}/propuesta  (obtencion del ranking)
  public void obtenerPropuesta(Context ctx) {
    Long id = Long.valueOf(ctx.pathParam("id"));
    propuestaRepository.buscarPorDonacion(id).ifPresentOrElse(
        p -> ctx.json(aDTO(p)),
        () -> ctx.status(404).json(Map.of(
            "error", "No hay propuesta para la donacion " + id + ". Ejecutá el matchmaking primero.")));
  }

  // ---------- mapeo ----------

  private PropuestaDTO aDTO(PropuestaMatchmaking p) {
    PropuestaDTO dto = new PropuestaDTO();
    dto.donacionId = p.getDonacion() != null ? p.getDonacion().getId() : null;
    dto.esCoincidenciaExacta = p.isEsCoincidenciaExacta();
    dto.entidadesSugeridas = p.getEntidadesSugeridas().stream()
        .map(this::resumenEntidad)
        .collect(Collectors.toList());
    return dto;
  }

  private EntidadBeneficiariaDTO resumenEntidad(EntidadBeneficiaria e) {
    EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
    dto.id = e.getId();
    dto.razonSocial = e.getRazonSocial();
    dto.direccion = e.getDireccion();
    return dto;
  }
}
