package ar.edu.utn.frba.dds.logistica;

import ar.edu.utn.frba.dds.logistica.adaptadores.DonacionesHttpAdapter;
import ar.edu.utn.frba.dds.logistica.adaptadores.PlanificadorHttpAdapter;
import ar.edu.utn.frba.dds.logistica.controller.*;
import ar.edu.utn.frba.dds.logistica.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.repository.*;
import ar.edu.utn.frba.dds.logistica.service.PlanificacionScheduler;
import ar.edu.utn.frba.dds.logistica.service.PlanificadorRutasService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.concurrent.TimeUnit;

public class LogisticaApp {
    
    public static void main(String[] args) {
        // 1. Inicializar persistencia en memoria
        CamionRepository camionRepository = new InMemoryCamionRepository();
        RutaRepository rutaRepository = new InMemoryRutaRepository();
        EntregaRepository entregaRepository = new InMemoryEntregaRepository();
        GpsRepository gpsRepository = new InMemoryGpsRepository();
        
        // 2. Inicializar adaptadores (puertos salientes) leídos desde config
        String donacionesUrl = ConfigManager.getProperty("api.donaciones.url", "http://localhost:8081");
        String planificadorUrl = ConfigManager.getProperty("api.planificador.url", "http://localhost:8082");
        
        DonacionesAPI donacionesAPI = new DonacionesHttpAdapter(donacionesUrl);
        PlanificadorExternoAPI planificadorAPI = new PlanificadorHttpAdapter(planificadorUrl);
        
        // 3. Inicializar servicios de dominio
        int port = Integer.parseInt(ConfigManager.getProperty("server.port", "8080"));
        String callbackUrl = "http://localhost:" + port + "/planificador/callback";
        PlanificadorRutasService planificadorService = new PlanificadorRutasService(
                donacionesAPI, planificadorAPI, camionRepository, callbackUrl);
                
        // 4. Inicializar tareas en background (Scheduler)
        PlanificacionScheduler scheduler = new PlanificacionScheduler(planificadorService);
        long periodMinutes = Long.parseLong(ConfigManager.getProperty("scheduler.period.minutes", "1"));
        scheduler.iniciar(periodMinutes, periodMinutes, TimeUnit.MINUTES);
        
        // 5. Inicializar controladores (puertos entrantes)
        CamionController camionController = new CamionController(camionRepository);
        PlanificadorController planificadorController = new PlanificadorController(
                rutaRepository, entregaRepository, camionRepository);
        RutaController rutaController = new RutaController(rutaRepository, donacionesAPI);
        EntregaController entregaController = new EntregaController(entregaRepository, camionRepository, donacionesAPI);
        GpsController gpsController = new GpsController(gpsRepository, camionRepository);
        
        // 6. Configurar e iniciar Javalin
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Para mapear LocalDate/Instant en JSON
        
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(mapper));
        }).start(port);
        
        // Rutas de Camiones (Gestión de Flota)
        app.get("/camiones", camionController::getAll);
        app.get("/camiones/{id}", camionController::getById);
        app.post("/camiones", camionController::create);
        app.put("/camiones/{id}", camionController::update);
        app.delete("/camiones/{id}", camionController::delete);
        
        // Rutas de Planificación de Rutas
        app.get("/rutas", rutaController::getAll);
        app.get("/rutas/{id}", rutaController::getById);
        
        // Rutas de Trazabilidad de Entregas
        app.get("/entregas", entregaController::getAll);
        app.get("/entregas/{id}", entregaController::getById);
        
        // Rutas del Planificador
        app.post("/planificador/callback", planificadorController::recibirCallback);
        
        // Rutas de Trazabilidad (Fases 1 y 2)
        app.post("/rutas/{id}/iniciar", rutaController::iniciarRuta);
        app.post("/entregas/{id}/confirmar", entregaController::confirmar);
        app.post("/entregas/{id}/rechazar", entregaController::rechazar);
        app.post("/entregas/{id}/retornar", entregaController::retornar);
        app.post("/entregas/{id}/fotos", entregaController::fotos);
        
        // Rutas de Monitoreo GPS (Fase 3)
        app.post("/camiones/{id}/posicion", gpsController::reportarPosicion);
        app.get("/dashboard/camiones", gpsController::getDashboard);
        
        // Exception handling general (opcional)
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("Ocurrió un error interno en el servidor: " + e.getMessage());
            e.printStackTrace();
        });
        
        System.out.println("Servidor Logistica iniciado en http://localhost:8080");
        
        // Hook para apagar el scheduler cuando se apaga la app
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::detener));
    }
}
