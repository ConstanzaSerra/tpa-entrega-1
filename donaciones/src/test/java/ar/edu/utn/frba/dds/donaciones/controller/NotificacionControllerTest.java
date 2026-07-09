package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Categoria;
import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.domain.EstadoDonacion;
import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaHumana;
import ar.edu.utn.frba.dds.donaciones.domain.Subcategoria;
import ar.edu.utn.frba.dds.donaciones.domain.Telefono;
import ar.edu.utn.frba.dds.donaciones.dto.EntregaAfectadaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.donaciones.dto.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// Tests de referencia de los eventos que Logistica le informa a Donaciones:
// cada evento actualiza la trazabilidad de la donacion y publica una notificacion (Observer).
class NotificacionControllerTest {

  private DonacionRepository donacionRepository;
  private GestorDeNotificaciones gestor;
  private NotificacionController controller;
  private Context ctx;

  @BeforeEach
  void setUp() {
    donacionRepository = mock(DonacionRepository.class);
    gestor = mock(GestorDeNotificaciones.class);
    controller = new NotificacionController(donacionRepository, gestor);
    ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
  }

  private Donacion donacionAsignada() {
    Email email = new Email("ana@mail.com");
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, new Telefono("111")));
    PersonaDonante donante = new PersonaHumana(medios, email, "Ana", "Perez", null, 123, null, null);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor", "Calle 1", "222", List.of("comedor@mail.com"), new ArrayList<>());

    Subcategoria sub = new Subcategoria("Arroz", Categoria.ALIMENTOS, false, false);
    Donacion donacion = new Donacion(sub, 10, "kg", donante, LocalDate.now());
    donacion.setId(1L);
    donacion.asignarEntidad(entidad);
    return donacion;
  }

  private Donacion donacionEnTraslado() {
    Donacion donacion = donacionAsignada();
    donacion.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
    donacion.avanzarHacia(EstadoDonacion.EN_TRASLADO);
    return donacion;
  }

  // inicio-ruta: la donacion pasa a EN_TRASLADO y se avisa a donante y entidad con el link al mapa
  @Test
  void testInicioRuta() {
    Donacion donacion = donacionAsignada();
    donacion.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));

    EventoInicioRutaDTO evento = new EventoInicioRutaDTO();
    evento.linkMapa = "http://localhost:8081/dashboard.html";
    EntregaAfectadaDTO afectada = new EntregaAfectadaDTO();
    afectada.donacionId = 1L;
    evento.entregasAfectadas = List.of(afectada);
    when(ctx.bodyAsClass(EventoInicioRutaDTO.class)).thenReturn(evento);

    controller.inicioRuta(ctx);

    assertEquals(EstadoDonacion.EN_TRASLADO, donacion.getEstado());
    verify(gestor).publicar(any());
    verify(ctx).status(200);
  }

  // entrega-confirmada: la donacion queda ENTREGADA y se notifica a las partes
  @Test
  void testEntregaConfirmada() {
    Donacion donacion = donacionEnTraslado();
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));

    EventoEntregaConfirmadaDTO evento = new EventoEntregaConfirmadaDTO();
    evento.donacionId = 1L;
    evento.patenteCamion = "ABC123";
    evento.fechaHora = LocalDateTime.now();
    when(ctx.bodyAsClass(EventoEntregaConfirmadaDTO.class)).thenReturn(evento);

    controller.entregaConfirmada(ctx);

    assertEquals(EstadoDonacion.ENTREGADA, donacion.getEstado());
    verify(gestor).publicar(any());
  }

  // entrega-fallida: ademas de las partes se notifica a la administracion (dos publicaciones)
  @Test
  void testEntregaFallidaNotificaTambienALaAdministracion() {
    Donacion donacion = donacionEnTraslado();
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));

    EventoEntregaFallidaDTO evento = new EventoEntregaFallidaDTO();
    evento.donacionId = 1L;
    evento.motivo = "entidad ausente";
    when(ctx.bodyAsClass(EventoEntregaFallidaDTO.class)).thenReturn(evento);

    controller.entregaFallida(ctx);

    assertEquals(EstadoDonacion.ENTREGA_FALLIDA, donacion.getEstado());
    verify(gestor, times(2)).publicar(any());
  }
}
