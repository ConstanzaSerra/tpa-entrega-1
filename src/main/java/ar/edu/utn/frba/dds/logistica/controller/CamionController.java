package ar.edu.utn.frba.dds.logistica.controller;

import ar.edu.utn.frba.dds.logistica.domain.Camion;
import ar.edu.utn.frba.dds.logistica.dto.CamionDTO;
import ar.edu.utn.frba.dds.logistica.repository.CamionRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Optional;

public class CamionController {
    private final CamionRepository camionRepository;

    public CamionController(CamionRepository camionRepository) {
        this.camionRepository = camionRepository;
    }

    public void getAll(Context ctx) {
        List<CamionDTO> camiones = camionRepository.buscarTodos()
                .stream()
                .map(CamionDTO::fromDomain)
                .toList();
        ctx.json(camiones);
    }

    public void getById(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Camion> camionOpt = camionRepository.buscarPorId(id);
        
        if (camionOpt.isPresent()) {
            ctx.json(CamionDTO.fromDomain(camionOpt.get()));
        } else {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
        }
    }

    public void create(Context ctx) {
        CamionDTO dto = ctx.bodyAsClass(CamionDTO.class);
        Camion nuevoCamion = dto.toDomain();
        Camion guardado = camionRepository.guardar(nuevoCamion);
        ctx.status(HttpStatus.CREATED).json(CamionDTO.fromDomain(guardado));
    }

    public void update(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Camion> camionOpt = camionRepository.buscarPorId(id);
        
        if (camionOpt.isPresent()) {
            CamionDTO dto = ctx.bodyAsClass(CamionDTO.class);
            Camion camionAActualizar = dto.toDomain();
            camionAActualizar.setId(id);
            
            Camion actualizado = camionRepository.actualizar(camionAActualizar);
            ctx.json(CamionDTO.fromDomain(actualizado));
        } else {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
        }
    }

    public void delete(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Camion> camionOpt = camionRepository.buscarPorId(id);
        
        if (camionOpt.isPresent()) {
            camionRepository.eliminar(id);
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
        }
    }
}
