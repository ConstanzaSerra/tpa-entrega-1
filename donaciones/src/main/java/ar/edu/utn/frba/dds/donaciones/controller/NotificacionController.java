package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.EstadoDonacion;
import ar.edu.utn.frba.dds.donaciones.dto.EntregaAfectadaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.service.ServicioDeNotificaciones;
import io.javalin.http.Context;

import java.util.Map;

/**
 * Recibe los eventos que invoca Logistica.
 * Cada evento: (1) actualiza la trazabilidad de la donacion y (2) dispara la notificacion.
 * Devuelve 200 para acusar recibo (Logistica los envia de forma asincrona).
 */
public class NotificacionController {

  private final DonacionRepository donacionRepository;
  private final ServicioDeNotificaciones notificaciones;

  public NotificacionController(DonacionRepository donacionRepository, ServicioDeNotificaciones notificaciones) {
    this.donacionRepository = donacionRepository;
    this.notificaciones = notificaciones;
  }

  // POST /notificaciones/inicio-ruta
  public void inicioRuta(Context ctx) {
    EventoInicioRutaDTO evento = ctx.bodyAsClass(EventoInicioRutaDTO.class);
    if (evento.entregasAfectadas != null) {
      for (EntregaAfectadaDTO entrega : evento.entregasAfectadas) {
        donacionRepository.buscarPorId(entrega.donacionId).ifPresent(d -> {
          transicionar(d, EstadoDonacion.EN_TRASLADO);
          notificarPartes(d, "Tu entrega inició su recorrido. Seguila en el mapa: " + evento.linkMapa);
        });
      }
    }
    ctx.status(200).json(Map.of("status", "ok"));
  }

  // POST /notificaciones/entrega-confirmada
  public void entregaConfirmada(Context ctx) {
    EventoEntregaConfirmadaDTO evento = ctx.bodyAsClass(EventoEntregaConfirmadaDTO.class);
    donacionRepository.buscarPorId(evento.donacionId).ifPresent(d -> {
      transicionar(d, EstadoDonacion.ENTREGADA);
      String comprobante = "Entrega confirmada. Fecha: " + evento.fechaHora + " | camión: " + evento.patenteCamion;
      notificarPartes(d, comprobante);
    });
    ctx.status(200).json(Map.of("status", "ok"));
  }

  // POST /notificaciones/entrega-fallida
  public void entregaFallida(Context ctx) {
    EventoEntregaFallidaDTO evento = ctx.bodyAsClass(EventoEntregaFallidaDTO.class);
    donacionRepository.buscarPorId(evento.donacionId).ifPresent(d -> {
      try {
        d.marcarEntregaFallida(evento.motivo);
      } catch (IllegalStateException e) {
        System.err.println("No se pudo marcar fallida la donacion " + d.getId() + ": " + e.getMessage());
      }
      notificarPartes(d, "La entrega no pudo concretarse. Motivo: " + evento.motivo);
      notificaciones.notificar("administracion", "Entrega fallida de la donacion " + d.getId() + ": " + evento.motivo);
    });
    ctx.status(200).json(Map.of("status", "ok"));
  }

  // ---- helpers ----

  private void transicionar(Donacion d, EstadoDonacion objetivo) {
    try {
      d.avanzarHacia(objetivo);
    } catch (IllegalStateException e) {
      System.err.println("Transicion invalida en donacion " + d.getId() + ": " + e.getMessage());
    }
  }

  private void notificarPartes(Donacion d, String mensaje) {
    if (d.getDonante() != null) {
      notificaciones.notificar("donante#" + d.getDonante().getId(), mensaje);
    }
    if (d.getEntidadAsignada() != null) {
      notificaciones.notificar("entidad#" + d.getEntidadAsignada().getId(), mensaje);
    }
  }
}
