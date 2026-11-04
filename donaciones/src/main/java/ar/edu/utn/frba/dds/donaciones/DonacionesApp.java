package ar.edu.utn.frba.dds.donaciones;

import ar.edu.utn.frba.dds.donaciones.adaptadores.LogisticaGraphQLAdapter;
import ar.edu.utn.frba.dds.donaciones.controller.DonacionController;
import ar.edu.utn.frba.dds.donaciones.controller.DonacionesGraphQLController;
import ar.edu.utn.frba.dds.donaciones.controller.DonanteController;
import ar.edu.utn.frba.dds.donaciones.controller.EntidadController;
import ar.edu.utn.frba.dds.donaciones.controller.MatchmakingController;
import ar.edu.utn.frba.dds.donaciones.controller.NecesidadController;
import ar.edu.utn.frba.dds.donaciones.controller.NotificacionController;
import ar.edu.utn.frba.dds.donaciones.matchmaking.ProcesadorMatchmaking;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EnviadorPorMedioPreferido;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.notificaciones.RegistradorNotificacionesWeb;
import ar.edu.utn.frba.dds.donaciones.puertos.LogisticaPort;
import ar.edu.utn.frba.dds.donaciones.repository.CargaDonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import ar.edu.utn.frba.dds.donaciones.repository.NotificacionWebRepository;
import ar.edu.utn.frba.dds.donaciones.repository.PropuestaRepository;
import ar.edu.utn.frba.dds.donaciones.repository.SubcategoriaRepository;
import ar.edu.utn.frba.dds.donaciones.repository.UsuarioRepository;
import ar.edu.utn.frba.dds.donaciones.service.TareaDeInactividad;
import ar.edu.utn.frba.dds.donaciones.service.TareaNocturnaMatchmaking;
import ar.edu.utn.frba.dds.donaciones.web.DatosInicialesSeeder;
import ar.edu.utn.frba.dds.donaciones.web.HandlebarsTemplateEngine;
import ar.edu.utn.frba.dds.donaciones.web.SesionManager;
import ar.edu.utn.frba.dds.donaciones.web.WebPortalController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.Map;

/**
 * Punto de arranque del microservicio principal de Donaciones + Cliente Liviano MVC (Entrega 4).
 * Levanta el servidor HTTP con soporte de plantillas Handlebars, autenticación por Cookies HTTP,
 * API GraphQL (/graphql) y API REST.
 */
public class DonacionesApp {

  private static final int PUERTO_POR_DEFECTO = 8080;

