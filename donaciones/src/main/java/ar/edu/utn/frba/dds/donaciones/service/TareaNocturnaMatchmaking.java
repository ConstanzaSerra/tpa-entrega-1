package ar.edu.utn.frba.dds.donaciones.service;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.*;
import ar.edu.utn.frba.dds.donaciones.repository.*;

import java.util.List;

public class TareaNocturnaMatchmaking {
  private DonacionRepository repoDonaciones;
  private EntidadRepository repoEntidades;
  private PropuestaRepository repoPropuestas;
  private ProcesadorMatchmaking procesador;

  public TareaNocturnaMatchmaking(DonacionRepository repoDonaciones,
                                  EntidadRepository repoEntidades,
                                  PropuestaRepository repoPropuestas,
                                  ProcesadorMatchmaking procesador) {
    this.repoDonaciones = repoDonaciones;
    this.repoEntidades = repoEntidades;
    this.repoPropuestas = repoPropuestas;
    this.procesador = procesador;
  }

  public void ejecutar() {
    List<Donacion> pendientes = repoDonaciones.obtenerDonacionesEnDeposito();
    List<EntidadBeneficiaria> todasLasEntidades = repoEntidades.obtenerTodas();
    List<Donacion> historial = repoDonaciones.obtenerTodas();

    for (Donacion donacion : pendientes) {
      try {
        PropuestaMatchmaking fichaResultante = procesador.procesar(donacion, todasLasEntidades, historial);
        repoPropuestas.guardar(fichaResultante);
      } catch (Exception e) {
        System.err.println("Error en donación: " + e.getMessage());
      }
    }
  }
}