package ar.edu.utn.frba.dds.donaciones.web;

import ar.edu.utn.frba.dds.donaciones.controller.DonacionController;
import ar.edu.utn.frba.dds.donaciones.controller.DonacionesGraphQLController;
import ar.edu.utn.frba.dds.donaciones.controller.NotificacionController;
import ar.edu.utn.frba.dds.donaciones.domain.EntidadBeneficiaria;
import ar.edu.utn.frba.dds.donaciones.domain.NotificacionWeb;
import ar.edu.utn.frba.dds.donaciones.domain.RolUsuario;
import ar.edu.utn.frba.dds.donaciones.domain.Usuario;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.notificaciones.RegistradorNotificacionesWeb;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import ar.edu.utn.frba.dds.donaciones.repository.DonanteRepository;
import ar.edu.utn.frba.dds.donaciones.repository.EntidadRepository;
import ar.edu.utn.frba.dds.donaciones.repository.NotificacionWebRepository;
import ar.edu.utn.frba.dds.donaciones.repository.SubcategoriaRepository;
import ar.edu.utn.frba.dds.donaciones.repository.UsuarioRepository;
import ar.edu.utn.frba.dds.donaciones.service.ImportadorDonantesCSV;
import ar.edu.utn.frba.dds.donaciones.service.LectorDonantesCSV;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Entrega4IntegracionTest {

  private DonanteRepository donanteRepo;
  private EntidadRepository entidadRepo;
  private SubcategoriaRepository subcategoriaRepo;
  private DonacionRepository donacionRepo;
  private UsuarioRepository usuarioRepo;
  private NotificacionWebRepository notifRepo;
  private GestorDeNotificaciones gestor;

  @BeforeEach
  void setUp() {
    donanteRepo = DonanteRepository.getInstancia();
    entidadRepo = new EntidadRepository();
    subcategoriaRepo = new SubcategoriaRepository();
    donacionRepo = new DonacionRepository();
    usuarioRepo = new UsuarioRepository();
    notifRepo = new NotificacionWebRepository();
    gestor = new GestorDeNotificaciones();
    gestor.suscribir(new RegistradorNotificacionesWeb(notifRepo));
  }

  @Test
  void usuarioHasheaYVerificaPasswordCorrectamente() {
    Usuario admin = new Usuario("admin_test@donatrack.org", "secreto123", "Admin Demo", RolUsuario.ADMIN, null, null);
    usuarioRepo.guardar(admin);

    assertNotNull(admin.getId());
    assertTrue(admin.verificarPassword("secreto123"));
    assertFalse(admin.verificarPassword("clave_incorrecta"));
    assertEquals(RolUsuario.ADMIN, admin.getRol());
  }

  @Test
  void observadorRegistradorNotificacionesWebPersisteBandejaDeEntidad() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor Comunitario Norte",
        "Av. Cabildo 2100, CABA",
        "1144445555",
        new ArrayList<>(List.of("norte@comedor.org")),
        new ArrayList<>()
    );
    entidadRepo.guardar(entidad);

    gestor.publicar(new EventoDeNotificacion(
        List.of(entidad),
        "Se le asignó la donación #10 de Leche Larga Vida."
    ));

    List<NotificacionWeb> bandeja = notifRepo.buscarPorEntidad(entidad.getId());
    assertEquals(1, bandeja.size());
    assertTrue(bandeja.get(0).getMensaje().contains("donación #10"));
    assertFalse(bandeja.get(0).isLeida());
  }

  @Test
  void importacionMasivaCsv10050FilasConCreacionDeUsuarios() {
    StringBuilder csv = new StringBuilder("tipo,tipo_documento,documento,nombre,email,telefono\n");
    int totalFilas = 10_050;
    for (int i = 1; i <= totalFilas; i++) {
      csv.append("HUMANA,DNI,").append(30000000 + i)
          .append(",Donante ").append(i)
          .append(",donante").append(i).append("@donatrack.org,+549110000\n");
    }

    ImportadorDonantesCSV importador = new ImportadorDonantesCSV(donanteRepo, new LectorDonantesCSV());
    ImportadorDonantesCSV.ResultadoImportacion resultado = importador.importarMasivoConUsuarios(
        new StringReader(csv.toString()), usuarioRepo
    );

    assertEquals(totalFilas, resultado.filasLeidas());
    assertEquals(totalFilas, resultado.donantesCreados());
    assertEquals(totalFilas, resultado.usuariosOtorgados());
    assertTrue(usuarioRepo.buscarPorEmail("donante1@donatrack.org").isPresent());
    assertTrue(usuarioRepo.buscarPorEmail("donante10050@donatrack.org").isPresent());
  }

  @Test
  void seederCargaDatosInicialesYGraphQLRespondeDonacionesAsignadas() {
    DatosInicialesSeeder.inicializarSiVacio(
        subcategoriaRepo, donanteRepo, entidadRepo, donacionRepo, usuarioRepo, gestor
    );

    assertFalse(usuarioRepo.obtenerTodos().isEmpty());

    DonacionesGraphQLController graphQLController = new DonacionesGraphQLController(
        donacionRepo, gestor
    );

    Context ctx = mock(Context.class);
    when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
        "query", "query { donacionesPorEstado(estado: \"ASIGNACION_REALIZADA\") { donacionId entidadBeneficiariaId direccionDestino } }"
    ));

    graphQLController.ejecutar(ctx);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(ctx).json(captor.capture());
    assertNotNull(captor.getValue().get("data"));

    HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
    Map<String, Object> model = new HashMap<>();
    model.put("titulo", "DonaTrack Test");
    model.put("rol", "DONANTE");
    model.put("esDonante", true);
    model.put("donacionesEntregadasCount", 2);
    model.put("entidadesCount", 3);
    model.put("donantesCount", 2);

    String htmlLanding = engine.render("landing.hbs", model, null);
    assertTrue(htmlLanding.contains("DonaTrack"));
    // Req 1: Antes del footer, sacar accesos directos a la plataforma
    assertFalse(htmlLanding.contains("Accesos Directos de la Plataforma"));

    String htmlLogin = engine.render("login.hbs", model, null);
    assertTrue(htmlLogin.contains("Iniciar Sesión"));

    // Req 2: El footer no repite el enlace de registro que ya está en la barra de navegación
    String htmlLayout = engine.render("legal.hbs", model, null);
    assertFalse(htmlLayout.contains("<a href=\"/registro\" class=\"me-3\">Registro</a>"));

    // Req 3: Botón que dice "Soy Persona Donante / Jurídica" eliminado de /registro
    String htmlRegistro = engine.render("registro.hbs", model, null);
    assertFalse(htmlRegistro.contains("Soy Persona Donante (Humana / Jurídica)"));
    assertTrue(htmlRegistro.contains("Registro de Entidad Beneficiaria"));
  }

  @Test
  void paginacionHelperCalculaPaginasYRangoCorrectamente() {
    List<String> items = List.of("Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7");
    WebPortalController.Paginacion<String> pag = new WebPortalController.Paginacion<>(
        items, 1, 3, "/test", Map.of("filtro", "alimentos")
    );

    assertEquals(1, pag.getPaginaActual());
    assertEquals(3, pag.getTotalPaginas());
    assertEquals(7, pag.getTotalItems());
    assertEquals(3, pag.getItems().size());
    assertEquals("Item 1", pag.getItems().get(0));
    assertTrue(pag.isTieneSiguiente());
    assertFalse(pag.isTieneAnterior());
    assertTrue(pag.isMostrarPaginacion());
    assertEquals(1, pag.getRangoInicio());
    assertEquals(3, pag.getRangoFin());

    // Segunda página
    WebPortalController.Paginacion<String> pag2 = new WebPortalController.Paginacion<>(
        items, 2, 3, "/test", Map.of("filtro", "alimentos")
    );
    assertEquals(2, pag2.getPaginaActual());
    assertEquals(3, pag2.getItems().size());
    assertEquals("Item 4", pag2.getItems().get(0));
    assertTrue(pag2.isTieneAnterior());
    assertTrue(pag2.isTieneSiguiente());
    assertEquals(4, pag2.getRangoInicio());
    assertEquals(6, pag2.getRangoFin());

    // Última página
    WebPortalController.Paginacion<String> pag3 = new WebPortalController.Paginacion<>(
        items, 3, 3, "/test", Map.of()
    );
    assertEquals(1, pag3.getItems().size());
    assertEquals("Item 7", pag3.getItems().get(0));
    assertFalse(pag3.isTieneSiguiente());
    assertEquals(7, pag3.getRangoInicio());
    assertEquals(7, pag3.getRangoFin());
  }

  @Test
  void donanteEntidadesTemplateContieneBotonDonarYModal() {
    HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
    Map<String, Object> model = new HashMap<>();
    model.put("tituloPagina", "Entidades Beneficiarias - DonaTrack");
    model.put("esDonante", true);

    Map<String, Object> necesidad = new HashMap<>();
    necesidad.put("id", 1L);
    necesidad.put("subcategoriaNombre", "Fideos secos");
    necesidad.put("categoriaNombre", "ALIMENTOS");
    necesidad.put("tipo", "RECURRENTE");
    necesidad.put("objetivo", 100);
    necesidad.put("recibida", 40);
    necesidad.put("faltante", 60);
    necesidad.put("porcentaje", 40);
    necesidad.put("descripcion", "Almuerzos semanales");
    necesidad.put("satisfecha", false);

    Map<String, Object> entidad = new HashMap<>();
    entidad.put("id", 1L);
    entidad.put("razonSocial", "Comedor Sonrisas");
    entidad.put("direccion", "Medrano 951");
    entidad.put("telefono", "4867-7500");
    entidad.put("emailPrincipal", "comedor@sonrisas.org");
    entidad.put("totalNecesidades", 1);
    entidad.put("totalPendientes", 1);
    entidad.put("totalSatisfechas", 0);
    entidad.put("necesidades", List.of(necesidad));

    model.put("entidades", List.of(entidad));

    String html = engine.render("donante-entidades.hbs", model, null);
    // Req 7: Botón en cada subcard para poder donar
    assertTrue(html.contains("Donar a esta necesidad"));
    assertTrue(html.contains("id=\"modalDonar\""));
    assertTrue(html.contains("action=\"/donante/donar\""));
  }

  @Test
  void seguridadCsrfProtegeContraRoboDeFormulario() {
    SesionManager sm = new SesionManager(usuarioRepo);
    Context ctx = mock(Context.class);

    // 1. Cuando un usuario accede por primera vez, no tiene cookie CSRF
    when(ctx.cookie(SesionManager.COOKIE_CSRF)).thenReturn(null);
    String tokenGenerado = sm.obtenerOCrearCsrfToken(ctx);
    assertNotNull(tokenGenerado);
    assertFalse(tokenGenerado.isBlank());
    verify(ctx).cookie(org.mockito.ArgumentMatchers.any(io.javalin.http.Cookie.class));

    // 2. Simulación de ataque CSRF ("robar el form"): origen externo envía form sin token o con token falso
    when(ctx.cookie(SesionManager.COOKIE_CSRF)).thenReturn("token-secreto-sesion-legitima");
    when(ctx.formParam("_csrf")).thenReturn(null);
    when(ctx.queryParam("_csrf")).thenReturn(null);
    when(ctx.header("X-CSRF-Token")).thenReturn(null);
    assertFalse(sm.validarCsrfToken(ctx), "Un formulario robado sin token debe ser rechazado por seguridad");

    when(ctx.formParam("_csrf")).thenReturn("token-falso-atacante");
    assertFalse(sm.validarCsrfToken(ctx), "Un formulario con token manipulado debe ser rechazado");

    // 3. Envío legítimo con el token coincidente
    when(ctx.formParam("_csrf")).thenReturn("token-secreto-sesion-legitima");
    assertTrue(sm.validarCsrfToken(ctx), "El formulario legítimo con token CSRF válido debe ser aceptado");

    // 4. Verificación de que las plantillas renderizan el campo oculto _csrf
    HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
    Map<String, Object> model = new HashMap<>();
    model.put("csrfToken", "test-token-csrf-123");
    String htmlLogin = engine.render("login.hbs", model, null);
    assertTrue(htmlLogin.contains("name=\"_csrf\""));
    assertTrue(htmlLogin.contains("test-token-csrf-123"));
  }

  @Test
  void adminDepositoContieneResumenConKpisYTabsDivididos() {
    HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
    Map<String, Object> model = new HashMap<>();
    model.put("tituloPagina", "Gestión de Depósito Central - DonaTrack");
    model.put("esAdmin", true);
    model.put("csrfToken", "csrf-admin-deposito");

    Map<String, Object> resumen = new HashMap<>();
    resumen.put("totalDonaciones", 15);
    resumen.put("enDeposito", 8);
    resumen.put("enTransito", 4);
    resumen.put("vencidas", 2);
    resumen.put("entregadas", 1);
    resumen.put("totalDonantes", 6);
    model.put("resumen", resumen);

    model.put("esTabInventario", true);
    model.put("donaciones", List.of());
    model.put("donantes", List.of());
    model.put("subcategorias", List.of());

    String html = engine.render("admin-deposito.hbs", model, null);

    // Verificación del resumen ejecutivo (KPI cards)
    assertTrue(html.contains("Stock en Depósito"));
    assertTrue(html.contains("En Logística / En Camino"));
    assertTrue(html.contains("Donaciones Vencidas"));
    assertTrue(html.contains("Donantes Registrados"));

    // Verificación de la división en pestañas/secciones
    assertTrue(html.contains("id=\"tab-inventario\""));
    assertTrue(html.contains("id=\"tab-recepcion\""));
    assertTrue(html.contains("id=\"tab-donante\""));

    // Verificación de token CSRF en formularios de depósito
    assertTrue(html.contains("action=\"/admin/donaciones\""));
    assertTrue(html.contains("action=\"/admin/donantes\""));
    assertTrue(html.contains("value=\"csrf-admin-deposito\""));
  }
}
