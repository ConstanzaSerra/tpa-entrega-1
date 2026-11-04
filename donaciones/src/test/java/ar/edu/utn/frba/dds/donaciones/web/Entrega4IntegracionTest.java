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

    String htmlLogin = engine.render("login.hbs", model, null);
    assertTrue(htmlLogin.contains("Iniciar Sesión"));
  }
}
