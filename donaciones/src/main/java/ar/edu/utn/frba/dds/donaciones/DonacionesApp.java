package ar.edu.utn.frba.dds.donaciones;

import ar.edu.utn.frba.dds.donaciones.controller.DonacionController;
import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
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

    // TODO temporal: datos de ejemplo para probar el endpoint hasta que exista el CRUD
    cargarDatosDeEjemplo(donacionRepository, entidadRepository);

    // 2. Controllers
    DonacionController donacionController = new DonacionController(donacionRepository);

    // 3. Servidor
    Javalin app = Javalin.create(config -> {
      config.showJavalinBanner = false;
    }).start(PUERTO);

    // 4. Rutas
    app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "service", "donaciones")));

    // Contrato con Logistica: donaciones listas para planificar rutas
    app.get("/donaciones", donacionController::listarPorEstado);

    System.out.println("Servidor Donaciones iniciado en http://localhost:" + PUERTO);
  }

  private static void cargarDatosDeEjemplo(DonacionRepository donacionRepo, EntidadRepository entidadRepo) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor Los Girasoles", "Av. Siempreviva 742, CABA", "011-4444-5555",
        new ArrayList<>(), new ArrayList<>());
    entidadRepo.guardar(entidad);

    Donacion donacion = new Donacion(null, 10, "kg", null, LocalDate.now());
    donacion.asignarEntidad(entidad); // pasa a ASIGNACION_REALIZADA
    donacionRepo.guardar(donacion);
  }
}
