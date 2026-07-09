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

public class PlanificadorRutasService {
    private static final int TAMANIO_LOTE = 100; // restriccion del proveedor por enunciado, max 100 donaciones por ejecucion

    private final DonacionesAPI donacionesAPI;
    private final PlanificadorExternoAPI planificadorExternoAPI;
    private final CamionRepository camionRepository;
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final String callbackUrl;
    private final String linkMapa;

    // Donaciones ya enviadas al planificador cuyo callback todavia no llego.
    // Concurrente porque lo tocan el hilo del scheduler y el hilo HTTP del callback.
    private final Set<Long> donacionesEnPlanificacion = ConcurrentHashMap.newKeySet();

    public PlanificadorRutasService(DonacionesAPI donacionesAPI,
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
        this.linkMapa = linkMapa;
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

        int lotes = (int) Math.ceil((double) aPlanificar.size() / TAMANIO_LOTE); //math.ceil rendondea para arriba

        for (int i = 0; i < lotes; i++) {
            int start = i * TAMANIO_LOTE;
            int end = Math.min(start + TAMANIO_LOTE, aPlanificar.size()); // min(a,b) = a
            List<DonacionParaRutaDTO> loteDonaciones = new ArrayList<>(aPlanificar.subList(start, end)); //me devuelve una sublista que tiene 100 elementos, la unica con menos de 100 va a ser la ultima

            PlanificacionRequestDTO request = new PlanificacionRequestDTO(
                    callbackUrl,
                    loteDonaciones,
                    camionesDTO
            );

            planificadorExternoAPI.solicitarPlanificacion(request); // armo el paquete nomás
            loteDonaciones.forEach(d -> donacionesEnPlanificacion.add(d.donacionId));// por si el scheduler labura de nuevo, no me las mande de vuelta
            System.out.println("Lote " + (i + 1) + " de " + lotes + " enviado al planificador.");
        }
    }

    public void registrarResultado(PlanificacionCallbackDTO callback) { //eventualmente cuando el servicio externo me responda, llamo a este metodo en el controller
        System.out.println("Recibido callback del planificador con " + callback.rutas.size() + " rutas armadas.");

        for (PlanificacionCallbackDTO.RutaArmadaDTO rutaDTO : callback.rutas) {  // el servicio externo me devuelve varias rutas, proceso 1 por 1
            Optional<Camion> camionOpt = camionRepository.buscarPorId(rutaDTO.camionId);
            if (camionOpt.isEmpty()) {
                System.err.println("Camion no encontrado para la id: " + rutaDTO.camionId);
                continue;
            }

            Ruta nuevaRuta = new Ruta(camionOpt.get());
            nuevaRuta.setLinkMapa(linkMapa);

            for (PlanificacionCallbackDTO.ParadaArmadaDTO paradaDTO : rutaDTO.paradas) {
                ParadaDeRuta parada = new ParadaDeRuta(paradaDTO.entidadBeneficiariaId, paradaDTO.direccion);

                for (Long donacionId : paradaDTO.donacionesIds) {
                    Entrega entrega = entregaPendienteExistente(donacionId) //para no duplicar entregas
                            .orElseGet(() -> new Entrega(donacionId, paradaDTO.entidadBeneficiariaId, paradaDTO.direccion));
                    entregaRepository.guardar(entrega);// me la guardo en memoria, eventualmente la voy a tener que cambiar por una base de datos o lo que sea
                    parada.agregarEntrega(entrega);

                    donacionesEnPlanificacion.remove(donacionId);
                    donacionesAPI.informarDonacionPlanificada(donacionId);
                }

                nuevaRuta.agregarParada(parada);
            }

            rutaRepository.guardar(nuevaRuta);
            System.out.println("Ruta generada y guardada para camion ID: " + rutaDTO.camionId);
        }

        if (callback.donacionesNoAsignadas != null && !callback.donacionesNoAsignadas.isEmpty()) {
            // Al soltarlas del set, siguen en ASIGNACION_REALIZADA en Donaciones y el
            // proximo ciclo del scheduler las vuelve a incluir: esa es la replanificacion.
            callback.donacionesNoAsignadas.forEach(donacionesEnPlanificacion::remove);
            System.out.println("Quedaron " + callback.donacionesNoAsignadas.size()
                    + " donaciones sin asignar; se replanificaran en el proximo ciclo.");
        }
    }

    // Donaciones que informa el servicio de Donaciones, excluyendo las que ya estan
    // en planificacion o ya tienen una entrega registrada aca (defensa contra duplicados
    // mientras Donaciones todavia no actualizo su estado).
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

    // Entregas que volvieron al deposito (PENDIENTE sin ruta PLANIFICADA que las contenga):
    // el enunciado permite generarles una nueva asignacion de ruta.
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

    private Optional<Entrega> entregaPendienteExistente(Long donacionId) {
        return entregaRepository.buscarPorDonacionId(donacionId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.PENDIENTE)
                .findFirst();
    }
}
