package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// Tests de referencia del CRUD de entidades beneficiarias.
class EntidadControllerTest {

  private EntidadRepository entidadRepository;
  private EntidadController controller;
  private Context ctx;

  @BeforeEach
  void setUp() {
    entidadRepository = mock(EntidadRepository.class);
    controller = new EntidadController(entidadRepository);
    ctx = mock(Context.class);
    when(ctx.status(anyInt())).thenReturn(ctx);
  }

  private EntidadBeneficiaria unaEntidad() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor", "Calle 1", "222", List.of("comedor@mail.com"), new ArrayList<>());
    entidad.setId(5L);
    return entidad;
  }

  // Caso feliz: POST /entidades crea la entidad y responde 201
  @Test
  void testCrear() {
    EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
    dto.razonSocial = "Comedor";
    dto.direccion = "Calle 1";
    dto.telefono = "222";
    when(ctx.bodyAsClass(EntidadBeneficiariaDTO.class)).thenReturn(dto);

    controller.crear(ctx);

    verify(entidadRepository).guardar(any(EntidadBeneficiaria.class));
    verify(ctx).status(201);
  }

  // Sin razon social o direccion no hay entidad valida: 400 sin guardar
  @Test
  void testCrearIncompletaDevuelve400() {
    EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
    dto.razonSocial = "Comedor"; // falta la direccion
    when(ctx.bodyAsClass(EntidadBeneficiariaDTO.class)).thenReturn(dto);

    controller.crear(ctx);

    verify(ctx).status(400);
    verify(entidadRepository, never()).guardar(any());
  }

  // Para testear que GET /entidades/{id} devuelve la entidad como DTO
  @Test
  void testObtenerPorIdEncontrada() {
    when(ctx.pathParam("id")).thenReturn("5");
    when(entidadRepository.buscarPorId(5L)).thenReturn(Optional.of(unaEntidad()));

    controller.obtenerPorId(ctx);

    ArgumentCaptor<EntidadBeneficiariaDTO> captor = ArgumentCaptor.forClass(EntidadBeneficiariaDTO.class);
    verify(ctx).json(captor.capture());
    assertEquals("Comedor", captor.getValue().razonSocial);
  }

  // Para testear que pedir una entidad inexistente responde 404
  @Test
  void testObtenerPorIdInexistente() {
    when(ctx.pathParam("id")).thenReturn("99");
    when(entidadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

    controller.obtenerPorId(ctx);

    verify(ctx).status(404);
  }
}
