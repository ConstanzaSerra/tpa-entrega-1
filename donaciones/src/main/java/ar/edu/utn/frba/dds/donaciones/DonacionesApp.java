package ar.edu.utn.frba.dds.donaciones;

import ar.edu.utn.frba.dds.donaciones.controller.DonacionController;
import ar.edu.utn.frba.dds.donaciones.controller.DonanteController;
import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

/**
 * Punto de arranque del microservicio de Donaciones (composition root).
 * Levanta el servidor HTTP, arma los repositorios y controllers, y registra las rutas REST.
 * Corre en el puerto 8080, que es el que espera el microservicio de Logistica.
 */
public class DonacionesApp {

  private static final int PUERTO = 8080;

  public static void main(String[] args) {
    // 1. Persistencia en memoria
    DonacionRepository donacionRepository = new DonacionRepository();
    EntidadRepository entidadRepository = new EntidadRepository();
    DonanteRepository donanteRepository = DonanteRepository.getInstancia();

    // 2. Controllers
    DonacionController donacionController = new DonacionController(donacionRepository, donanteRepository);
    DonanteController donanteController = new DonanteController(donanteRepository);

    // 3. Servidor
    Javalin app = Javalin.create(config -> {
      config.showJavalinBanner = false;
    }).start(PUERTO);

    // 4. Rutas
    app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "service", "donaciones")));

    // Contrato con Logistica: donaciones listas para planificar rutas
    app.get("/donaciones", donacionController::listarPorEstado);

    // CRUD de donaciones (parte 1: crear y leer)
    app.post("/donaciones", donacionController::crear);
    app.get("/donaciones/{id}", donacionController::obtenerPorId);
    app.patch("/donaciones/{id}", donacionController::actualizar);
    app.delete("/donaciones/{id}", donacionController::eliminar);

    // CRUD de personas donantes
    app.post("/donantes", donanteController::crear);
    app.get("/donantes", donanteController::listar);
    app.get("/donantes/{id}", donanteController::obtenerPorId);
    app.patch("/donantes/{id}", donanteController::actualizar);
    app.delete("/donantes/{id}", donanteController::eliminar);

    System.out.println("Servidor Donaciones iniciado en http://localhost:" + PUERTO);
  }
}
