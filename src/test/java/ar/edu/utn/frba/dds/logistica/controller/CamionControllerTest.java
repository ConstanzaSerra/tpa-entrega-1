package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.Camion;
import ar.edu.utn.frba.dds.logistica.dto.CamionDTO;
import ar.edu.utn.frba.dds.logistica.repository.CamionRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CamionControllerTest {

    private CamionRepository camionRepository;
    private CamionController camionController;
    private Context ctx;

    @BeforeEach
    void setUp() {
        camionRepository = mock(CamionRepository.class);
        camionController = new CamionController(camionRepository);
        ctx = mock(Context.class);
    }

    @Test
    void testGetAll() {
        Camion c1 = new Camion("AAA", 1, 1, 1);
        c1.setId(1L);
        Camion c2 = new Camion("BBB", 2, 2, 2);
        c2.setId(2L);
        
        when(camionRepository.buscarTodos()).thenReturn(Arrays.asList(c1, c2));

        camionController.getAll(ctx);

        verify(ctx).json(any(Iterable.class));
    }

    @Test
    void testGetByIdFound() {
        Camion c = new Camion("AAA", 1, 1, 1);
        c.setId(1L);
        
        when(ctx.pathParam("id")).thenReturn("1");
        when(camionRepository.buscarPorId(1L)).thenReturn(Optional.of(c));

        camionController.getById(ctx);

        ArgumentCaptor<CamionDTO> captor = ArgumentCaptor.forClass(CamionDTO.class);
        verify(ctx).json(captor.capture());
        assertEquals("AAA", captor.getValue().patente());
    }

    @Test
    void testGetByIdNotFound() {
        when(ctx.pathParam("id")).thenReturn("99");
        when(camionRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        when(ctx.status(HttpStatus.NOT_FOUND)).thenReturn(ctx);

        camionController.getById(ctx);

        verify(ctx).status(HttpStatus.NOT_FOUND);
        verify(ctx).result("Camion no encontrado");
    }

    @Test
    void testCreate() {
        CamionDTO inputDto = new CamionDTO(null, "CCC", 5, 2, 5000);
        Camion savedCamion = new Camion("CCC", 5, 2, 5000);
        savedCamion.setId(3L);
        
        when(ctx.bodyAsClass(CamionDTO.class)).thenReturn(inputDto);
        when(camionRepository.guardar(any(Camion.class))).thenReturn(savedCamion);
        when(ctx.status(HttpStatus.CREATED)).thenReturn(ctx);

        camionController.create(ctx);

        verify(ctx).status(HttpStatus.CREATED);
        ArgumentCaptor<CamionDTO> captor = ArgumentCaptor.forClass(CamionDTO.class);
        verify(ctx).json(captor.capture());
        assertEquals(3L, captor.getValue().id());
    }
}