  public static void main(String[] args) {
    // 1. Repositorios JPA (PostgreSQL o fallback HSQLDB en memoria)
    DonacionRepository donacionRepository = new DonacionRepository();
    EntidadRepository entidadRepository = new EntidadRepository();
    DonanteRepository donanteRepository = DonanteRepository.getInstancia();
    SubcategoriaRepository subcategoriaRepository = new SubcategoriaRepository();
    PropuestaRepository propuestaRepository = new PropuestaRepository();
    CargaDonacionRepository cargaDonacionRepository = new CargaDonacionRepository();
    UsuarioRepository usuarioRepository = new UsuarioRepository();
    NotificacionWebRepository notificacionWebRepository = new NotificacionWebRepository();
    ProcesadorMatchmaking procesadorMatchmaking = new ProcesadorMatchmaking();

    // Gestor de notificaciones (Observer) con enviador externo + registrador de bandeja web
    GestorDeNotificaciones gestorNotificaciones = new GestorDeNotificaciones();
    gestorNotificaciones.suscribir(new EnviadorPorMedioPreferido());
    gestorNotificaciones.suscribir(new RegistradorNotificacionesWeb(notificacionWebRepository));

    // Seed de datos iniciales para demostración completa de la Entrega 4
    DatosInicialesSeeder.inicializarSiVacio(
        subcategoriaRepository, donanteRepository, entidadRepository,
        donacionRepository, usuarioRepository, gestorNotificaciones
    );

    // Adaptador GraphQL hacia el servicio de Logística (Entrega 4 - Req. 3b)
    String logisticaUrl = System.getenv("LOGISTICA_URL");
    if (logisticaUrl == null || logisticaUrl.isBlank()) {
      logisticaUrl = "http://localhost:8081";
    }
    LogisticaPort logisticaPort = new LogisticaGraphQLAdapter(logisticaUrl);
    SesionManager sesionManager = new SesionManager(usuarioRepository);

    // 2. Controllers API REST, GraphQL y Cliente Liviano MVC
    DonacionController donacionController = new DonacionController(donacionRepository, donanteRepository, entidadRepository, gestorNotificaciones);
    DonanteController donanteController = new DonanteController(donanteRepository);
    EntidadController entidadController = new EntidadController(entidadRepository);
    NecesidadController necesidadController = new NecesidadController(entidadRepository);
    NotificacionController notificacionController = new NotificacionController(donacionRepository, gestorNotificaciones);
    MatchmakingController matchmakingController = new MatchmakingController(donacionRepository, entidadRepository, propuestaRepository, procesadorMatchmaking);
    DonacionesGraphQLController graphQLController = new DonacionesGraphQLController(donacionRepository, gestorNotificaciones);

    WebPortalController webController = new WebPortalController(
        donacionRepository, donanteRepository, entidadRepository, subcategoriaRepository,
        propuestaRepository, cargaDonacionRepository, usuarioRepository, notificacionWebRepository,
        procesadorMatchmaking, gestorNotificaciones, logisticaPort, sesionManager
    );

    // Tareas calendarizadas
    TareaDeInactividad tareaInactividad = new TareaDeInactividad(donanteRepository, gestorNotificaciones);
    TareaNocturnaMatchmaking tareaNocturnaMatchmaking =
        new TareaNocturnaMatchmaking(donacionRepository, entidadRepository, propuestaRepository, procesadorMatchmaking);

    // 3. Servidor Javalin + Handlebars Template Engine
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    String envPort = System.getenv("PORT");
    int puerto = (envPort != null && !envPort.isBlank()) ? Integer.parseInt(envPort) : PUERTO_POR_DEFECTO;

    Javalin app = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.jsonMapper(new JavalinJackson(mapper, false));
      config.fileRenderer(new HandlebarsTemplateEngine());
    }).start(puerto);

    // 4. Rutas del Cliente Liviano MVC (Entrega 4 - Pantallas 1 a 17)
    // Público (1–4)
    app.get("/", webController::landing);
    app.get("/galeria", webController::galeriaFotos);
    app.get("/mapa-entregas", webController::mapaEntregasPublico);
    app.get("/legal", webController::legalPrivacidad);
    app.get("/registro", webController::mostrarRegistro);
    app.post("/registro/donante", webController::registrarDonante);
    app.post("/registro/entidad", webController::registrarEntidad);
    app.get("/login", webController::mostrarLogin);
    app.post("/login", webController::procesarLogin);
    app.post("/logout", webController::logout);
    app.get("/logout", webController::logout);

    // Persona Donante (5–7)
    app.get("/donante/donaciones", webController::donanteMisDonaciones);
    app.get("/donante/entidades", webController::donanteEntidades);
    app.get("/donante/seguimiento", webController::donanteSeguimientoMapa);

    // Entidad Beneficiaria (8–12)
    app.get("/entidad/necesidades", webController::entidadNecesidades);
    app.post("/entidad/necesidades", webController::entidadCrearNecesidad);
    app.get("/entidad/donaciones", webController::entidadDonaciones);
    app.post("/entidad/donaciones/{id}/confirmar", webController::entidadConfirmarRecepcion);
    app.get("/entidad/notificaciones", webController::entidadNotificaciones);
    app.get("/entidad/seguimiento", webController::entidadSeguimientoMapa);

    // Persona Administradora (13–17)
    app.get("/admin/deposito", webController::adminDeposito);
    app.post("/admin/donantes", webController::adminCrearDonante);
    app.post("/admin/donaciones", webController::adminRegistrarCargaDonacion);
    app.post("/admin/donaciones/{id}/vencer", webController::adminMarcarVencida);
    app.get("/admin/matchmaking", webController::adminMatchmaking);
    app.post("/admin/donaciones/{id}/matchmaking", webController::adminEjecutarMatchmaking);
    app.post("/admin/donaciones/{id}/asignar", webController::adminAsignarEntidad);
    app.get("/admin/camiones", webController::adminCamiones);
    app.post("/admin/camiones", webController::adminCrearCamion);
    app.post("/admin/camiones/{id}/eliminar", webController::adminEliminarCamion);
    app.post("/admin/rutas", webController::adminCrearRuta);
    app.post("/admin/rutas/{id}/iniciar", webController::adminIniciarRuta);
    app.post("/admin/camiones/{id}/posicion", webController::adminActualizarGps);
    app.get("/admin/importar-csv", webController::adminImportarCsvVista);
    app.post("/admin/importar-csv", webController::adminProcesarCsv);
    app.get("/admin/importar-csv/ejemplo-10000.csv", webController::adminDescargarCsvEjemplo10k);

    // 5. Endpoint GraphQL (Entrega 4 - Requerimiento detallado 3b)
    app.post("/graphql", graphQLController::ejecutar);

    // 6. Endpoints API REST existentes (Entregas 2 y 3)
    app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "service", "donaciones", "protocol", "GraphQL+REST+MVC")));

    app.get("/donaciones", donacionController::listarPorEstado);
    app.post("/donaciones", donacionController::crear);
    app.get("/donaciones/{id}", donacionController::obtenerPorId);
    app.patch("/donaciones/{id}", donacionController::actualizar);
    app.delete("/donaciones/{id}", donacionController::eliminar);

    app.post("/donaciones/{id}/asignacion", donacionController::asignar);
    app.get("/donaciones/{id}/historial", donacionController::historial);

    app.post("/donaciones/{id}/matchmaking", matchmakingController::ejecutar);
    app.get("/donaciones/{id}/propuesta", matchmakingController::obtenerPropuesta);

    app.post("/donantes", donanteController::crear);
    app.get("/donantes", donanteController::listar);
    app.get("/donantes/{id}", donanteController::obtenerPorId);
    app.patch("/donantes/{id}", donanteController::actualizar);
    app.delete("/donantes/{id}", donanteController::eliminar);

    app.post("/entidades", entidadController::crear);
    app.get("/entidades", entidadController::listar);
    app.get("/entidades/{id}", entidadController::obtenerPorId);
    app.patch("/entidades/{id}", entidadController::actualizar);
    app.delete("/entidades/{id}", entidadController::eliminar);

    app.post("/entidades/{id}/necesidades", necesidadController::crear);
    app.get("/entidades/{id}/necesidades", necesidadController::listar);
    app.patch("/entidades/{id}/necesidades/{nid}", necesidadController::actualizar);
    app.delete("/entidades/{id}/necesidades/{nid}", necesidadController::eliminar);

    app.post("/notificaciones/inicio-ruta", notificacionController::inicioRuta);
    app.post("/notificaciones/entrega-confirmada", notificacionController::entregaConfirmada);
    app.post("/notificaciones/entrega-fallida", notificacionController::entregaFallida);

    app.post("/tareas/inactividad", ctx -> {
      tareaInactividad.ejecutar();
      ctx.json(Map.of("status", "ejecutado"));
    });

    tareaInactividad.iniciarDiario();
    tareaNocturnaMatchmaking.iniciarShedulerProduccion();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      tareaInactividad.detener();
      tareaNocturnaMatchmaking.detener();
    }));

    System.out.println("Servidor Donaciones + Cliente Liviano Web iniciado en http://localhost:" + puerto);
  }
}
