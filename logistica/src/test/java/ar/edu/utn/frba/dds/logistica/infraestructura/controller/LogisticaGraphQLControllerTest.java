package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.GpsRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.InMemoryCamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.InMemoryEntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.InMemoryGpsRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.InMemoryRutaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogisticaGraphQLControllerTest {

  private CamionRepository camionRepo;
  private EntregaRepository entregaRepo;
  private RutaRepository rutaRepo;
  private GpsRepository gpsRepo;
  private DonacionesAPI donacionesAPI;
  private LogisticaGraphQLController controller;

  @BeforeEach
  void setUp() {
    camionRepo = new InMemoryCamionRepository();
    entregaRepo = new InMemoryEntregaRepository();
    rutaRepo = new InMemoryRutaRepository();
    gpsRepo = new InMemoryGpsRepository();
    donacionesAPI = mock(DonacionesAPI.class);
    controller = new LogisticaGraphQLController(camionRepo, rutaRepo, entregaRepo, gpsRepo, donacionesAPI);
  }

  @Test
  void mutationCrearCamionYQueryCamionesViaGraphQL() {
    Context ctxMut = mock(Context.class);
    when(ctxMut.bodyAsClass(Map.class)).thenReturn(Map.of(
        "query", "mutation { crearCamion(patente: \"AG 777 ZZ\", volumenM3: 30.0, alturaM: 3.2, capacidadKg: 6000.0) { id patente capacidadKg } }"
    ));

    controller.ejecutar(ctxMut);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(ctxMut).json(captor.capture());

    Map<String, Object> response = captor.getValue();
    assertNotNull(response.get("data"));
    assertEquals(1, camionRepo.buscarTodos().size());
    Camion guardado = camionRepo.buscarTodos().get(0);
    assertEquals("AG 777 ZZ", guardado.getPatente());
  }
}
