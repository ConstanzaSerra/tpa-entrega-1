package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionCallbackDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionRequestDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.InMemoryEntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.InMemoryRutaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PlanificadorRutasServiceTest {

    private DonacionesAPI donacionesAPI;
    private PlanificadorExternoAPI planificadorExternoAPI;
    private CamionRepository camionRepository;
    private RutaRepository rutaRepository;
    private EntregaRepository entregaRepository;
    private PlanificadorRutasService planificadorService;

    @BeforeEach
    void setUp() {
        donacionesAPI = mock(DonacionesAPI.class);
        planificadorExternoAPI = mock(PlanificadorExternoAPI.class);
        camionRepository = mock(CamionRepository.class);
        // Repos reales en memoria: el ciclo planificar → callback → replanificar necesita estado
        rutaRepository = new InMemoryRutaRepository();
        entregaRepository = new InMemoryEntregaRepository();
        planificadorService = new PlanificadorRutasService(
                donacionesAPI, planificadorExternoAPI, camionRepository,
                rutaRepository, entregaRepository, "http://callback", "http://dashboard"
        );
        when(camionRepository.buscarTodos()).thenReturn(List.of(camionConId(1L)));
        when(camionRepository.buscarPorId(1L)).thenReturn(java.util.Optional.of(camionConId(1L)));
    }

    private Camion camionConId(Long id) {
        Camion camion = new Camion("AAA111", 1, 1, 1);
        camion.setId(id);
        return camion;
    }

    private List<DonacionParaRutaDTO> donaciones(int cantidad) {
        List<DonacionParaRutaDTO> donaciones = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            donaciones.add(new DonacionParaRutaDTO((long) i, 1L, "Direccion"));
        }
        return donaciones;
    }

    private PlanificacionCallbackDTO callbackConRuta(List<Long> asignadas, List<Long> noAsignadas) {
        PlanificacionCallbackDTO callback = new PlanificacionCallbackDTO();
        PlanificacionCallbackDTO.ParadaArmadaDTO parada = new PlanificacionCallbackDTO.ParadaArmadaDTO();
        parada.entidadBeneficiariaId = 1L;
        parada.direccion = "Direccion";
        parada.donacionesIds = asignadas;
        PlanificacionCallbackDTO.RutaArmadaDTO ruta = new PlanificacionCallbackDTO.RutaArmadaDTO();
        ruta.camionId = 1L;
        ruta.paradas = List.of(parada);
        callback.rutas = List.of(ruta);
        callback.donacionesNoAsignadas = noAsignadas;
        return callback;
    }

    // Para testear que con 100 donaciones o menos se envia una sola solicitud al planificador
    @Test
    void testPlanificarConMenosDe100Donaciones_MandaUnSoloLote() {
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones(50));

        planificadorService.planificar();

        ArgumentCaptor<PlanificacionRequestDTO> captor = ArgumentCaptor.forClass(PlanificacionRequestDTO.class);
        verify(planificadorExternoAPI, times(1)).solicitarPlanificacion(captor.capture());

        assertEquals(50, captor.getValue().donaciones.size());
    }

    // Para testear el particionado en lotes de maximo 100 (req. de implementacion 2)
    @Test
    void testPlanificarConMasDe100Donaciones_MandaVariosLotes() {
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones(250));

        planificadorService.planificar();

        ArgumentCaptor<PlanificacionRequestDTO> captor = ArgumentCaptor.forClass(PlanificacionRequestDTO.class);

        // 250 donaciones = 3 lotes (100, 100, 50)
        verify(planificadorExternoAPI, times(3)).solicitarPlanificacion(captor.capture());

        List<PlanificacionRequestDTO> requests = captor.getAllValues();
        assertEquals(100, requests.get(0).donaciones.size());
        assertEquals(100, requests.get(1).donaciones.size());
        assertEquals(50, requests.get(2).donaciones.size());
    }

    // Mientras no llegue el callback, un nuevo ciclo no debe reenviar las mismas donaciones
    @Test
    void testPlanificarDosVecesSinCallback_NoReenviaLasMismasDonaciones() {
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones(10));

        planificadorService.planificar();
        planificadorService.planificar();

        verify(planificadorExternoAPI, times(1)).solicitarPlanificacion(any());
    }

    // El callback crea rutas y entregas, y avisa a Donaciones por cada donacion asignada
    @Test
    void testRegistrarResultado_CreaEntregasEInformaADonaciones() {
        planificadorService.registrarResultado(callbackConRuta(List.of(1L, 2L), List.of()));

        assertEquals(1, rutaRepository.buscarTodas().size());
        assertEquals(2, entregaRepository.buscarTodas().size());
        verify(donacionesAPI).informarDonacionPlanificada(1L);
        verify(donacionesAPI).informarDonacionPlanificada(2L);
    }

    // Una donacion ya asignada (con entrega registrada) no vuelve a planificarse
    // aunque Donaciones todavia la devuelva en el GET
    @Test
    void testDonacionYaAsignada_NoSeVuelveAPlanificar() {
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones(1)); // donacionId 0

        planificadorService.planificar();
        planificadorService.registrarResultado(callbackConRuta(List.of(0L), List.of()));
        planificadorService.planificar(); // Donaciones sigue devolviendo la donacion 0

        verify(planificadorExternoAPI, times(1)).solicitarPlanificacion(any());
    }

    // Las no asignadas del callback se replanifican en el proximo ciclo (req. de implementacion 3)
    @Test
    void testDonacionesNoAsignadas_SeReplanificanEnElProximoCiclo() {
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones(2)); // 0 y 1

        planificadorService.planificar();
        // El planificador asigna la 0 y devuelve la 1 sin asignar
        planificadorService.registrarResultado(callbackConRuta(List.of(0L), List.of(1L)));
        planificadorService.planificar();

        ArgumentCaptor<PlanificacionRequestDTO> captor = ArgumentCaptor.forClass(PlanificacionRequestDTO.class);
        verify(planificadorExternoAPI, times(2)).solicitarPlanificacion(captor.capture());

        List<DonacionParaRutaDTO> segundoEnvio = captor.getAllValues().get(1).donaciones;
        assertEquals(1, segundoEnvio.size());
        assertEquals(1L, segundoEnvio.get(0).donacionId);
    }

    // Una entrega que volvio al deposito (PENDIENTE, ruta ya iniciada) entra en la
    // proxima planificacion y el callback reutiliza la entrega existente sin duplicarla
    @Test
    void testEntregaRetornadaADeposito_SeReplanificaSinDuplicarse() {
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones(1)); // donacionId 0

        planificadorService.planificar();
        planificadorService.registrarResultado(callbackConRuta(List.of(0L), List.of()));

        // La ruta sale, la entrega falla y la donacion vuelve al deposito
        rutaRepository.buscarTodas().get(0).iniciar();
        Entrega entrega = entregaRepository.buscarTodas().get(0);
        entrega.iniciarTraslado();
        entrega.rechazar();
        entrega.retornarADeposito();

        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(List.of());
        planificadorService.planificar();

        ArgumentCaptor<PlanificacionRequestDTO> captor = ArgumentCaptor.forClass(PlanificacionRequestDTO.class);
        verify(planificadorExternoAPI, times(2)).solicitarPlanificacion(captor.capture());
        assertEquals(0L, captor.getAllValues().get(1).donaciones.get(0).donacionId);

        // El nuevo callback reutiliza la entrega PENDIENTE existente: sigue habiendo una sola
        planificadorService.registrarResultado(callbackConRuta(List.of(0L), List.of()));
        assertEquals(1, entregaRepository.buscarTodas().size());
        assertTrue(entregaRepository.buscarTodas().stream()
                .allMatch(e -> e.getEstado() == EstadoEntrega.PENDIENTE));
    }
}
