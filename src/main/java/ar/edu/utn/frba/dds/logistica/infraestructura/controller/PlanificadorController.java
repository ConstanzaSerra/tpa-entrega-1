package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionCallbackDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Optional;

public class PlanificadorController {
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final CamionRepository camionRepository;

    public PlanificadorController(RutaRepository rutaRepository,
                                  EntregaRepository entregaRepository,
                                  CamionRepository camionRepository) {
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.camionRepository = camionRepository;
    }

    public void recibirCallback(Context ctx) {
        PlanificacionCallbackDTO callbackData = ctx.bodyAsClass(PlanificacionCallbackDTO.class);
        
        System.out.println("Recibido callback del planificador con " + callbackData.rutas.size() + " rutas armadas.");
        if (callbackData.donacionesNoAsignadas != null && !callbackData.donacionesNoAsignadas.isEmpty()) {
            System.out.println("Atención: Quedaron " + callbackData.donacionesNoAsignadas.size() + " donaciones sin asignar.");
        }

        for (PlanificacionCallbackDTO.RutaArmadaDTO rutaDTO : callbackData.rutas) {
            Optional<Camion> camionOpt = camionRepository.buscarPorId(rutaDTO.camionId);
            if (camionOpt.isEmpty()) {
                System.err.println("Camion no encontrado para la id: " + rutaDTO.camionId);
                continue;
            }

            Ruta nuevaRuta = new Ruta(camionOpt.get());

            for (PlanificacionCallbackDTO.ParadaArmadaDTO paradaDTO : rutaDTO.paradas) {
                ParadaDeRuta parada = new ParadaDeRuta(paradaDTO.entidadBeneficiariaId, paradaDTO.direccion);

                for (Long donacionId : paradaDTO.donacionesIds) {
                    Entrega entrega = new Entrega(donacionId, paradaDTO.entidadBeneficiariaId);
                    entregaRepository.guardar(entrega);
                    parada.agregarEntrega(entrega);
                }

                nuevaRuta.agregarParada(parada);
            }

            rutaRepository.guardar(nuevaRuta);
            System.out.println("Ruta generada y guardada para camion ID: " + rutaDTO.camionId);
        }

        ctx.status(HttpStatus.OK).result("Planificación procesada correctamente.");
    }
}
