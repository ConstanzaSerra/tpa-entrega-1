package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.CamionDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
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
        CamionDTO dto = ctx.bodyAsClass(CamionDTO.class); // convierte el JSON del request a un dto da error si el json está mal formateado
        try {
            Camion nuevoCamion = dto.toDomain(); // crea el objeto en si, en este caso un camion
            Camion guardado = camionRepository.guardar(nuevoCamion); //me lo guardo en memoria
            ctx.status(HttpStatus.CREATED).json(CamionDTO.fromDomain(guardado)); // responde 201 con el camion creado. incluyendo el id asignado
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
        }
    }

    public void update(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Camion> camionOpt = camionRepository.buscarPorId(id);

        if (camionOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Camion no encontrado");
            return;
        }

        CamionDTO dto = ctx.bodyAsClass(CamionDTO.class);
        try {
            Camion camionAActualizar = dto.toDomain();
            camionAActualizar.setId(id);
            Camion actualizado = camionRepository.actualizar(camionAActualizar);
            ctx.json(CamionDTO.fromDomain(actualizado));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
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
