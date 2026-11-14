package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.service.TareaDeInactividad;
import ar.edu.utn.frba.dds.donaciones.service.TareaNocturnaMatchmaking;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

public class TareasProgramadasController {

  private final TareaNocturnaMatchmaking tareaMatchmaking;
  private final TareaDeInactividad tareaDeInactividad;

  public TareasProgramadasController(TareaNocturnaMatchmaking tareaMatchmaking, TareaDeInactividad tareaDeInactividad) {
    this.tareaMatchmaking = tareaMatchmaking;
    this.tareaDeInactividad = tareaDeInactividad;
  }

  public void ejecutarMatchmaking(Context ctx) {
    validarApiKey(ctx);
    tareaMatchmaking.ejecutar();
    ctx.status(200).result("Matchmaking ejecutado correctamente");
  }

  public void ejecutarInactividad(Context ctx) {
    validarApiKey(ctx);
    tareaDeInactividad.ejecutar();
    ctx.status(200).result("Notificaciones de inactividad enviadas correctamente");
  }

  private void validarApiKey(Context ctx) {
    String apiKeyEsperada = System.getenv("CRON_API_KEY");
    String apiKeyEnviada = ctx.header("X-API-KEY");

    if (apiKeyEsperada == null || !apiKeyEsperada.equals(apiKeyEnviada)) {
      throw new UnauthorizedResponse("No autorizado para ejecutar procesos batch");
    }
  }
}