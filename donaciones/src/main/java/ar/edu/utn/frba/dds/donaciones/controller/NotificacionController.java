package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.EstadoDonacion;
import ar.edu.utn.frba.dds.donaciones.domain.Notificable;
import ar.edu.utn.frba.dds.donaciones.dto.EntregaAfectadaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recibe los eventos que invoca Logistica.
 * Cada evento: (1) actualiza la trazabilidad de la donacion y (2) publica un evento de notificacion.
 * El envio real lo resuelven los observadores suscriptos al GestorDeNotificaciones.
 */
public class NotificacionController {

  private final DonacionRepository donacionRepository;
  private final GestorDeNotificaciones gestor;

  public NotificacionController(DonacionRepository donacionRepository, GestorDeNotificaciones gestor) {
    this.donacionRepository = donacionRepository;
    this.gestor = gestor;
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
      notificarPartes(d, "Entrega confirmada. Fecha: " + evento.fechaHora + " | camión: " + evento.patenteCamion);
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
      // La administracion tambien se entera (destinatario definido al vuelo con un lambda Notificable)
      Notificable admin = () -> new Email("admin@donatrack.org");
      gestor.publicar(new EventoDeNotificacion(List.of(admin),
          "Entrega fallida de la donacion " + d.getId() + ": " + evento.motivo));
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
    List<Notificable> destinatarios = new ArrayList<>();
    if (d.getDonante() != null) {
      destinatarios.add(d.getDonante());
    }
    if (d.getEntidadAsignada() != null) {
      destinatarios.add(d.getEntidadAsignada());
    }
    if (!destinatarios.isEmpty()) {
      gestor.publicar(new EventoDeNotificacion(destinatarios, mensaje));
    }
  }
}
