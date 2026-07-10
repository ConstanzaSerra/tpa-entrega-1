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
import ar.edu.utn.frba.dds.donaciones.dto.AsignarEntidadDTO;
import ar.edu.utn.frba.dds.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// Tests de referencia para los controllers de Donaciones, con el mismo enfoque que en
// Logistica: se mockean los repositorios y el Context de Javalin, y se llama al handler directo.
class DonacionControllerTest {

  private DonacionRepository donacionRepository;
  private DonanteRepository donanteRepository;
  private EntidadRepository entidadRepository;
  private GestorDeNotificaciones gestor;
  private DonacionController controller;
  private Context ctx;

  @BeforeEach
  void setUp() {
    donacionRepository = mock(DonacionRepository.class);
    donanteRepository = mock(DonanteRepository.class);
    entidadRepository = mock(EntidadRepository.class);
    gestor = mock(GestorDeNotificaciones.class);
    controller = new DonacionController(donacionRepository, donanteRepository, entidadRepository, gestor);
    ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx); // permite el encadenado ctx.status(..).json(..)
  }

  private PersonaDonante unDonante() {
    Email email = new Email("ana@mail.com");
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, new Telefono("111")));
    PersonaDonante donante = new PersonaHumana(medios, email, "Ana", "Perez", null, 123, null, null);
    donante.setId(1L);
    return donante;
  }

  private Donacion unaDonacion() {
    Subcategoria sub = new Subcategoria("Arroz", Categoria.ALIMENTOS, false, false);
    Donacion donacion = new Donacion(sub, 10, "kg", unDonante(), LocalDate.now());
    donacion.setId(1L);
    return donacion;
  }

  private EntidadBeneficiaria unaEntidad() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor", "Calle 1", "222", List.of("comedor@mail.com"), new ArrayList<>());
    entidad.setId(5L);
    return entidad;
  }

  // Para testear que GET /donaciones/{id} devuelve la donacion como DTO
  @Test
  void testObtenerPorIdEncontrada() {
    when(ctx.pathParam("id")).thenReturn("1");
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(unaDonacion()));

    controller.obtenerPorId(ctx);

    ArgumentCaptor<DonacionDTO> captor = ArgumentCaptor.forClass(DonacionDTO.class);
    verify(ctx).json(captor.capture());
    assertEquals("EN_DEPOSITO", captor.getValue().estado);
    assertEquals(10, captor.getValue().cantidad);
  }

  // Para testear que pedir una donacion inexistente responde 404
  @Test
  void testObtenerPorIdInexistente() {
    when(ctx.pathParam("id")).thenReturn("99");
    when(donacionRepository.buscarPorId(99L)).thenReturn(Optional.empty());

    controller.obtenerPorId(ctx);

    verify(ctx).status(404);
  }

  // Caso feliz: POST /donaciones crea la donacion y responde 201
  @Test
  void testCrear() {
    DonacionDTO dto = new DonacionDTO();
    dto.donanteId = 1L;
    dto.cantidad = 10;
    dto.unidadMedida = "kg";
    dto.subcategoriaNombre = "Arroz";
    dto.categoria = "ALIMENTOS";
    when(ctx.bodyAsClass(DonacionDTO.class)).thenReturn(dto);
    when(donanteRepository.buscarPorId(1L)).thenReturn(Optional.of(unDonante()));

    controller.crear(ctx);

    verify(donacionRepository).guardar(any(Donacion.class));
    verify(ctx).status(201);
  }

  // No se puede crear una donacion para un donante que no existe; responde 400 sin guardar
  @Test
  void testCrearDonanteInexistenteDevuelve400() {
    DonacionDTO dto = new DonacionDTO();
    dto.donanteId = 99L;
    when(ctx.bodyAsClass(DonacionDTO.class)).thenReturn(dto);
    when(donanteRepository.buscarPorId(99L)).thenReturn(Optional.empty());

    controller.crear(ctx);

    verify(ctx).status(400);
    verify(donacionRepository, never()).guardar(any());
  }

  // Caso feliz: POST /donaciones/{id}/asignacion asigna la entidad y cambia el estado
  @Test
  void testAsignar() {
    Donacion donacion = unaDonacion();
    when(ctx.pathParam("id")).thenReturn("1");
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));
    when(entidadRepository.buscarPorId(5L)).thenReturn(Optional.of(unaEntidad()));
    AsignarEntidadDTO dto = new AsignarEntidadDTO();
    dto.entidadBeneficiariaId = 5L;
    when(ctx.bodyAsClass(AsignarEntidadDTO.class)).thenReturn(dto);

    controller.asignar(ctx);

    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstado());
  }

  // Asignar una donacion que ya no esta EN_DEPOSITO es un conflicto de estado: 409
  @Test
  void testAsignarConEstadoInvalidoDevuelve409() {
    Donacion donacion = unaDonacion();
    donacion.asignarEntidad(unaEntidad()); // ya quedo ASIGNACION_REALIZADA
    when(ctx.pathParam("id")).thenReturn("1");
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));
    when(entidadRepository.buscarPorId(5L)).thenReturn(Optional.of(unaEntidad()));
    AsignarEntidadDTO dto = new AsignarEntidadDTO();
    dto.entidadBeneficiariaId = 5L;
    when(ctx.bodyAsClass(AsignarEntidadDTO.class)).thenReturn(dto);

    controller.asignar(ctx);

    verify(ctx).status(409);
  }

  // La trazabilidad: GET /donaciones/{id}/historial devuelve un cambio por cada transicion
  @Test
  void testHistorial() {
    Donacion donacion = unaDonacion();
    donacion.asignarEntidad(unaEntidad()); // segunda entrada en el historial
    when(ctx.pathParam("id")).thenReturn("1");
    when(donacionRepository.buscarPorId(1L)).thenReturn(Optional.of(donacion));

    controller.historial(ctx);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(ctx).json(captor.capture());
    assertEquals(2, captor.getValue().size());
  }
}
