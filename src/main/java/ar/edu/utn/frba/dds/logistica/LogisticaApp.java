package ar.edu.utn.frba.dds.logistica;

import ar.edu.utn.frba.dds.logistica.adaptadores.DonacionesHttpAdapter;
import ar.edu.utn.frba.dds.logistica.adaptadores.PlanificadorHttpAdapter;
import ar.edu.utn.frba.dds.logistica.controller.CamionController;
import ar.edu.utn.frba.dds.logistica.controller.PlanificadorController;
import ar.edu.utn.frba.dds.logistica.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.puertos.PlanificadorExternoAPI;
import ar.edu.utn.frba.dds.logistica.repository.*;
import ar.edu.utn.frba.dds.logistica.service.PlanificacionScheduler;
import ar.edu.utn.frba.dds.logistica.service.PlanificadorRutasService;
import io.javalin.Javalin;

import java.util.concurrent.TimeUnit;

public class LogisticaApp {
    
    public static void main(String[] args) {
        // 1. Inicializar persistencia en memoria
        CamionRepository camionRepository = new InMemoryCamionRepository();
        RutaRepository rutaRepository = new InMemoryRutaRepository();
        EntregaRepository entregaRepository = new InMemoryEntregaRepository();
        
        // 2. Inicializar adaptadores (puertos salientes)
        String donacionesUrl = "http://localhost:8081"; // Harcoded por ahora
        String planificadorUrl = "http://localhost:8082"; // Harcoded por ahora
        
        DonacionesAPI donacionesAPI = new DonacionesHttpAdapter(donacionesUrl);
        PlanificadorExternoAPI planificadorAPI = new PlanificadorHttpAdapter(planificadorUrl);
        
        // 3. Inicializar servicios de dominio
        String callbackUrl = "http://localhost:8080/planificador/callback";
        PlanificadorRutasService planificadorService = new PlanificadorRutasService(
                donacionesAPI, planificadorAPI, camionRepository, callbackUrl);
                
        // 4. Inicializar tareas en background (Scheduler)
        PlanificacionScheduler scheduler = new PlanificacionScheduler(planificadorService);
        // Configurado para correr cada 1 minuto (facilita pruebas manuales)
        scheduler.iniciar(1, 1, TimeUnit.MINUTES);
        
        // 5. Inicializar controladores (puertos entrantes)
        CamionController camionController = new CamionController(camionRepository);
        PlanificadorController planificadorController = new PlanificadorController(
                rutaRepository, entregaRepository, camionRepository);
        
        // 6. Configurar e iniciar Javalin
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(8080);
        
        // Rutas de Camiones (Gestión de Flota)
        app.get("/camiones", camionController::getAll);
        app.get("/camiones/{id}", camionController::getById);
        app.post("/camiones", camionController::create);
        app.put("/camiones/{id}", camionController::update);
        app.delete("/camiones/{id}", camionController::delete);
        
        // Rutas del Planificador
        app.post("/planificador/callback", planificadorController::recibirCallback);
        
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
