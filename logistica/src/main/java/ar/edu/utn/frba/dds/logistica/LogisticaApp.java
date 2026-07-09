package ar.edu.utn.frba.dds.logistica;

import ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores.DonacionesHttpAdapter;
import ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores.PlanificadorHttpAdapter;
import ar.edu.utn.frba.dds.logistica.infraestructura.controller.*;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.*;
import ar.edu.utn.frba.dds.logistica.aplicacion.service.PlanificacionScheduler;
import ar.edu.utn.frba.dds.logistica.aplicacion.service.PlanificadorRutasService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class LogisticaApp {
    
    public static void main(String[] args) {
        // persistencia en memoria
        CamionRepository camionRepository = new InMemoryCamionRepository();
        RutaRepository rutaRepository = new InMemoryRutaRepository();
        EntregaRepository entregaRepository = new InMemoryEntregaRepository();
        GpsRepository gpsRepository = new InMemoryGpsRepository();
        
        // Inicializo adaptadores leidos desde el config
        String donacionesUrl = ConfigManager.getProperty("api.donaciones.url", "http://localhost:8080");
        String planificadorUrl = ConfigManager.getProperty("api.planificador.url", "http://localhost:8082");

        DonacionesAPI donacionesAPI = new DonacionesHttpAdapter(donacionesUrl);
        PlanificadorExternoAPI planificadorAPI = new PlanificadorHttpAdapter(planificadorUrl);

        //  Inicializo servicios de dominio
        int port = Integer.parseInt(ConfigManager.getProperty("server.port", "8081"));
        String publicUrl = ConfigManager.getProperty("server.public.url", "http://localhost:" + port);
        String callbackUrl = publicUrl + "/planificador/callback";
        // Enlace al mapa interactivo que viaja en la notificacion de inicio de ruta
        String linkMapa = publicUrl + "/dashboard.html";
        PlanificadorRutasService planificadorService = new PlanificadorRutasService(
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

        // Inicializo controladores
        CamionController camionController = new CamionController(camionRepository);
        PlanificadorController planificadorController = new PlanificadorController(planificadorService);
        RutaController rutaController = new RutaController(rutaRepository, camionRepository, entregaRepository, donacionesAPI);
        EntregaController entregaController = new EntregaController(entregaRepository, camionRepository, donacionesAPI);
        GpsController gpsController = new GpsController(gpsRepository, camionRepository, rutaRepository);
        
        // Configurar el javalin
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(mapper, false));
            // Dashboard de monitoreo
            config.staticFiles.add("/public");
        }).start(port);

        //"comandos" del servidor

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
        
        // Rutas de Monitoreo GPS (Fase 3)
        app.post("/camiones/{id}/posicion", gpsController::reportarPosicion);
        app.get("/dashboard/camiones", gpsController::getDashboard);


        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("Ocurrió un error interno en el servidor: " + e.getMessage());
            e.printStackTrace();
        });
        
        System.out.println("Servidor Logistica iniciado en http://localhost:" + port);
        
        //apagar el scheduler cuando se apaga la app
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::detener));
    }
}
