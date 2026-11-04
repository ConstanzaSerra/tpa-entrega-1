package ar.edu.utn.frba.dds.logistica;

import ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores.DonacionesGraphQLAdapter;
import ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores.PlanificadorHttpAdapter;
import ar.edu.utn.frba.dds.logistica.infraestructura.controller.*;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.*;
import ar.edu.utn.frba.dds.logistica.aplicacion.service.PlanificacionScheduler;
import ar.edu.utn.frba.dds.logistica.aplicacion.service.PlanificadorRutas;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogisticaApp {

    public static void main(String[] args) {
        // Persistencia con JPA (PostgreSQL o fallback HSQLDB en memoria)
        CamionRepository camionRepository = new JpaCamionRepository();
        RutaRepository rutaRepository = new JpaRutaRepository();
        EntregaRepository entregaRepository = new JpaEntregaRepository();
        GpsRepository gpsRepository = new JpaGpsRepository();

        // Inicializo adaptadores leidos desde el config / variables de entorno
        String envDonacionesUrl = System.getenv("DONACIONES_URL");
        String donacionesUrl = (envDonacionesUrl != null && !envDonacionesUrl.isBlank())
                ? envDonacionesUrl
                : ConfigManager.getProperty("api.donaciones.url", "http://localhost:8080");
        String planificadorUrl = ConfigManager.getProperty("api.planificador.url", "http://localhost:8082");

        // Entrega 4 (Req. 3b): Comunicacion entre microservicios via GraphQL en reemplazo de HTTP REST
        DonacionesAPI donacionesAPI = new DonacionesGraphQLAdapter(donacionesUrl);
        PlanificadorExternoAPI planificadorAPI = new PlanificadorHttpAdapter(planificadorUrl);

        // Inicializo servicios de dominio
        String envPort = System.getenv("PORT");
        int port = (envPort != null && !envPort.isBlank())
                ? Integer.parseInt(envPort)
                : Integer.parseInt(ConfigManager.getProperty("server.port", "8081"));
        String publicUrl = ConfigManager.getProperty("server.public.url", "http://localhost:" + port);
        String callbackUrl = publicUrl + "/planificador/callback";
        String linkMapa = publicUrl + "/dashboard.html";
        PlanificadorRutas planificadorService = new PlanificadorRutas(
                donacionesAPI, planificadorAPI, camionRepository, rutaRepository, entregaRepository, callbackUrl, linkMapa);

        // Inicializo el Scheduler, corre a las 2 de la mañana
        PlanificacionScheduler scheduler = new PlanificacionScheduler(planificadorService);
        String periodMinutes = ConfigManager.getProperty("scheduler.period.minutes", "");
        if (!periodMinutes.isBlank()) {
            long period = Long.parseLong(periodMinutes);
            scheduler.iniciar(period, period, TimeUnit.MINUTES);
        } else {
            LocalTime horaBajaCarga = LocalTime.parse(ConfigManager.getProperty("scheduler.hora", "02:00"));
            scheduler.iniciarDiario(horaBajaCarga);
        }

        // Inicializo controladores REST + GraphQL (Entrega 4)
        CamionController camionController = new CamionController(camionRepository);
        PlanificadorController planificadorController = new PlanificadorController(planificadorService);
        RutaController rutaController = new RutaController(rutaRepository, camionRepository, entregaRepository, donacionesAPI);
        EntregaController entregaController = new EntregaController(entregaRepository, camionRepository, donacionesAPI);
        GpsController gpsController = new GpsController(gpsRepository, camionRepository, rutaRepository);
        LogisticaGraphQLController graphQLController = new LogisticaGraphQLController(
                camionRepository, rutaRepository, entregaRepository, gpsRepository, donacionesAPI);

        // Configurar el javalin
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(mapper, false));
            config.staticFiles.add("/public");
        }).start(port);

        app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "service", "logistica", "protocol", "GraphQL+REST")));

        // Endpoint GraphQL (Entrega 4 - Requerimiento detallado 3b)
        app.post("/graphql", graphQLController::ejecutar);

        // Rutas de Camiones (Gestión de Flota)
        app.get("/camiones", camionController::getAll);
        app.get("/camiones/{id}", camionController::getById);
        app.post("/camiones", camionController::create);
        app.put("/camiones/{id}", camionController::update);
        app.delete("/camiones/{id}", camionController::delete);

        // Rutas de Planificación de Rutas
        app.get("/rutas", rutaController::getAll);
        app.get("/rutas/{id}", rutaController::getById);
        app.post("/rutas", rutaController::create);
        app.put("/rutas/{id}", rutaController::update);
        app.delete("/rutas/{id}", rutaController::delete);

        // Rutas de Trazabilidad de Entregas
        app.get("/entregas", entregaController::getAll);
        app.get("/entregas/{id}", entregaController::getById);
        app.post("/entregas", entregaController::create);
        app.put("/entregas/{id}", entregaController::update);
        app.delete("/entregas/{id}", entregaController::delete);

        // Rutas del Planificador
        app.post("/planificador/callback", planificadorController::recibirCallback);

        // Rutas de Trazabilidad
        app.post("/rutas/{id}/iniciar", rutaController::iniciarRuta);
        app.post("/entregas/{id}/confirmar", entregaController::confirmar);
        app.post("/entregas/{id}/rechazar", entregaController::rechazar);
        app.post("/entregas/{id}/retornar", entregaController::retornar);
        app.post("/entregas/{id}/fotos", entregaController::fotos);

        // Rutas de Monitoreo GPS
        app.post("/camiones/{id}/posicion", gpsController::reportarPosicion);
        app.get("/dashboard/camiones", gpsController::getDashboard);

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("Ocurrió un error interno en el servidor: " + e.getMessage());
            e.printStackTrace();
        });

        System.out.println("Servidor Logistica iniciado en http://localhost:" + port + " (GraphQL en /graphql)");

        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::detener));
    }
}
