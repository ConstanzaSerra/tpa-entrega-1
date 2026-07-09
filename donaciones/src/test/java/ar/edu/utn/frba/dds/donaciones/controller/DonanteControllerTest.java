package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.MedioDeContacto;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaDonante;
import ar.edu.utn.frba.dds.donaciones.domain.PersonaHumana;
import ar.edu.utn.frba.dds.donaciones.domain.Telefono;
import ar.edu.utn.frba.dds.donaciones.dto.DonanteDTO;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// Tests de referencia del CRUD de personas donantes (humanas y juridicas).
class DonanteControllerTest {

  private DonanteRepository donanteRepository;
  private DonanteController controller;
  private Context ctx;

  @BeforeEach
  void setUp() {
    donanteRepository = mock(DonanteRepository.class);
    controller = new DonanteController(donanteRepository);
    ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
  }

  private PersonaDonante unDonante() {
    Email email = new Email("ana@mail.com");
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, new Telefono("111")));
    PersonaDonante donante = new PersonaHumana(medios, email, "Ana", "Perez", null, 123, null, null);
    donante.setId(1L);
    return donante;
  }

  // Para testear que GET /donantes devuelve el listado como DTOs
  @Test
  void testListar() {
    when(donanteRepository.obtenerTodos()).thenReturn(List.of(unDonante()));

    controller.listar(ctx);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(ctx).json(captor.capture());
    assertEquals(1, captor.getValue().size());
  }

  // Para testear que pedir un donante inexistente responde 404
  @Test
  void testObtenerPorIdInexistente() {
    when(ctx.pathParam("id")).thenReturn("99");
    when(donanteRepository.buscarPorId(99L)).thenReturn(Optional.empty());

    controller.obtenerPorId(ctx);

    verify(ctx).status(404);
  }

  // Caso feliz: POST /donantes con tipo HUMANA crea una PersonaHumana y responde 201
  @Test
  void testCrearHumana() {
    DonanteDTO dto = new DonanteDTO();
    dto.tipo = "HUMANA";
    dto.nombre = "Ana";
    dto.apellido = "Perez";
    dto.email = "ana@mail.com";
    dto.telefono = "111";
    when(ctx.bodyAsClass(DonanteDTO.class)).thenReturn(dto);

    controller.crear(ctx);

    ArgumentCaptor<PersonaDonante> captor = ArgumentCaptor.forClass(PersonaDonante.class);
    verify(donanteRepository).guardar(captor.capture());
    assertTrue(captor.getValue() instanceof PersonaHumana);
    verify(ctx).status(201);
  }

  // Un tipo que no es HUMANA ni JURIDICA se rechaza con 400 sin guardar
  @Test
  void testCrearTipoInvalidoDevuelve400() {
    DonanteDTO dto = new DonanteDTO();
    dto.tipo = "MARCIANA";
    dto.email = "zork@mail.com";
    dto.telefono = "111";
    when(ctx.bodyAsClass(DonanteDTO.class)).thenReturn(dto);

    controller.crear(ctx);

    verify(ctx).status(400);
    verify(donanteRepository, never()).guardar(any());
  }

  // DELETE /donantes/{id} sobre un donante existente responde 204
  @Test
  void testEliminar() {
    when(ctx.pathParam("id")).thenReturn("1");
    when(donanteRepository.eliminar(1L)).thenReturn(true);

    controller.eliminar(ctx);

    verify(ctx).status(204);
  }
}
