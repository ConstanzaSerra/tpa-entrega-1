package ar.edu.utn.frba.dds.logistica.repository;

import ar.edu.utn.frba.dds.logistica.domain.Camion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryCamionRepositoryTest {
    private CamionRepository repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new InMemoryCamionRepository();
    }

    @Test
    void testGuardarCamion() {
        Camion camion = new Camion("AB123CD", 10.0, 3.5, 15000);
        Camion guardado = repositorio.guardar(camion);

        assertNotNull(guardado.getId());
        assertEquals("AB123CD", guardado.getPatente());
    }

    @Test
    void testBuscarPorId() {
        Camion camion = new Camion("XYZ789", 15.0, 4.0, 20000);
        Camion guardado = repositorio.guardar(camion);

        Optional<Camion> encontrado = repositorio.buscarPorId(guardado.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(guardado.getId(), encontrado.get().getId());
    }

    @Test
    void testBuscarTodos() {
        repositorio.guardar(new Camion("A", 1, 1, 1));
        repositorio.guardar(new Camion("B", 2, 2, 2));

        List<Camion> camiones = repositorio.buscarTodos();

        assertEquals(2, camiones.size());
    }

    @Test
    void testActualizarCamion() {
        Camion camion = new Camion("A", 1, 1, 1);
        Camion guardado = repositorio.guardar(camion);

        guardado.setPatente("NUEVA");
        Camion actualizado = repositorio.actualizar(guardado);

        assertEquals("NUEVA", actualizado.getPatente());
        
        Optional<Camion> verificado = repositorio.buscarPorId(guardado.getId());
        assertTrue(verificado.isPresent());
        assertEquals("NUEVA", verificado.get().getPatente());
    }

    @Test
    void testEliminarCamion() {
        Camion camion = new Camion("A", 1, 1, 1);
        Camion guardado = repositorio.guardar(camion);

        repositorio.eliminar(guardado.getId());

        Optional<Camion> encontrado = repositorio.buscarPorId(guardado.getId());
        assertFalse(encontrado.isPresent());
    }
}
