package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.RutaRequestDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EntregaAfectadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RutaController {
    private final RutaRepository rutaRepository;
    private final CamionRepository camionRepository;
    private final EntregaRepository entregaRepository;
    private final DonacionesAPI donacionesAPI;

    public RutaController(RutaRepository rutaRepository,
                          CamionRepository camionRepository,
                          EntregaRepository entregaRepository,
                          DonacionesAPI donacionesAPI) {
        this.rutaRepository = rutaRepository;
        this.camionRepository = camionRepository;
        this.entregaRepository = entregaRepository;
        this.donacionesAPI = donacionesAPI;
    }

    public void create(Context ctx) {
        RutaRequestDTO dto = ctx.bodyAsClass(RutaRequestDTO.class);

        if (dto.camionId == null) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Falta camionId");
            return;
        }
        Optional<Camion> camionOpt = camionRepository.buscarPorId(dto.camionId);
        if (camionOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
            return;
        }

        Ruta ruta = new Ruta(camionOpt.get());
        if (dto.paradas != null) {
            for (RutaRequestDTO.ParadaRequestDTO paradaDTO : dto.paradas) {
                ParadaDeRuta parada = new ParadaDeRuta(paradaDTO.entidadBeneficiariaId, paradaDTO.direccion);
                if (paradaDTO.donacionesIds != null) {
                    for (Long donacionId : paradaDTO.donacionesIds) {
                        Entrega entrega = new Entrega(donacionId, paradaDTO.entidadBeneficiariaId, paradaDTO.direccion);
                        entregaRepository.guardar(entrega);
                        parada.agregarEntrega(entrega);
                    }
                }
                ruta.agregarParada(parada);
            }
        }

        Ruta guardada = rutaRepository.guardar(ruta);
        ctx.status(HttpStatus.CREATED).json(guardada);
    }

    public void delete(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Ruta> rutaOpt = rutaRepository.buscarPorId(id);

        if (rutaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Ruta no encontrada");
            return;
        }
        if (rutaOpt.get().getEstado() != EstadoRuta.PLANIFICADA) {
            // Una ruta en curso o completada es historia de la trazabilidad: no se borra
            ctx.status(HttpStatus.BAD_REQUEST).result("Solo se puede eliminar una ruta PLANIFICADA");
            return;
        }

        rutaRepository.eliminar(id);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public void iniciarRuta(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Ruta> rutaOpt = rutaRepository.buscarPorId(id);

        if (rutaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Ruta no encontrada");
            return;
        }

        Ruta ruta = rutaOpt.get();
        
        try {
            ruta.iniciar();
            
            List<EntregaAfectadaDTO> entregasAfectadas = new ArrayList<>();
            
            // Iniciar traslado de cada entrega asociada a la ruta
            for (ParadaDeRuta parada : ruta.getParadas()) {
                for (Entrega entrega : parada.getEntregas()) {
                    entrega.iniciarTraslado();
                    
                    entregasAfectadas.add(new EntregaAfectadaDTO(
                            entrega.getId(),
                            entrega.getDonacionId(),
                            entrega.getEntidadBeneficiariaId()
                    ));
                }
            }

            // Disparar notificacion
            EventoInicioRutaDTO evento = new EventoInicioRutaDTO(
                    ruta.getId(),
                    ruta.getCamion() != null ? ruta.getCamion().getPatente() : "Sin asignar",
                    ruta.getLinkMapa(),
                    entregasAfectadas
            );
            
            donacionesAPI.notificarInicioRuta(evento);
            
            ctx.status(HttpStatus.OK).result("Ruta iniciada correctamente");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
        }
    }

    public void getAll(Context ctx) {
        ctx.json(rutaRepository.buscarTodas());
    }

    public void getById(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Ruta> rutaOpt = rutaRepository.buscarPorId(id);

        if (rutaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Ruta no encontrada");
        } else {
            ctx.json(rutaOpt.get());
        }
    }
}

