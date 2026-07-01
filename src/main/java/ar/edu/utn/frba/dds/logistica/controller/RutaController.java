package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.Entrega;
import ar.edu.utn.frba.dds.logistica.domain.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.domain.Ruta;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EntregaAfectadaDTO;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.repository.RutaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RutaController {
    private final RutaRepository rutaRepository;
    private final DonacionesAPI donacionesAPI;

    public RutaController(RutaRepository rutaRepository, DonacionesAPI donacionesAPI) {
        this.rutaRepository = rutaRepository;
        this.donacionesAPI = donacionesAPI;
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

