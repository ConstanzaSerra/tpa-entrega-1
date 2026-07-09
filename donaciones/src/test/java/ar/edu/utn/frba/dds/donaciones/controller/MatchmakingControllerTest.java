package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Categoria;
import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.Subcategoria;
import ar.edu.utn.frba.dds.donaciones.matchmaking.ProcesadorMatchmaking;
import ar.edu.utn.frba.dds.donaciones.matchmaking.PropuestaMatchmaking;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import ar.edu.utn.frba.dds.donaciones.repository.PropuestaRepository;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// Tests de referencia de la exposicion REST del matchmaking (ejecucion a demanda + ranking).
class MatchmakingControllerTest {

  private DonacionRepository donacionRepository;
  private EntidadRepository entidadRepository;
  private PropuestaRepository propuestaRepository;
  private ProcesadorMatchmaking procesador;
  private MatchmakingController controller;
  private Context ctx;

  @BeforeEach
  void setUp() {
    donacionRepository = mock(DonacionRepository.class);
    entidadRepository = mock(EntidadRepository.class);
    propuestaRepository = mock(PropuestaRepository.class);
    procesador = mock(ProcesadorMatchmaking.class);
    controller = new MatchmakingController(donacionRepository, entidadRepository, propuestaRepository, procesador);
    ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
  }

  private Donacion unaDonacion() {
    Subcategoria sub = new Subcategoria("Arroz", Categoria.ALIMENTOS, false, false);
    Donacion donacion = new Donacion(sub, 10, "kg", null, LocalDate.now());
    donacion.setId(1L);
    return donacion;
  }

  // Ejecutar el matchmaking sobre una donacion inexistente responde 404 sin procesar nada
  @Test
  void testEjecutarDonacionInexistente() {
    when(ctx.pathParam("id")).thenReturn("99");
    when(donacionRepository.buscarPorId(99L)).thenReturn(Optional.empty());

    controller.ejecutar(ctx);

    verify(ctx).status(404);
    verify(procesador, never()).procesar(any(), anyList(), anyList());
  }

  // Caso feliz: POST /donaciones/{id}/matchmaking procesa y persiste la propuesta generada
  @Test
  void testEjecutarGuardaLaPropuesta() {
    Donacion donacion = unaDonacion();
    when(ctx.pathParam("id")).thenReturn("1");
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));
    when(donacionRepository.obtenerTodas()).thenReturn(List.of(donacion));
    when(entidadRepository.obtenerTodas()).thenReturn(List.of());

    PropuestaMatchmaking propuesta = mock(PropuestaMatchmaking.class);
    when(propuesta.getDonacion()).thenReturn(donacion);
    when(propuesta.getEntidadesSugeridas()).thenReturn(List.of());
    when(procesador.procesar(any(), anyList(), anyList())).thenReturn(propuesta);

    controller.ejecutar(ctx);

    verify(propuestaRepository).guardar(propuesta);
    verify(ctx).json(any());
  }

  // Pedir el ranking sin haber ejecutado el matchmaking antes responde 404
  @Test
  void testObtenerPropuestaSinMatchmakingPrevio() {
    when(ctx.pathParam("id")).thenReturn("1");
    when(propuestaRepository.buscarPorDonacion(1L)).thenReturn(Optional.empty());

    controller.obtenerPropuesta(ctx);

    verify(ctx).status(404);
  }
}
