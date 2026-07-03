package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionRequestDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlanificadorRutasServiceTest {

    private DonacionesAPI donacionesAPI;
    private PlanificadorExternoAPI planificadorExternoAPI;
    private CamionRepository camionRepository;
    private PlanificadorRutasService planificadorService;

    @BeforeEach
    void setUp() {
        donacionesAPI = mock(DonacionesAPI.class);
        planificadorExternoAPI = mock(PlanificadorExternoAPI.class);
        camionRepository = mock(CamionRepository.class);
        planificadorService = new PlanificadorRutasService(
                donacionesAPI, planificadorExternoAPI, camionRepository, "http://callback"
        );
    }

    @Test
    void testPlanificarConMenosDe100Donaciones_MandaUnSoloLote() {
        List<DonacionParaRutaDTO> donaciones = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            donaciones.add(new DonacionParaRutaDTO((long) i, 1L, "Direccion"));
        }
        
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones);
        when(camionRepository.buscarTodos()).thenReturn(List.of(new Camion("AAA", 1, 1, 1)));

        planificadorService.planificar();

        ArgumentCaptor<PlanificacionRequestDTO> captor = ArgumentCaptor.forClass(PlanificacionRequestDTO.class);
        verify(planificadorExternoAPI, times(1)).solicitarPlanificacion(captor.capture());
        
        assertEquals(50, captor.getValue().donaciones.size());
    }

    @Test
    void testPlanificarConMasDe100Donaciones_MandaVariosLotes() {
        List<DonacionParaRutaDTO> donaciones = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            donaciones.add(new DonacionParaRutaDTO((long) i, 1L, "Direccion"));
        }
        
        when(donacionesAPI.obtenerDonacionesListasParaRepartir()).thenReturn(donaciones);
        when(camionRepository.buscarTodos()).thenReturn(List.of(new Camion("AAA", 1, 1, 1)));

        planificadorService.planificar();

        ArgumentCaptor<PlanificacionRequestDTO> captor = ArgumentCaptor.forClass(PlanificacionRequestDTO.class);
        
        // 250 donaciones = 3 lotes (100, 100, 50)
        verify(planificadorExternoAPI, times(3)).solicitarPlanificacion(captor.capture());
        
        List<PlanificacionRequestDTO> requests = captor.getAllValues();
        assertEquals(100, requests.get(0).donaciones.size());
        assertEquals(100, requests.get(1).donaciones.size());
        assertEquals(50, requests.get(2).donaciones.size());
    }
}
