package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.Camion;
import ar.edu.utn.frba.dds.logistica.domain.Entrega;
import ar.edu.utn.frba.dds.logistica.dto.AgregarFotoRequestDTO;
import ar.edu.utn.frba.dds.logistica.dto.ConfirmarEntregaRequestDTO;
import ar.edu.utn.frba.dds.logistica.dto.RechazarEntregaRequestDTO;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.logistica.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.repository.EntregaRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public class EntregaController {
    private final EntregaRepository entregaRepository;
    private final CamionRepository camionRepository;
    private final DonacionesAPI donacionesAPI;

    public EntregaController(EntregaRepository entregaRepository, 
                             CamionRepository camionRepository, 
                             DonacionesAPI donacionesAPI) {
        this.entregaRepository = entregaRepository;
        this.camionRepository = camionRepository;
        this.donacionesAPI = donacionesAPI;
    }

    public void confirmar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        ConfirmarEntregaRequestDTO request = ctx.bodyAsClass(ConfirmarEntregaRequestDTO.class);
        
        Optional<Entrega> entregaOpt = entregaRepository.buscarPorId(id);
        if (entregaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Entrega no encontrada");
            return;
        }
        
        Optional<Camion> camionOpt = camionRepository.buscarPorId(request.camionId);
        if (camionOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
            return;
        }

        Entrega entrega = entregaOpt.get();
        Camion camion = camionOpt.get();
        LocalDateTime fechaHora = request.fechaHora != null ? request.fechaHora : LocalDateTime.now();

        try {
            entrega.confirmar(camion, fechaHora);
            
            // Disparar notificacion asincronica
            EventoEntregaConfirmadaDTO evento = new EventoEntregaConfirmadaDTO(
                    entrega.getId(),
                    entrega.getDonacionId(),
                    camion.getPatente(),
                    fechaHora
            );
            donacionesAPI.notificarEntregaConfirmada(evento);
            
            ctx.status(HttpStatus.OK).result("Entrega confirmada correctamente");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
        }
    }

    public void rechazar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        RechazarEntregaRequestDTO request = ctx.bodyAsClass(RechazarEntregaRequestDTO.class);

        Optional<Entrega> entregaOpt = entregaRepository.buscarPorId(id);
        if (entregaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Entrega no encontrada");
            return;
        }

        Entrega entrega = entregaOpt.get();
        
        try {
            entrega.rechazar();

            // Disparar notificacion asincronica
            EventoEntregaFallidaDTO evento = new EventoEntregaFallidaDTO(
                    entrega.getId(),
                    entrega.getDonacionId(),
                    request.motivo
            );
            donacionesAPI.notificarEntregaFallida(evento);

            ctx.status(HttpStatus.OK).result("Entrega rechazada correctamente");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
        }
    }

    public void retornar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Entrega> entregaOpt = entregaRepository.buscarPorId(id);
        if (entregaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Entrega no encontrada");
            return;
        }

        try {
            entregaOpt.get().retornarADeposito();
            ctx.status(HttpStatus.OK).result("Entrega retornada a deposito");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
        }
    }

    public void fotos(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        AgregarFotoRequestDTO request = ctx.bodyAsClass(AgregarFotoRequestDTO.class);
        
        Optional<Entrega> entregaOpt = entregaRepository.buscarPorId(id);
        if (entregaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Entrega no encontrada");
            return;
        }

        entregaOpt.get().agregarFoto(request.url);
        ctx.status(HttpStatus.OK).result("Foto agregada");
    }

    public void getAll(Context ctx) {
        ctx.json(entregaRepository.buscarTodas());
    }

    public void getById(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Entrega> entregaOpt = entregaRepository.buscarPorId(id);

        if (entregaOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Entrega no encontrada");
        } else {
            ctx.json(entregaOpt.get());
        }
    }
}

