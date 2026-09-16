package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionCallbackDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;

import java.util.Optional;
import java.util.Set;

public class ConstructorRutasPlanificadas {
    
    private final CamionRepository camionRepository;
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final DonacionesAPI donacionesAPI;
    private final String linkMapa;

    public ConstructorRutasPlanificadas(CamionRepository camionRepository, RutaRepository rutaRepository, EntregaRepository entregaRepository, DonacionesAPI donacionesAPI, String linkMapa) {
        this.camionRepository = camionRepository;
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.donacionesAPI = donacionesAPI;
        this.linkMapa = linkMapa;
    }

    public void procesar(PlanificacionCallbackDTO.RutaArmadaDTO rutaDTO, Set<Long> donacionesEnPlanificacion) {
        Optional<Camion> camionOpt = camionRepository.buscarPorId(rutaDTO.camionId);
        if (camionOpt.isEmpty()) {
            System.err.println("Camion no encontrado para la id: " + rutaDTO.camionId);
            return;
        }

        Ruta nuevaRuta = new Ruta(camionOpt.get());
        nuevaRuta.setLinkMapa(linkMapa);

        for (PlanificacionCallbackDTO.ParadaArmadaDTO paradaDTO : rutaDTO.paradas) {
            ParadaDeRuta parada = new ParadaDeRuta(paradaDTO.entidadBeneficiariaId, paradaDTO.direccion);

            for (Long donacionId : paradaDTO.donacionesIds) {
                Entrega entrega = entregaPendienteExistente(donacionId)
                        .orElseGet(() -> new Entrega(donacionId, paradaDTO.entidadBeneficiariaId, paradaDTO.direccion));
                entregaRepository.guardar(entrega);
                parada.agregarEntrega(entrega);

                donacionesEnPlanificacion.remove(donacionId);
                donacionesAPI.informarDonacionPlanificada(donacionId);
            }

            nuevaRuta.agregarParada(parada);
        }

        rutaRepository.guardar(nuevaRuta);
        System.out.println("Ruta generada y guardada para camion ID: " + rutaDTO.camionId);
    }
    
    private Optional<Entrega> entregaPendienteExistente(Long donacionId) {
        return entregaRepository.buscarPorDonacionId(donacionId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.PENDIENTE)
                .findFirst();
    }
}
