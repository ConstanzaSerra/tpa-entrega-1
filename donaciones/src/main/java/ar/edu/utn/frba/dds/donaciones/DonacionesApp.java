package ar.edu.utn.frba.dds.donaciones;

import ar.edu.utn.frba.dds.donaciones.controller.DonacionController;
import ar.edu.utn.frba.dds.donaciones.controller.DonanteController;
import ar.edu.utn.frba.dds.donaciones.controller.EntidadController;
import ar.edu.utn.frba.dds.donaciones.controller.NotificacionController;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import ar.edu.utn.frba.dds.donaciones.service.ServicioDeNotificaciones;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

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
    DonacionController donacionController = new DonacionController(donacionRepository, donanteRepository, entidadRepository);
    DonanteController donanteController = new DonanteController(donanteRepository);
    EntidadController entidadController = new EntidadController(entidadRepository);
    ServicioDeNotificaciones servicioDeNotificaciones = new ServicioDeNotificaciones();
    NotificacionController notificacionController = new NotificacionController(donacionRepository, servicioDeNotificaciones);

    // 3. Servidor (Jackson con soporte de fechas Java 8, igual que Logistica)
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Javalin app = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.jsonMapper(new JavalinJackson(mapper, false));
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

    // Accion de negocio + trazabilidad
    app.post("/donaciones/{id}/asignacion", donacionController::asignar);
    app.get("/donaciones/{id}/historial", donacionController::historial);

    // CRUD de personas donantes
    app.post("/donantes", donanteController::crear);
    app.get("/donantes", donanteController::listar);
    app.get("/donantes/{id}", donanteController::obtenerPorId);
    app.patch("/donantes/{id}", donanteController::actualizar);
    app.delete("/donantes/{id}", donanteController::eliminar);

    // Entidades beneficiarias (por ahora crear y leer)
    app.post("/entidades", entidadController::crear);
    app.get("/entidades", entidadController::listar);
    app.get("/entidades/{id}", entidadController::obtenerPorId);

    // Eventos que invoca Logistica (trazabilidad + notificacion)
    app.post("/notificaciones/inicio-ruta", notificacionController::inicioRuta);
    app.post("/notificaciones/entrega-confirmada", notificacionController::entregaConfirmada);
    app.post("/notificaciones/entrega-fallida", notificacionController::entregaFallida);

    System.out.println("Servidor Donaciones iniciado en http://localhost:" + PUERTO);
  }
}
