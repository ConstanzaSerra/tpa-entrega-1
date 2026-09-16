package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.CamionDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionCallbackDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionRequestDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
// planificadorRutas (sin service) delegar responsabilidades, metodos muy grandes

public class PlanificadorRutas {
    private static final int TAMANIO_LOTE = 100;

    private final DonacionesAPI donacionesAPI;
    private final PlanificadorExternoAPI planificadorExternoAPI;
    private final CamionRepository camionRepository;
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final String callbackUrl;
    private final ConstructorRutasPlanificadas constructorRutas;

    private final Set<Long> donacionesEnPlanificacion = ConcurrentHashMap.newKeySet();

    public PlanificadorRutas(DonacionesAPI donacionesAPI,
                                    PlanificadorExternoAPI planificadorExternoAPI,
                                    CamionRepository camionRepository,
                                    RutaRepository rutaRepository,
                                    EntregaRepository entregaRepository,
                                    String callbackUrl,
                                    String linkMapa) {
        this.donacionesAPI = donacionesAPI;
        this.planificadorExternoAPI = planificadorExternoAPI;
        this.camionRepository = camionRepository;
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.callbackUrl = callbackUrl;
        this.constructorRutas = new ConstructorRutasPlanificadas(camionRepository, rutaRepository, entregaRepository, donacionesAPI, linkMapa);
    }

    public void planificar() {
        System.out.println("Iniciando tarea de planificacion de rutas...");

        List<DonacionParaRutaDTO> aPlanificar = new ArrayList<>();
        aPlanificar.addAll(donacionesNuevasDesdeDonaciones());
        aPlanificar.addAll(entregasReplanificables());

        if (aPlanificar.isEmpty()) {
            System.out.println("No hay donaciones para planificar.");
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

        List<List<DonacionParaRutaDTO>> lotes = CalculadorDeLotes.agrupar(aPlanificar, TAMANIO_LOTE);

        for (int i = 0; i < lotes.size(); i++) {
            List<DonacionParaRutaDTO> lote = lotes.get(i);
            PlanificacionRequestDTO request = new PlanificacionRequestDTO(callbackUrl, lote, camionesDTO);

            planificadorExternoAPI.solicitarPlanificacion(request);
            lote.forEach(d -> donacionesEnPlanificacion.add(d.donacionId));
            System.out.println("Lote " + (i + 1) + " de " + lotes.size() + " enviado al planificador.");
        }
    }

    public void registrarResultado(PlanificacionCallbackDTO callback) {
        System.out.println("Recibido callback del planificador con " + callback.rutas.size() + " rutas armadas.");

        for (PlanificacionCallbackDTO.RutaArmadaDTO rutaDTO : callback.rutas) {
            constructorRutas.procesar(rutaDTO, donacionesEnPlanificacion);
        }

        if (callback.donacionesNoAsignadas != null && !callback.donacionesNoAsignadas.isEmpty()) {
            callback.donacionesNoAsignadas.forEach(donacionesEnPlanificacion::remove);
            System.out.println("Quedaron " + callback.donacionesNoAsignadas.size()
                    + " donaciones sin asignar; se replanificaran en el proximo ciclo.");
        }
    }

    private List<DonacionParaRutaDTO> donacionesNuevasDesdeDonaciones() {
        List<DonacionParaRutaDTO> donaciones = donacionesAPI.obtenerDonacionesListasParaRepartir();
        if (donaciones == null) {
            return List.of();
        }
        return donaciones.stream()
                .filter(d -> !donacionesEnPlanificacion.contains(d.donacionId))
                .filter(d -> entregaRepository.buscarPorDonacionId(d.donacionId).isEmpty())
                .toList();
    }

    private List<DonacionParaRutaDTO> entregasReplanificables() {
        return entregaRepository.buscarPorEstado(EstadoEntrega.PENDIENTE).stream()
                .filter(e -> !donacionesEnPlanificacion.contains(e.getDonacionId()))
                .filter(e -> !perteneceARutaPlanificada(e))
                .map(e -> new DonacionParaRutaDTO(e.getDonacionId(), e.getEntidadBeneficiariaId(), e.getDireccionDestino()))
                .toList();
    }

    private boolean perteneceARutaPlanificada(Entrega entrega) {
        return rutaRepository.buscarTodas().stream()
                .filter(r -> r.getEstado() == EstadoRuta.PLANIFICADA)
                .flatMap(r -> r.getParadas().stream())
                .flatMap(p -> p.getEntregas().stream())
                .anyMatch(e -> e.getId() != null && e.getId().equals(entrega.getId()));
    }
}
    private Optional<Entrega> entregaPendienteExistente(Long donacionId) {
        return entregaRepository.buscarPorDonacionId(donacionId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.PENDIENTE)
                .findFirst();
    }
}
