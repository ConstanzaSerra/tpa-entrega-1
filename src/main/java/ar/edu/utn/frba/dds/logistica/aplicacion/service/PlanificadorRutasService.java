package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.CamionDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionRequestDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;

import java.util.ArrayList;
import java.util.List;

public class PlanificadorRutasService {
    private final DonacionesAPI donacionesAPI;
    private final PlanificadorExternoAPI planificadorExternoAPI;
    private final CamionRepository camionRepository;
    private final String callbackUrl;

    public PlanificadorRutasService(DonacionesAPI donacionesAPI,
                                    PlanificadorExternoAPI planificadorExternoAPI,
                                    CamionRepository camionRepository,
                                    String callbackUrl) {
        this.donacionesAPI = donacionesAPI;
        this.planificadorExternoAPI = planificadorExternoAPI;
        this.camionRepository = camionRepository;
        this.callbackUrl = callbackUrl;
    }

    public void planificar() {
        System.out.println("Iniciando tarea de planificacion de rutas...");
        
        List<DonacionParaRutaDTO> donacionesListas = donacionesAPI.obtenerDonacionesListasParaRepartir();
        if (donacionesListas == null || donacionesListas.isEmpty()) {
            System.out.println("No hay donaciones listas para repartir.");
            return;
        }

        List<Camion> camiones = camionRepository.buscarTodos();
        if (camiones.isEmpty()) {
            System.out.println("No hay camiones disponibles.");
            return;
        }

        List<CamionDTO> camionesDTO = camiones.stream()
                .map(CamionDTO::fromDomain)
                .toList();

        // En lotes de maximo 100 donaciones
        int lotes = (int) Math.ceil((double) donacionesListas.size() / 100);
        
        for (int i = 0; i < lotes; i++) {
            int start = i * 100;
            int end = Math.min(start + 100, donacionesListas.size());
            List<DonacionParaRutaDTO> loteDonaciones = new ArrayList<>(donacionesListas.subList(start, end));
            
            PlanificacionRequestDTO request = new PlanificacionRequestDTO(
                    callbackUrl,
                    loteDonaciones,
                    camionesDTO
            );
            
            planificadorExternoAPI.solicitarPlanificacion(request);
            System.out.println("Lote " + (i + 1) + " de " + lotes + " enviado al planificador.");
        }
    }
}
