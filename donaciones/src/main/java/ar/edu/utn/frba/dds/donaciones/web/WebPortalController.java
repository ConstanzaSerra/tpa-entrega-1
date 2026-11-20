package ar.edu.utn.frba.dds.donaciones.web;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.matchmaking.ProcesadorMatchmaking;
import ar.edu.utn.frba.dds.donaciones.matchmaking.PropuestaMatchmaking;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.puertos.LogisticaPort;
import ar.edu.utn.frba.dds.donaciones.repository.*;
import ar.edu.utn.frba.dds.donaciones.service.ImportadorDonantesCSV;
import ar.edu.utn.frba.dds.donaciones.service.LectorDonantesCSV;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador MVC del Cliente Liviano (Entrega 4).
 * Renderiza las vistas HTML mediante el motor de plantillas Handlebars
 * para los 4 perfiles: Persona no autenticada, Persona donante,
 * Entidad beneficiaria y Persona administradora.
 */
public class WebPortalController {

  /**
   * Fotos que se muestran en galeria, inicio y mapa cuando la entrega todavia no tiene fotos cargadas.
   * Se van alternando para que dos tarjetas seguidas no tengan la misma foto.
   */
  private static final List<String> FOTOS_ILUSTRATIVAS_DONACION = List.of(
      "https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=800&q=80",
      "/img/donacion-alimentos.jpg");

  private final DonacionRepository donacionRepo;
  private final DonanteRepository donanteRepo;
  private final EntidadRepository entidadRepo;
  private final SubcategoriaRepository subcategoriaRepo;
  private final PropuestaRepository propuestaRepo;
  private final CargaDonacionRepository cargaRepo;
  private final UsuarioRepository usuarioRepo;
  private final NotificacionWebRepository notifWebRepo;
  private final ProcesadorMatchmaking procesadorMatchmaking;
  private final GestorDeNotificaciones gestorNotificaciones;
  private final LogisticaPort logisticaPort;
  private final SesionManager sesionManager;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public WebPortalController(DonacionRepository donacionRepo,
                             DonanteRepository donanteRepo,
                             EntidadRepository entidadRepo,
                             SubcategoriaRepository subcategoriaRepo,
                             PropuestaRepository propuestaRepo,
                             CargaDonacionRepository cargaRepo,
                             UsuarioRepository usuarioRepo,
                             NotificacionWebRepository notifWebRepo,
                             ProcesadorMatchmaking procesadorMatchmaking,
                             GestorDeNotificaciones gestorNotificaciones,
                             LogisticaPort logisticaPort,
                             SesionManager sesionManager) {
    this.donacionRepo = donacionRepo;
    this.donanteRepo = donanteRepo;
    this.entidadRepo = entidadRepo;
    this.subcategoriaRepo = subcategoriaRepo;
    this.propuestaRepo = propuestaRepo;
    this.cargaRepo = cargaRepo;
    this.usuarioRepo = usuarioRepo;
    this.notifWebRepo = notifWebRepo;
    this.procesadorMatchmaking = procesadorMatchmaking;
    this.gestorNotificaciones = gestorNotificaciones;
    this.logisticaPort = logisticaPort;
    this.sesionManager = sesionManager;
  }

  // =========================================================================
  // 1–4. PERSONA NO AUTENTICADA (PÚBLICO)
  // =========================================================================

  /** 1. Landing Page pública */
  public void landing(Context ctx) {
    Map<String, Object> model = modeloBase(ctx, "Inicio - DonaTrack");
    List<Donacion> todas = donacionRepo.obtenerTodas();
    long entregadasCount = todas.stream().filter(d -> d.getEstado() == EstadoDonacion.ENTREGADA).count();

    model.put("totalDonaciones", todas.size());
    model.put("totalEntregadas", entregadasCount);
    model.put("totalDonantes", donanteRepo.obtenerTodos().size());
    model.put("totalEntidades", entidadRepo.obtenerTodas().size());
    model.put("entregasDestacadas", armarEntregasEntregadasVista().stream().limit(3).toList());
    ctx.render("landing.hbs", model);
  }

  /** 1.i Visualización de fotos de donaciones entregadas sin login */
  public void galeriaFotos(Context ctx) {
    int pagina = Math.max(1, parsearEntero(ctx.queryParam("page"), 1));
    List<Map<String, Object>> entregas = armarEntregasEntregadasVista();
    Paginacion<Map<String, Object>> paginacion = new Paginacion<>(entregas, pagina, 6, "/galeria", Map.of());

    Map<String, Object> model = modeloBase(ctx, "Galería de Donaciones Entregadas - DonaTrack");
    model.put("entregas", paginacion.getItems());
    model.put("paginacion", paginacion);
    ctx.render("galeria.hbs", model);
  }

  /** 2. Mapa interactivo público de donaciones ya entregadas */
  public void mapaEntregasPublico(Context ctx) {
    Map<String, Object> model = modeloBase(ctx, "Mapa de Donaciones Entregadas - DonaTrack");
    List<Map<String, Object>> entregas = armarEntregasEntregadasVista();
    model.put("entregas", entregas);
    try {
      model.put("marcadoresJson", jsonParaScript(entregas));
    } catch (Exception e) {
      model.put("marcadoresJson", "[]");
    }
    ctx.render("mapa-entregas.hbs", model);
  }

  /** 1.iii Información legal y de privacidad */
  public void legalPrivacidad(Context ctx) {
    Map<String, Object> model = modeloBase(ctx, "Información Legal y Privacidad - DonaTrack");
    ctx.render("legal.hbs", model);
  }

  /** 3. Vista de Registro para personas donantes y entidades beneficiarias */
  public void mostrarRegistro(Context ctx) {
    Map<String, Object> model = modeloBase(ctx, "Registro - DonaTrack");
    ctx.render("registro.hbs", model);
  }

  /** 3.a POST Registro de Persona Donante (Humana o Jurídica) */
  public void registrarDonante(Context ctx) {
    String tipo = valor(ctx.formParam("tipo"), "HUMANA");
    String emailStr = valor(ctx.formParam("email"), "").trim().toLowerCase();
    String password = valor(ctx.formParam("password"), "");
    String telefonoStr = valor(ctx.formParam("telefono"), "").trim();
    String whatsappStr = valor(ctx.formParam("whatsapp"), "").trim();
    String medioPreferido = valor(ctx.formParam("medioPreferido"), "EMAIL");

    ValidadorFormulario validador = new ValidadorFormulario()
        .obligatorio("email", emailStr, "el correo").email("email", emailStr)
        .obligatorio("password", password, "la contraseña").longitudMinima("password", password, 6, "la contraseña")
        .obligatorio("telefono", telefonoStr, "el teléfono").telefono("telefono", telefonoStr)
        .telefono("whatsapp", whatsappStr);
    if (!validador.errores().containsKey("email") && usuarioRepo.buscarPorEmail(emailStr).isPresent()) {
      validador.agregar("email", "Ya hay una cuenta con el correo " + emailStr + ".");
    }
    if (validador.hayErrores()) {
      ctx.redirect("/registro?error=" + urlEncode(validador.resumen()));
      return;
    }

    Email email = new Email(emailStr);
    Telefono tel = new Telefono(telefonoStr);
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, tel));
    MedioDeContacto predeterminado = email;
    if ("TELEFONO".equalsIgnoreCase(medioPreferido)) {
      predeterminado = tel;
    } else if ("WHATSAPP".equalsIgnoreCase(medioPreferido) && !whatsappStr.isBlank()) {
      WhatsApp ws = new WhatsApp(whatsappStr);
      medios.add(ws);
      predeterminado = ws;
    }

    PersonaDonante donante;
    String nombreMostrar;
    if ("JURIDICA".equalsIgnoreCase(tipo)) {
      String razonSocial = valor(ctx.formParam("razonSocial"), "Organización Solidaria");
      String tipoJurStr = valor(ctx.formParam("tipoJuridico"), "EMPRESA");
      String rubro = valor(ctx.formParam("rubro"), "General");
      TipoJuridico tipoJuridico;
      try {
        tipoJuridico = TipoJuridico.valueOf(tipoJurStr);
      } catch (Exception e) {
        tipoJuridico = TipoJuridico.ONG;
      }
      donante = new PersonaJuridica(medios, predeterminado, razonSocial, tipoJuridico, rubro, new ArrayList<>());
      nombreMostrar = razonSocial;
    } else {
      String nombre = valor(ctx.formParam("nombre"), "Donante");
      String apellido = valor(ctx.formParam("apellido"), "");
      Integer edad = parsearEntero(ctx.formParam("edad"), 30);
      Integer dni = parsearEntero(ctx.formParam("dni"), 10000000);
      String genero = valor(ctx.formParam("genero"), "No especificado");
      String direccion = valor(ctx.formParam("direccion"), "CABA");
      donante = new PersonaHumana(medios, predeterminado, nombre, apellido, edad, dni, genero, direccion);
      nombreMostrar = (nombre + " " + apellido).trim();
    }

    donanteRepo.guardar(donante);
    Usuario nuevoUsuario = new Usuario(emailStr, password, nombreMostrar, RolUsuario.DONANTE, donante, null);
    usuarioRepo.guardar(nuevoUsuario);
    sesionManager.iniciarSesion(ctx, nuevoUsuario);

    ctx.redirect("/donante/donaciones?msg=" + urlEncode("¡Bienvenido/a " + nombreMostrar + "! Tu cuenta de donante fue creada."));
  }

  /** 3.b POST Registro de Entidad Beneficiaria */
  public void registrarEntidad(Context ctx) {
    String razonSocial = valor(ctx.formParam("razonSocial"), "").trim();
    String direccion = valor(ctx.formParam("direccion"), "").trim();
    String telefono = valor(ctx.formParam("telefono"), "").trim();
    String emailStr = valor(ctx.formParam("email"), "").trim().toLowerCase();
    // Sin contrasena por defecto: antes, si faltaba, la cuenta quedaba con "entidad123"
    String password = valor(ctx.formParam("password"), "");

    ValidadorFormulario validador = new ValidadorFormulario()
        .obligatorio("razonSocial", razonSocial, "la razón social")
        .obligatorio("direccion", direccion, "la dirección de la sede")
        .obligatorio("telefono", telefono, "el teléfono").telefono("telefono", telefono)
        .obligatorio("email", emailStr, "el correo").email("email", emailStr)
        .obligatorio("password", password, "la contraseña").longitudMinima("password", password, 6, "la contraseña");
    if (!validador.errores().containsKey("email") && usuarioRepo.buscarPorEmail(emailStr).isPresent()) {
      validador.agregar("email", "Ya hay una cuenta con este correo. Si es tuya, iniciá sesión.");
    }
    if (validador.hayErrores()) {
      // Se vuelve a mostrar el formulario con lo que la persona escribio (salvo la contrasena)
      // y cada error debajo de su campo, en lugar de redirigir a un formulario vacio.
      Map<String, Object> model = modeloBase(ctx, "Registro - DonaTrack");
      model.put("valores", Map.of("razonSocial", razonSocial, "direccion", direccion,
          "telefono", telefono, "email", emailStr));
      model.put("errores", validador.errores());
      model.put("error", "Revisá los campos marcados en rojo.");
      ctx.status(400);
      ctx.render("registro.hbs", model);
      return;
    }

    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        razonSocial, direccion, telefono, new ArrayList<>(List.of(emailStr)), new ArrayList<>()
    );
    entidadRepo.guardar(entidad);

    Usuario nuevoUsuario = new Usuario(emailStr, password, razonSocial, RolUsuario.ENTIDAD, null, entidad);
    usuarioRepo.guardar(nuevoUsuario);
    sesionManager.iniciarSesion(ctx, nuevoUsuario);

    ctx.redirect("/entidad/necesidades?msg=" + urlEncode("¡Entidad " + razonSocial + " registrada con éxito!"));
  }

  /** 4. Vista de Inicio de Sesión */
  public void mostrarLogin(Context ctx) {
    Map<String, Object> model = modeloBase(ctx, "Iniciar Sesión - DonaTrack");
    // Los botones de acceso rapido muestran contrasenas: solo con MOSTRAR_ACCESOS_DEMO=true
    model.put("mostrarAccesosDemo", "true".equalsIgnoreCase(System.getenv("MOSTRAR_ACCESOS_DEMO")));
    ctx.render("login.hbs", model);
  }

  /** 4. POST Inicio de Sesión con Cookie HTTP */
  public void procesarLogin(Context ctx) {
    String email = valor(ctx.formParam("email"), "").trim().toLowerCase();
    String password = valor(ctx.formParam("password"), "");

    Optional<Usuario> oUsuario = usuarioRepo.buscarPorEmail(email);
    if (oUsuario.isEmpty() || !oUsuario.get().verificarPassword(password)) {
      ctx.redirect("/login?error=" + urlEncode("Credenciales inválidas. Verificá tu correo y contraseña."));
      return;
    }

    Usuario usuario = oUsuario.get();
    if (usuario.getDonante() != null) {
      usuario.getDonante().registrarInteraccion();
      donanteRepo.guardar(usuario.getDonante());
    }
    sesionManager.iniciarSesion(ctx, usuario);

    switch (usuario.getRol()) {
      case ADMIN -> ctx.redirect("/admin/deposito?msg=" + urlEncode("Sesión iniciada como Persona Administradora"));
      case ENTIDAD -> ctx.redirect("/entidad/donaciones?msg=" + urlEncode("Bienvenido/a, " + usuario.getNombreMostrar()));
      default -> ctx.redirect("/donante/donaciones?msg=" + urlEncode("Bienvenido/a, " + usuario.getNombreMostrar()));
    }
  }

  public void logout(Context ctx) {
    sesionManager.cerrarSesion(ctx);
    ctx.redirect("/?msg=" + urlEncode("Cerraste sesión correctamente"));
  }

  // =========================================================================
  // 5–7. ROL PERSONA DONANTE
  // =========================================================================

  /** 5. Filtrar sus donaciones realizadas por estado y categoría o subcategoría/s con paginación */
  public void donanteMisDonaciones(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    String filtroEstado = valor(ctx.queryParam("estado"), "");
    String filtroCategoria = valor(ctx.queryParam("categoria"), "");
    String filtroSubcategoria = valor(ctx.queryParam("subcategoria"), "").trim();
    int pagina = Math.max(1, parsearEntero(ctx.queryParam("page"), 1));

    List<Donacion> base = (u.getDonante() != null)
        ? donacionRepo.buscarPorDonante(u.getDonante().getId())
        : donacionRepo.obtenerTodas();

    List<Donacion> filtradas = base.stream()
        .filter(d -> filtroEstado.isBlank() || (d.getEstado() != null && d.getEstado().name().equalsIgnoreCase(filtroEstado)))
        .filter(d -> filtroCategoria.isBlank() || (d.getSubcategoria() != null && d.getSubcategoria().getCategoria() != null
            && d.getSubcategoria().getCategoria().name().equalsIgnoreCase(filtroCategoria)))
        .filter(d -> filtroSubcategoria.isBlank() || (d.getSubcategoria() != null && d.getSubcategoria().getNombre() != null
            && d.getSubcategoria().getNombre().toLowerCase().contains(filtroSubcategoria.toLowerCase())))
        .toList();

    List<Map<String, Object>> donacionesMapeadas = filtradas.stream().map(this::mapearDonacionVista).toList();
    Map<String, String> qParams = new HashMap<>();
    if (!filtroEstado.isBlank()) qParams.put("estado", filtroEstado);
    if (!filtroCategoria.isBlank()) qParams.put("categoria", filtroCategoria);
    if (!filtroSubcategoria.isBlank()) qParams.put("subcategoria", filtroSubcategoria);

    Paginacion<Map<String, Object>> paginacion = new Paginacion<>(donacionesMapeadas, pagina, 5, "/donante/donaciones", qParams);

    Map<String, Object> model = modeloBase(ctx, "Mis Donaciones - DonaTrack");
    model.put("donaciones", paginacion.getItems());
    model.put("paginacion", paginacion);
    model.put("filtroEstado", filtroEstado);
    model.put("filtroCategoria", filtroCategoria);
    model.put("filtroSubcategoria", filtroSubcategoria);
    model.put("subcategoriasDisponibles", subcategoriaRepo.obtenerTodas());
    ctx.render("donante-donaciones.hbs", model);
  }

  /** 6. Navegar por las entidades beneficiarias registradas con filtros y paginación (Req. 4 y 6) */
  public void donanteEntidades(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    String busqueda = valor(ctx.queryParam("q"), "").trim();
    String filtroCategoria = valor(ctx.queryParam("categoria"), "").trim();
    boolean soloPendientes = "true".equalsIgnoreCase(ctx.queryParam("soloPendientes"));
    int pagina = Math.max(1, parsearEntero(ctx.queryParam("page"), 1));

    List<EntidadBeneficiaria> base = entidadRepo.obtenerTodas();
    List<EntidadBeneficiaria> filtradas = base.stream()
        .filter(e -> {
          if (busqueda.isBlank()) return true;
          String b = busqueda.toLowerCase();
          boolean coincideEntidad = (e.getRazonSocial() != null && e.getRazonSocial().toLowerCase().contains(b))
              || (e.getDireccion() != null && e.getDireccion().toLowerCase().contains(b));
          boolean coincideNecesidad = e.getNecesidades() != null && e.getNecesidades().stream().anyMatch(n ->
              (n.getDescripcion() != null && n.getDescripcion().toLowerCase().contains(b))
                  || (n.getSubcategoria() != null && n.getSubcategoria().getNombre() != null
                  && n.getSubcategoria().getNombre().toLowerCase().contains(b))
          );
          return coincideEntidad || coincideNecesidad;
        })
        .filter(e -> {
          if (filtroCategoria.isBlank()) return true;
          return e.getNecesidades() != null && e.getNecesidades().stream().anyMatch(n ->
              n.getSubcategoria() != null && n.getSubcategoria().getCategoria() != null
                  && n.getSubcategoria().getCategoria().name().equalsIgnoreCase(filtroCategoria)
          );
        })
        .filter(e -> {
          if (!soloPendientes) return true;
          return e.getNecesidades() != null && e.getNecesidades().stream().anyMatch(n -> !Boolean.TRUE.equals(n.estaSatisfecha()));
        })
        .toList();

    List<Map<String, Object>> entidadesMapeadas = filtradas.stream().map(this::mapearEntidadVista).toList();

    Map<String, String> queryParams = new HashMap<>();
    if (!busqueda.isBlank()) queryParams.put("q", busqueda);
    if (!filtroCategoria.isBlank()) queryParams.put("categoria", filtroCategoria);
    if (soloPendientes) queryParams.put("soloPendientes", "true");

    Paginacion<Map<String, Object>> paginacion = new Paginacion<>(entidadesMapeadas, pagina, 4, "/donante/entidades", queryParams);

    Map<String, Object> model = modeloBase(ctx, "Entidades Beneficiarias - DonaTrack");
    model.put("entidades", paginacion.getItems());
    model.put("paginacion", paginacion);
    model.put("busqueda", busqueda);
    model.put("filtroCategoria", filtroCategoria);
    model.put("soloPendientes", soloPendientes);
    model.put("totalEntidadesEncontradas", filtradas.size());
    model.put("subcategoriasDisponibles", subcategoriaRepo.obtenerTodas());
    ctx.render("donante-entidades.hbs", model);
  }

  /** 7.b POST Donar directamente a una necesidad desde la subcard (Req. 7) */
  public void donanteRealizarDonacion(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    Long entidadId = parsearLong(ctx.formParam("entidadId"));
    Long necesidadId = parsearLong(ctx.formParam("necesidadId"));
    String subcategoriaNombre = valor(ctx.formParam("subcategoriaNombre"), "").trim();
    int cantidad = Math.max(1, parsearEntero(ctx.formParam("cantidad"), 1));
    String unidadMedida = valor(ctx.formParam("unidadMedida"), "unidades").trim();

    Optional<EntidadBeneficiaria> oEntidad = entidadId != null ? entidadRepo.buscarPorId(entidadId) : Optional.empty();
    if (oEntidad.isEmpty()) {
      ctx.redirect("/donante/entidades?error=" + urlEncode("Entidad beneficiaria no encontrada"));
      return;
    }

    Optional<Subcategoria> oSub = subcategoriaRepo.buscarPorNombre(subcategoriaNombre);
    if (oSub.isEmpty()) {
      ctx.redirect("/donante/entidades?error=" + urlEncode("Subcategoría '" + subcategoriaNombre + "' no válida"));
      return;
    }

    EntidadBeneficiaria entidad = oEntidad.get();
    Subcategoria sub = oSub.get();

    PersonaDonante donante = u.getDonante();
    if (donante == null) {
      List<PersonaDonante> todos = donanteRepo.obtenerTodos();
      if (!todos.isEmpty()) {
        donante = todos.get(0);
      }
    }

    if (donante != null) {
      donante.registrarInteraccion();
      donanteRepo.guardar(donante);
    }

    Donacion d = new Donacion(sub, cantidad, unidadMedida, donante, LocalDate.now());
    d.asignarEntidad(entidad);
    donacionRepo.guardar(d);

    // Actualizar progreso de la necesidad específica en la entidad
    if (entidad.getNecesidades() != null) {
      for (Necesidad n : entidad.getNecesidades()) {
        if ((necesidadId != null && n.getId() != null && n.getId().equals(necesidadId))
            || (n.getSubcategoria() != null && n.getSubcategoria().getNombre().equalsIgnoreCase(sub.getNombre()))) {
          try {
            n.registrarRecepcion(cantidad);
          } catch (Exception ignored) {}
          break;
        }
      }
      entidadRepo.guardar(entidad);
    }

    // Notificaciones (Observer)
    gestorNotificaciones.publicar(new EventoDeNotificacion(
        List.of(entidad),
        "¡Nueva donación asignada! " + (donante != null ? nombreDeDonante(donante) : "Una persona donante")
            + " donó " + cantidad + " " + unidadMedida + " de " + sub.getNombre() + "."
    ));

    if (donante != null) {
      gestorNotificaciones.publicar(new EventoDeNotificacion(
          List.of(donante),
          "¡Gracias por tu donación #" + d.getId() + "! Se registró tu aporte de " + cantidad + " " + unidadMedida
              + " de " + sub.getNombre() + " para " + entidad.getRazonSocial() + "."
      ));
    }

    ctx.redirect("/donante/donaciones?msg=" + urlEncode(
        "¡Muchas gracias! Tu donación de " + cantidad + " " + unidadMedida + " de " + sub.getNombre()
            + " para " + entidad.getRazonSocial() + " fue registrada exitosamente en estado ASIGNACION_REALIZADA."));
  }

  /** 7. Seguir en un mapa las entregas activas de las donaciones que realizó */
  public void donanteSeguimientoMapa(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    List<Donacion> misDonaciones = (u.getDonante() != null)
        ? donacionRepo.buscarPorDonante(u.getDonante().getId())
        : donacionRepo.obtenerTodas();

    List<Donacion> activas = misDonaciones.stream()
        .filter(d -> d.getEstado() == EstadoDonacion.EN_TRASLADO
            || d.getEstado() == EstadoDonacion.LISTA_PARA_ENTREGAR
            || d.getEstado() == EstadoDonacion.ASIGNACION_REALIZADA)
        .toList();

    Map<String, Object> model = modeloBase(ctx, "Seguimiento en Tiempo Real - DonaTrack");
    poblarModeloSeguimientoMapa(model, activas);
    ctx.render("seguimiento-mapa.hbs", model);
  }

  // =========================================================================
  // 8–12. ROL ENTIDAD BENEFICIARIA
  // =========================================================================

  /** 8. Vista y registro de necesidades materiales concretas con paginación */
  public void entidadNecesidades(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    EntidadBeneficiaria entidad = resolverEntidadDeUsuario(u);
    Map<String, Object> model = modeloBase(ctx, "Necesidades Materiales - DonaTrack");
    if (entidad != null) {
      Map<String, Object> entidadVista = mapearEntidadVista(entidad);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> todasNecs = (List<Map<String, Object>>) entidadVista.get("necesidades");
      int pagina = Math.max(1, parsearEntero(ctx.queryParam("page"), 1));
      Paginacion<Map<String, Object>> paginacion = new Paginacion<>(todasNecs, pagina, 5, "/entidad/necesidades", Map.of());
      entidadVista.put("necesidades", paginacion.getItems());
      model.put("entidad", entidadVista);
      model.put("necesidades", paginacion.getItems());
      model.put("paginacion", paginacion);
    }
    model.put("subcategorias", subcategoriasVista());
    ctx.render("entidad-necesidades.hbs", model);
  }

  /** 8.b POST Registrar nueva necesidad material (Extraordinaria o Recurrente) */
  public void entidadCrearNecesidad(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    EntidadBeneficiaria entidad = resolverEntidadDeUsuario(u);
    if (entidad == null) {
      ctx.redirect("/entidad/necesidades?error=" + urlEncode("No se encontró entidad asociada"));
      return;
    }

    String tipoNecesidad = valor(ctx.formParam("tipoNecesidad"), "EXTRAORDINARIA");
    String descripcion = valor(ctx.formParam("descripcion"), "Necesidad registrada en plataforma").trim();
    int cantidad = Math.max(1, parsearEntero(ctx.formParam("cantidad"), 10));
    String periodoStr = valor(ctx.formParam("periodo"), "MENSUAL");

    // Misma regla que en el deposito: la subcategoria sale del catalogo. Una
    // necesidad de "Arros" nunca cruzaria con una donacion de "Arroz".
    Optional<Subcategoria> oSub = buscarSubcategoriaDelFormulario(ctx);
    if (oSub.isEmpty()) {
      ctx.redirect("/entidad/necesidades?error=" + urlEncode(
          "Elegí una subcategoría del catálogo para registrar la necesidad."));
      return;
    }
    Subcategoria sub = oSub.get();
    String subcategoriaNombre = sub.getNombre();

    if (descripcion.isBlank()) {
      descripcion = "Demanda de " + subcategoriaNombre;
    }

    Necesidad nueva;
    if ("RECURRENTE".equalsIgnoreCase(tipoNecesidad)) {
      String descPeriodo = (periodoStr == null || periodoStr.isBlank()) ? "MENSUAL" : periodoStr.trim().toUpperCase();
      PeriodoConsumo periodo = new PeriodoConsumo(cantidad, descPeriodo, 0);
      nueva = new NecesidadRecurrente(sub, descripcion, periodo);
    } else {
      nueva = new NecesidadExtraordinaria(sub, descripcion, cantidad, 0);
    }

    entidad.agregarNecesidad(nueva);
    entidadRepo.guardar(entidad);

    ctx.redirect("/entidad/necesidades?msg=" + urlEncode("Necesidad material (" + subcategoriaNombre + ") registrada correctamente"));
  }

  /** 9 y 10. Ver estado de las donaciones asignadas y confirmar recepción con fotos (con paginación) */
  public void entidadDonaciones(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    EntidadBeneficiaria entidad = resolverEntidadDeUsuario(u);
    List<Donacion> asignadas = (entidad != null)
        ? donacionRepo.buscarPorEntidadAsignada(entidad.getId())
        : donacionRepo.obtenerTodas();

    int pagina = Math.max(1, parsearEntero(ctx.queryParam("page"), 1));
    List<Map<String, Object>> donacionesMapeadas = asignadas.stream().map(this::mapearDonacionVista).toList();
    Paginacion<Map<String, Object>> paginacion = new Paginacion<>(donacionesMapeadas, pagina, 5, "/entidad/donaciones", Map.of());

    Map<String, Object> model = modeloBase(ctx, "Donaciones Asignadas - DonaTrack");
    model.put("entidadNombre", entidad != null ? entidad.getRazonSocial() : "Todas las Entidades");
    model.put("donaciones", paginacion.getItems());
    model.put("paginacion", paginacion);
    ctx.render("entidad-donaciones.hbs", model);
  }

  /** 10. POST Confirmar la recepción de una donación cargando fotos de la misma */
  public void entidadConfirmarRecepcion(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    Long donacionId = Long.valueOf(ctx.pathParam("id"));
    Optional<Donacion> oDonacion = donacionRepo.buscarPorId(donacionId);
    if (oDonacion.isEmpty()) {
      ctx.redirect("/entidad/donaciones?error=" + urlEncode("No se encontró la donación #" + donacionId));
      return;
    }

    Donacion d = oDonacion.get();

    // Una entidad solo puede confirmar donaciones asignadas a ella: sin este chequeo,
    // cambiando el numero en la URL se podia confirmar la donacion de otra entidad.
    if (u.getRol() == RolUsuario.ENTIDAD) {
      EntidadBeneficiaria propia = resolverEntidadDeUsuario(u);
      if (propia == null || d.getEntidadAsignada() == null
          || !propia.getId().equals(d.getEntidadAsignada().getId())) {
        ctx.redirect("/entidad/donaciones?error=" + urlEncode("La donación #" + donacionId + " no está asignada a tu entidad."));
        return;
      }
    }
    // Se recibe lo que llego: la donacion tiene que estar en camino (la ruta del camion ya inicio).
    if (!puedeConfirmarRecepcion(d)) {
      ctx.redirect("/entidad/donaciones?error=" + urlEncode("La donación #" + donacionId
          + " todavía no está en camino: se puede confirmar cuando el camión inicia su recorrido."));
      return;
    }

    String fotoFinal = valor(ctx.formParam("fotoUrl"), "").trim();

    UploadedFile archivo = ctx.uploadedFile("fotoArchivo");
    if (archivo != null && archivo.size() > 0) {
      try {
        byte[] bytes = archivo.content().readAllBytes();
        String mime = archivo.contentType() != null ? archivo.contentType() : "image/jpeg";
        fotoFinal = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
      } catch (Exception ignored) {}
    }

    // La foto es el comprobante publico de la entrega: sin foto no se confirma.
    // (Antes se usaba una foto de stock, que aparecia en la galeria como si fuera la real.)
    if (fotoFinal.isBlank()) {
      ctx.redirect("/entidad/donaciones?error=" + urlEncode("Para confirmar la recepción de la donación #" + donacionId
          + " subí una foto o pegá el enlace de una foto de los bienes recibidos."));
      return;
    }

    try {
      d.avanzarHacia(EstadoDonacion.ENTREGADA);
      donacionRepo.guardar(d);
    } catch (IllegalStateException e) {
      ctx.redirect("/entidad/donaciones?error=" + urlEncode("No se pudo confirmar la donación #" + donacionId + ": " + e.getMessage()));
      return;
    }

    // Actualizar satisfacción de necesidad en la entidad beneficiaria si coincide la subcategoría
    EntidadBeneficiaria entidad = d.getEntidadAsignada();
    if (entidad != null && d.getSubcategoria() != null) {
      entidad.getNecesidades().stream()
          .filter(n -> n.getSubcategoria() != null
              && n.getSubcategoria().getNombre().equalsIgnoreCase(d.getSubcategoria().getNombre()))
          .findFirst()
          .ifPresent(n -> {
            try {
              n.registrarRecepcion(d.getCantidad());
            } catch (Exception ignored) {}
          });
      entidadRepo.guardar(entidad);
    }

    // Sincronizar con microservicio de Logística vía GraphQL y guardar foto
    Long entidadId = entidad != null ? entidad.getId() : 1L;
    String direccion = entidad != null ? entidad.getDireccion() : "";
    logisticaPort.confirmarEntregaConFoto(donacionId, entidadId, direccion, fotoFinal);

    // Publicar notificación tanto para la entidad como para el donante (Req. 11)
    List<Notificable> destinatarios = new ArrayList<>();
    if (entidad != null) destinatarios.add(entidad);
    if (d.getDonante() != null) destinatarios.add(d.getDonante());
    if (!destinatarios.isEmpty()) {
      gestorNotificaciones.publicar(new EventoDeNotificacion(
          destinatarios,
          "Recepción confirmada con comprobante fotográfico para la donación #" + d.getId()
              + " (" + d.getCantidad() + " " + d.getUnidadMedida() + " de "
              + (d.getSubcategoria() != null ? d.getSubcategoria().getNombre() : "bienes") + ")."
      ));
    }

    ctx.redirect("/entidad/donaciones?msg=" + urlEncode("¡Recepción de donación #" + donacionId + " confirmada con éxito y foto registrada!"));
  }

  /** 11. Bandeja de notificaciones de la entidad beneficiaria */
  public void entidadNotificaciones(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    List<NotificacionWeb> notificaciones;
    if (u.getRol() == RolUsuario.ENTIDAD && u.getEntidadBeneficiaria() != null) {
      notificaciones = notifWebRepo.buscarPorEntidad(u.getEntidadBeneficiaria().getId());
    } else if (u.getRol() == RolUsuario.DONANTE && u.getDonante() != null) {
      notificaciones = notifWebRepo.buscarPorDonante(u.getDonante().getId());
    } else {
      notificaciones = notifWebRepo.obtenerTodas();
    }

    Map<String, Object> model = modeloBase(ctx, "Mis Notificaciones - DonaTrack");
    model.put("notificaciones", notificaciones);
    ctx.render("entidad-notificaciones.hbs", model);
  }

  /** 12. Seguir en un mapa las entregas activas que la entidad debe recibir */
  public void entidadSeguimientoMapa(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    EntidadBeneficiaria entidad = resolverEntidadDeUsuario(u);
    List<Donacion> asignadas = (entidad != null)
        ? donacionRepo.buscarPorEntidadAsignada(entidad.getId())
        : donacionRepo.obtenerTodas();

    List<Donacion> activas = asignadas.stream()
        .filter(d -> d.getEstado() == EstadoDonacion.EN_TRASLADO
            || d.getEstado() == EstadoDonacion.LISTA_PARA_ENTREGAR
            || d.getEstado() == EstadoDonacion.ASIGNACION_REALIZADA)
        .toList();

    Map<String, Object> model = modeloBase(ctx, "Seguimiento de Entregas a Recibir - DonaTrack");
    poblarModeloSeguimientoMapa(model, activas);
    ctx.render("seguimiento-mapa.hbs", model);
  }

  // =========================================================================
  // 13–17. ROL PERSONA ADMINISTRADORA
  // =========================================================================

  /** 13 y 14. Gestión de Depósito: registrar donantes, cargas de donaciones y marcar vencidas */
  public void adminDeposito(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    // Filtros y orden viajan por la URL para que la vista sea compartible,
    // el boton "atras" funcione y recargar no pierda lo que la persona eligio.
    EstadoDonacion estado = parsearEnum(EstadoDonacion.class, ctx.queryParam("estado"));
    Categoria categoria = parsearEnum(Categoria.class, ctx.queryParam("categoria"));
    Long donanteId = parsearIdOpcional(ctx.queryParam("donante"));
    String orden = valor(ctx.queryParam("orden"), DonacionRepository.ORDEN_POR_DEFECTO);
    boolean descendente = !"asc".equalsIgnoreCase(ctx.queryParam("dir"));
    int pagina = Math.max(1, parsearEntero(ctx.queryParam("pagina"), 1));
    int tamanio = Math.min(100, Math.max(5, parsearEntero(ctx.queryParam("tam"), 10)));

    DonacionRepository.PaginaDonaciones resultado =
        donacionRepo.buscarPaginado(estado, categoria, donanteId, orden, descendente, pagina, tamanio);

    // Resumen ejecutivo y métricas de depósito (Gestión de Depósito General)
    List<Donacion> todasDonaciones = donacionRepo.obtenerTodas();
    long totalEnDeposito = todasDonaciones.stream().filter(d -> d.getEstado() == EstadoDonacion.EN_DEPOSITO).count();
    long totalEnTransito = todasDonaciones.stream().filter(d -> d.getEstado() == EstadoDonacion.ASIGNACION_REALIZADA || d.getEstado() == EstadoDonacion.LISTA_PARA_ENTREGAR || d.getEstado() == EstadoDonacion.EN_TRASLADO).count();
    long totalVencidas = todasDonaciones.stream().filter(d -> d.getEstado() == EstadoDonacion.VENCIDA).count();
    long totalEntregadas = todasDonaciones.stream().filter(d -> d.getEstado() == EstadoDonacion.ENTREGADA).count();
    long totalDonantes = donanteRepo.obtenerTodos().size();

    Map<String, Object> resumen = new HashMap<>();
    resumen.put("totalDonaciones", todasDonaciones.size());
    resumen.put("enDeposito", totalEnDeposito);
    resumen.put("enTransito", totalEnTransito);
    resumen.put("vencidas", totalVencidas);
    resumen.put("entregadas", totalEntregadas);
    resumen.put("totalDonantes", totalDonantes);

    String tab = valor(ctx.queryParam("tab"), "inventario");

    Map<String, Object> model = modeloBase(ctx, "Administración de Depósito - DonaTrack");
    model.put("resumen", resumen);
    model.put("tabActivo", tab);
    model.put("esTabInventario", "inventario".equalsIgnoreCase(tab));
    model.put("esTabRecepcion", "recepcion".equalsIgnoreCase(tab) || "ingreso".equalsIgnoreCase(tab));
    model.put("esTabDonante", "donante".equalsIgnoreCase(tab));
    model.put("donantes", donanteRepo.obtenerTodos().stream().map(this::mapearDonanteVista).toList());
    model.put("donaciones", resultado.items().stream().map(this::mapearDonacionVista).toList());
    model.put("subcategorias", subcategoriasVista());
    model.put("categorias", List.of(Categoria.values()));
    model.put("estados", List.of(EstadoDonacion.values()));

    Map<String, String> filtros = new LinkedHashMap<>();
    if (estado != null) { filtros.put("estado", estado.name()); }
    if (categoria != null) { filtros.put("categoria", categoria.name()); }
    if (donanteId != null) { filtros.put("donante", String.valueOf(donanteId)); }
    filtros.put("tam", String.valueOf(tamanio));

    model.put("filtroEstado", estado != null ? estado.name() : "");
    model.put("filtroCategoria", categoria != null ? categoria.name() : "");
    model.put("filtroDonante", donanteId != null ? String.valueOf(donanteId) : "");
    model.put("hayFiltros", estado != null || categoria != null || donanteId != null);
    model.put("tamanio", tamanio);
    model.put("ordenActual", orden);
    model.put("dirActual", descendente ? "desc" : "asc");
    model.put("orden", enlacesDeOrden(filtros, orden, descendente));
    model.put("paginacion", datosDePaginacion(filtros, orden, descendente, resultado));

    ctx.render("admin-deposito.hbs", model);
  }

  /** Subcategorias con su categoria, para que el combo se filtre al elegirla. */
  private List<Map<String, Object>> subcategoriasVista() {
    return subcategoriaRepo.obtenerTodas().stream()
        .sorted(Comparator.comparing(Subcategoria::getNombre, String.CASE_INSENSITIVE_ORDER))
        .map(sc -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", sc.getId());
          m.put("nombre", sc.getNombre());
          m.put("categoria", sc.getCategoria() != null ? sc.getCategoria().name() : "");
          return m;
        })
        .toList();
  }

  /** Un enlace por columna ordenable: alterna la direccion y conserva los filtros. */
  private Map<String, Map<String, String>> enlacesDeOrden(Map<String, String> filtros,
                                                          String ordenActual, boolean descendente) {
    Map<String, Map<String, String>> enlaces = new LinkedHashMap<>();
    for (String columna : List.of("fecha", "id", "cantidad", "estado", "subcategoria", "donante")) {
      boolean esLaActual = columna.equals(ordenActual);
      // Si ya se esta ordenando por esta columna, el click invierte la direccion.
      String direccion = (esLaActual && descendente) ? "asc" : "desc";
      Map<String, String> datos = new LinkedHashMap<>();
      datos.put("href", urlDeposito(filtros, columna, direccion, 1));
      datos.put("icono", !esLaActual ? "bi-arrow-down-up text-muted"
          : (descendente ? "bi-sort-down" : "bi-sort-up"));
      enlaces.put(columna, datos);
    }
    return enlaces;
  }

  private Map<String, Object> datosDePaginacion(Map<String, String> filtros, String orden,
                                                boolean descendente,
                                                DonacionRepository.PaginaDonaciones resultado) {
    String dir = descendente ? "desc" : "asc";
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("pagina", resultado.pagina());
    p.put("totalPaginas", resultado.totalPaginas());
    p.put("total", resultado.total());
    p.put("hayVariasPaginas", resultado.totalPaginas() > 1);
    p.put("desde", resultado.total() == 0 ? 0 : (resultado.pagina() - 1) * resultado.tamanio() + 1);
    p.put("hasta", Math.min((long) resultado.pagina() * resultado.tamanio(), resultado.total()));
    p.put("hayAnterior", resultado.pagina() > 1);
    p.put("haySiguiente", resultado.pagina() < resultado.totalPaginas());
    p.put("hrefPrimera", urlDeposito(filtros, orden, dir, 1));
    p.put("hrefAnterior", urlDeposito(filtros, orden, dir, resultado.pagina() - 1));
    p.put("hrefSiguiente", urlDeposito(filtros, orden, dir, resultado.pagina() + 1));
    p.put("hrefUltima", urlDeposito(filtros, orden, dir, resultado.totalPaginas()));
    return p;
  }

  private String urlDeposito(Map<String, String> filtros, String orden, String dir, int pagina) {
    StringBuilder url = new StringBuilder("/admin/deposito?orden=").append(orden)
        .append("&dir=").append(dir)
        .append("&pagina=").append(Math.max(1, pagina));
    filtros.forEach((clave, valor) -> url.append("&").append(clave).append("=").append(urlEncode(valor)));
    return url.toString();
  }

  private <E extends Enum<E>> E parsearEnum(Class<E> tipo, String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(tipo, valor.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private Long parsearIdOpcional(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(valor.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 13.a POST Registrar persona donante desde el panel administrador */
  public void adminCrearDonante(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    String tipo = valor(ctx.formParam("tipo"), "HUMANA");
    String emailStr = valor(ctx.formParam("email"), "").trim().toLowerCase();
    String telefonoStr = valor(ctx.formParam("telefono"), "").trim();

    // Antes los datos faltantes se completaban con valores inventados (DNI 30000000,
    // telefono +54 11 0000-0000, "Nombre Apellido"): ahora se rechazan y se dice que falta.
    ValidadorFormulario validador = new ValidadorFormulario()
        .obligatorio("email", emailStr, "el correo del donante").email("email", emailStr)
        .obligatorio("telefono", telefonoStr, "el teléfono del donante").telefono("telefono", telefonoStr);
    if ("JURIDICA".equalsIgnoreCase(tipo)) {
      validador.obligatorio("razonSocial", ctx.formParam("razonSocial"), "la razón social");
    } else {
      validador.obligatorio("nombre", ctx.formParam("nombre"), "el nombre")
          .obligatorio("apellido", ctx.formParam("apellido"), "el apellido")
          .obligatorio("dni", ctx.formParam("dni"), "el DNI")
          .enteroEntre("dni", ctx.formParam("dni"), 1_000_000, 99_999_999, "Ingresá un DNI válido, de 7 u 8 dígitos y sin puntos.");
    }
    if (validador.hayErrores()) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("No se registró el donante. " + validador.resumen()));
      return;
    }

    Email email = new Email(emailStr);
    Telefono tel = new Telefono(telefonoStr);
    List<MedioDeContacto> medios = new ArrayList<>(List.of(email, tel));

    PersonaDonante donante;
    String nombreMostrar;
    if ("JURIDICA".equalsIgnoreCase(tipo)) {
      String razonSocial = valor(ctx.formParam("razonSocial"), "Organización Donante");
      String tipoJurStr = valor(ctx.formParam("tipoJuridico"), "EMPRESA");
      String rubro = valor(ctx.formParam("rubro"), "General");
      TipoJuridico tj;
      try {
        tj = TipoJuridico.valueOf(tipoJurStr);
      } catch (Exception e) {
        tj = TipoJuridico.EMPRESA;
      }
      donante = new PersonaJuridica(medios, email, razonSocial, tj, rubro, new ArrayList<>());
      nombreMostrar = razonSocial;
    } else {
      String nombre = valor(ctx.formParam("nombre"), "Nombre");
      String apellido = valor(ctx.formParam("apellido"), "Apellido");
      Integer edad = parsearEntero(ctx.formParam("edad"), 35);
      Integer dni = parsearEntero(ctx.formParam("dni"), 30000000);
      String genero = valor(ctx.formParam("genero"), "No especificado");
      String direccion = valor(ctx.formParam("direccion"), "CABA");
      donante = new PersonaHumana(medios, email, nombre, apellido, edad, dni, genero, direccion);
      nombreMostrar = (nombre + " " + apellido).trim();
    }

    donanteRepo.guardar(donante);
    if (usuarioRepo.buscarPorEmail(emailStr).isEmpty()) {
      usuarioRepo.guardar(new Usuario(emailStr, "donante123", nombreMostrar, RolUsuario.DONANTE, donante, null));
    }

    gestorNotificaciones.publicar(new EventoDeNotificacion(
        List.of(donante),
        "¡Bienvenido/a a DonaTrack! Tu usuario de acceso fue habilitado con el correo " + emailStr + "."
    ));

    ctx.redirect("/admin/deposito?msg=" + urlEncode("Donante '" + nombreMostrar + "' registrado y usuario otorgado"));
  }

  /** 13.b POST Registrar donación / carga de bienes en el depósito asociándola a la persona donante */
  public void adminRegistrarCargaDonacion(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long donanteId = parsearLong(ctx.formParam("donanteId"));
    if (donanteId == null) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("Seleccioná una persona donante"));
      return;
    }
    Optional<PersonaDonante> oDonante = donanteRepo.buscarPorId(donanteId);
    if (oDonante.isEmpty()) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("No existe el donante seleccionado"));
      return;
    }

    String descripcionGeneral = valor(ctx.formParam("descripcionGeneral"), "Ingreso por depósito");
    int cantidad = Math.max(1, parsearEntero(ctx.formParam("cantidad"), 10));
    String unidadMedida = valor(ctx.formParam("unidadMedida"), "unidades");
    String estadoBienStr = valor(ctx.formParam("esNuevo"), "true");
    String fechaVencStr = valor(ctx.formParam("fechaVencimiento"), "");

    // La subcategoria se elige del catalogo y viaja por id. Si no existe, la
    // donacion no se registra: antes se creaba una nueva en silencio y un
    // tipeo dejaba la donacion fuera del matchmaking sin ningun aviso.
    Optional<Subcategoria> oSub = buscarSubcategoriaDelFormulario(ctx);
    if (oSub.isEmpty()) {
      ctx.redirect("/admin/deposito?error=" + urlEncode(
          "Elegí una subcategoría del catálogo. Si la que necesitás no está, agregala con \"Nueva subcategoría\"."));
      return;
    }
    Subcategoria sub = oSub.get();
    // La categoria la define la subcategoria elegida, que es su unica fuente.
    Categoria cat = sub.getCategoria();
    boolean requiereEstado = (cat == Categoria.MOBILIARIO || cat == Categoria.VESTIMENTA);

    Bien bien;
    if (!fechaVencStr.isBlank()) {
      LocalDate venc = LocalDate.parse(fechaVencStr);
      bien = new BienPercibible(descripcionGeneral, null, sub, cantidad, unidadMedida, venc);
    } else if (requiereEstado) {
      bien = new BienConEstado(descripcionGeneral, null, sub, cantidad, unidadMedida, Boolean.parseBoolean(estadoBienStr));
    } else {
      bien = new BienSimple(descripcionGeneral, null, sub, cantidad, unidadMedida);
    }

    PersonaDonante donante = oDonante.get();
    donante.registrarInteraccion();
    donanteRepo.guardar(donante);

    CargaDeDonacion carga = new CargaDeDonacion(descripcionGeneral, LocalDate.now(), donante, new ArrayList<>(List.of(bien)));
    cargaRepo.guardar(carga);

    List<Donacion> segmentadas = carga.segmentar();
    for (Donacion d : segmentadas) {
      donacionRepo.guardar(d);
    }

    ctx.redirect("/admin/deposito?msg=" + urlEncode(
        "Carga registrada y segmentada en " + segmentadas.size() + " donación(es) en estado EN_DEPOSITO"));
  }

  /** 14. POST Actualizar el estado de una donación en depósito a Vencida */
  public void adminMarcarVencida(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long id = Long.valueOf(ctx.pathParam("id"));
    Optional<Donacion> oDonacion = donacionRepo.buscarPorId(id);
    if (oDonacion.isEmpty()) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("No existe la donación #" + id));
      return;
    }
    try {
      Donacion d = oDonacion.get();
      d.marcarVencida();
      donacionRepo.guardar(d);
      ctx.redirect("/admin/deposito?msg=" + urlEncode("Donación #" + id + " marcada como VENCIDA"));
    } catch (IllegalStateException e) {
      ctx.redirect("/admin/deposito?error=" + urlEncode(e.getMessage()));
    }
  }

  /** 15. Vista de Matchmaking y Asignación de Entidad Beneficiaria Final */
  public void adminMatchmaking(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    List<Donacion> enDeposito = donacionRepo.obtenerDonacionesEnDeposito();
    List<EntidadBeneficiaria> todasEntidades = entidadRepo.obtenerTodas();
    List<Donacion> historial = donacionRepo.obtenerTodas();

    List<Map<String, Object>> pendientesVista = new ArrayList<>();
    for (Donacion d : enDeposito) {
      Map<String, Object> item = mapearDonacionVista(d);
      PropuestaMatchmaking propuesta = propuestaRepo.buscarPorDonacion(d.getId())
          .orElseGet(() -> {
            PropuestaMatchmaking p = procesadorMatchmaking.procesar(d, todasEntidades, historial);
            propuestaRepo.guardar(p);
            return p;
          });
      item.put("esCoincidenciaExacta", propuesta.isEsCoincidenciaExacta());
      item.put("sugeridas", propuesta.getEntidadesSugeridas().stream().map(this::mapearEntidadVista).toList());
      pendientesVista.add(item);
    }

    int pagina = Math.max(1, parsearEntero(ctx.queryParam("page"), 1));
    Paginacion<Map<String, Object>> paginacion = new Paginacion<>(pendientesVista, pagina, 5, "/admin/matchmaking", Map.of());

    Map<String, Object> model = modeloBase(ctx, "Matchmaking y Asignación - DonaTrack");
    model.put("donacionesPendientes", paginacion.getItems());
    model.put("paginacion", paginacion);
    model.put("todasEntidades", todasEntidades);
    ctx.render("admin-matchmaking.hbs", model);
  }

  /** 15.a POST Ejecutar a demanda el algoritmo de matchmaking para una donación */
  public void adminEjecutarMatchmaking(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long id = Long.valueOf(ctx.pathParam("id"));
    donacionRepo.buscarPorId(id).ifPresent(d -> {
      PropuestaMatchmaking p = procesadorMatchmaking.procesar(
          d, entidadRepo.obtenerTodas(), donacionRepo.obtenerTodas()
      );
      propuestaRepo.guardar(p);
    });
    ctx.redirect("/admin/matchmaking?msg=" + urlEncode("Algoritmos de matchmaking ejecutados para la donación #" + id));
  }

  /** 15.b POST Seleccionar entidad beneficiaria final para una donación pendiente */
  public void adminAsignarEntidad(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long donacionId = Long.valueOf(ctx.pathParam("id"));
    Long entidadId = parsearLong(ctx.formParam("entidadBeneficiariaId"));

    Optional<Donacion> oDonacion = donacionRepo.buscarPorId(donacionId);
    Optional<EntidadBeneficiaria> oEntidad = entidadId != null ? entidadRepo.buscarPorId(entidadId) : Optional.empty();

    if (oDonacion.isEmpty() || oEntidad.isEmpty()) {
      ctx.redirect("/admin/matchmaking?error=" + urlEncode("Donación o entidad beneficiaria inválida"));
      return;
    }

    Donacion d = oDonacion.get();
    EntidadBeneficiaria entidad = oEntidad.get();
    try {
      d.asignarEntidad(entidad);
      donacionRepo.guardar(d);

      gestorNotificaciones.publicar(new EventoDeNotificacion(
          List.of(entidad),
          "Se te asignó la donación #" + d.getId() + " (" + d.getCantidad() + " " + d.getUnidadMedida()
              + " de " + (d.getSubcategoria() != null ? d.getSubcategoria().getNombre() : "bienes") + ")."
      ));
      if (d.getDonante() != null) {
        gestorNotificaciones.publicar(new EventoDeNotificacion(
            List.of(d.getDonante()),
            "Tu donación #" + d.getId() + " fue asignada a " + entidad.getRazonSocial() + ". ¡Gracias por tu aporte!"
        ));
      }

      ctx.redirect("/admin/matchmaking?msg=" + urlEncode(
          "Donación #" + donacionId + " asignada a '" + entidad.getRazonSocial() + "' y partes notificadas"));
    } catch (IllegalStateException e) {
      ctx.redirect("/admin/matchmaking?error=" + urlEncode(e.getMessage()));
    }
  }

  /** 16. Administrar los camiones disponibles y rutas (vía GraphQL con Logística) */
  public void adminCamiones(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Map<String, Object> model = modeloBase(ctx, "Flota de Camiones y Logística (GraphQL) - DonaTrack");
    model.put("camiones", logisticaPort.obtenerCamiones());
    model.put("rutas", logisticaPort.obtenerRutas());
    model.put("seguimiento", logisticaPort.obtenerSeguimientoCamiones());
    model.put("donacionesAsignadas", donacionRepo.buscarPorEstado(EstadoDonacion.ASIGNACION_REALIZADA).stream()
        .map(this::mapearDonacionVista).toList());
    ctx.render("admin-camiones.hbs", model);
  }

  /** 16.a POST Dar de alta un camión en Logística vía GraphQL */
  public void adminCrearCamion(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    String patente = valor(ctx.formParam("patente"), "AG-000-AA").trim().toUpperCase();
    double volumenM3 = parsearDouble(ctx.formParam("volumenM3"), 20.0);
    double alturaM = parsearDouble(ctx.formParam("alturaM"), 3.0);
    double capacidadKg = parsearDouble(ctx.formParam("capacidadKg"), 4500.0);

    logisticaPort.crearCamion(patente, volumenM3, alturaM, capacidadKg);
    ctx.redirect("/admin/camiones?msg=" + urlEncode("Camión " + patente + " registrado vía GraphQL en el servicio de Logística"));
  }

  /** 16.b POST Eliminar un camión en Logística vía GraphQL */
  public void adminEliminarCamion(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long id = Long.valueOf(ctx.pathParam("id"));
    logisticaPort.eliminarCamion(id);
    ctx.redirect("/admin/camiones?msg=" + urlEncode("Camión #" + id + " eliminado de la flota"));
  }

  /** 16.c POST Planificar ruta para una donación asignada e iniciar recorrido */
  public void adminCrearRuta(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long camionId = parsearLong(ctx.formParam("camionId"));
    Long donacionId = parsearLong(ctx.formParam("donacionId"));
    if (camionId == null || donacionId == null) {
      ctx.redirect("/admin/camiones?error=" + urlEncode("Seleccioná un camión y una donación asignada"));
      return;
    }

    donacionRepo.buscarPorId(donacionId).ifPresent(d -> {
      Long entidadId = d.getEntidadAsignada() != null ? d.getEntidadAsignada().getId() : 1L;
      String direccion = d.getEntidadAsignada() != null ? d.getEntidadAsignada().getDireccion() : "CABA";
      logisticaPort.crearRutaDirecta(camionId, entidadId, direccion, donacionId);
      try {
        if (d.getEstado() == EstadoDonacion.ASIGNACION_REALIZADA) {
          d.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
          donacionRepo.guardar(d);
        }
      } catch (IllegalStateException ignored) {}
    });

    ctx.redirect("/admin/camiones?msg=" + urlEncode("Ruta planificada vía GraphQL para la donación #" + donacionId));
  }

  /** 16.d POST Iniciar ruta de un camión */
  public void adminIniciarRuta(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long rutaId = Long.valueOf(ctx.pathParam("id"));
    // Las donaciones de ESTA ruta pasan a EN_TRASLADO cuando Logistica avisa con
    // notificarInicioRuta (DonacionesGraphQLController); aca no se tocan las de otras rutas.
    if (!logisticaPort.iniciarRuta(rutaId)) {
      ctx.redirect("/admin/camiones?error=" + urlEncode("No se pudo iniciar la ruta #" + rutaId
          + ": el camión ya está recorriendo otra ruta o esta ruta no está planificada."));
      return;
    }
    ctx.redirect("/admin/camiones?msg=" + urlEncode("Ruta #" + rutaId + " iniciada. Sus entregas están en camino."));
  }

  /** 16.e POST Actualizar posición GPS de un camión vía GraphQL */
  public void adminActualizarGps(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Long camionId = Long.valueOf(ctx.pathParam("id"));
    double lat = parsearDouble(ctx.formParam("latitud"), -34.5986);
    double lon = parsearDouble(ctx.formParam("longitud"), -58.4199);
    double vel = parsearDouble(ctx.formParam("velocidad"), 40.0);

    logisticaPort.reportarPosicionCamion(camionId, lat, lon, vel);
    ctx.redirect("/admin/camiones?msg=" + urlEncode("Posición GPS del camión #" + camionId + " actualizada vía GraphQL"));
  }

  /** 17. Vista para importar personas donantes desde archivos CSV (>10.000 filas) */
  public void adminImportarCsvVista(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    Map<String, Object> model = modeloBase(ctx, "Importación Masiva CSV (+10.000 filas) - DonaTrack");
    model.put("totalDonantesActuales", donanteRepo.obtenerTodos().size());
    model.put("totalUsuariosActuales", usuarioRepo.obtenerTodos().size());
    ctx.render("admin-csv.hbs", model);
  }

  /** 17.a POST Procesar subida de archivo CSV o generar e importar lote de 10.500 filas */
  public void adminProcesarCsv(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    String modoDemo10k = ctx.formParam("generarDemo10k");
    ImportadorDonantesCSV importador = new ImportadorDonantesCSV(donanteRepo, new LectorDonantesCSV());

    try {
      Reader reader;
      if ("true".equalsIgnoreCase(modoDemo10k)) {
        reader = new StringReader(construirCsv10500Filas());
      } else {
        UploadedFile archivo = ctx.uploadedFile("archivoCsv");
        if (archivo == null || archivo.size() == 0) {
          ctx.redirect("/admin/importar-csv?error=" + urlEncode("Seleccioná un archivo .csv válido"));
          return;
        }
        reader = new InputStreamReader(archivo.content(), StandardCharsets.UTF_8);
      }

      long inicio = System.currentTimeMillis();
      ImportadorDonantesCSV.ResultadoImportacion res = importador.importarMasivoConUsuarios(reader, usuarioRepo);
      long ms = System.currentTimeMillis() - inicio;

      // Se renderiza en vez de redirigir: el detalle de errores es una lista
      // y no puede viajar en un parametro de la URL.
      renderResultadoCsv(ctx, res, ms, null);
    } catch (Exception e) {
      renderResultadoCsv(ctx, null, 0L, "No se pudo procesar el archivo: " + e.getMessage());
    }
  }

  /** Muestra la pantalla de importacion con el resultado del ultimo intento. */
  private void renderResultadoCsv(Context ctx, ImportadorDonantesCSV.ResultadoImportacion res,
                                  long ms, String errorGeneral) {
    Map<String, Object> model = modeloBase(ctx, "Importación Masiva CSV (+10.000 filas) - DonaTrack");
    model.put("totalDonantesActuales", donanteRepo.obtenerTodos().size());
    model.put("totalUsuariosActuales", usuarioRepo.obtenerTodos().size());

    if (errorGeneral != null) {
      model.put("errorCsv", errorGeneral);
    }

    if (res != null) {
      Map<String, Object> resumen = new LinkedHashMap<>();
      resumen.put("milisegundos", ms);
      resumen.put("filasLeidas", res.filasLeidas());
      resumen.put("filasImportadas", res.filasImportadas());
      resumen.put("donantesCreados", res.donantesCreados());
      resumen.put("donantesActualizados", res.donantesActualizados());
      resumen.put("usuariosOtorgados", res.usuariosOtorgados());
      resumen.put("filasConError", res.filasConError());
      resumen.put("tuvoErrores", res.tuvoErrores());
      model.put("resultado", resumen);

      List<Map<String, Object>> errores = new ArrayList<>();
      for (ImportadorDonantesCSV.ErrorDeFila err : res.errores()) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("linea", err.linea());
        fila.put("referencia", err.referencia());
        fila.put("motivo", err.motivo());
        errores.add(fila);
      }
      if (!errores.isEmpty()) {
        model.put("errores", errores);
      }
    }

    ctx.render("admin-csv.hbs", model);
  }

  /** 17.b GET Descargar un archivo CSV de prueba con 10.500 filas válidas */
  public void adminDescargarCsvEjemplo10k(Context ctx) {
    String contenido = construirCsv10500Filas();
    ctx.header("Content-Disposition", "attachment; filename=\"donantes_10500_filas.csv\"");
    ctx.contentType("text/csv; charset=UTF-8");
    ctx.result(contenido);
  }

  // =========================================================================
  // HELPERS DE MAPEO DE VISTAS Y COORDENADAS PARA MAPAS INTERACTIVOS
  // =========================================================================

  /**
   * Resuelve la subcategoria elegida en un formulario. Acepta un id del
   * catalogo (o nombre validado contra el catalogo y su categoria): no crea nada.
   * Devolver Optional vacio y que el controlador redirija con un mensaje es
   * deliberado, para que el dato invalido no entre.
   */
  private Optional<Subcategoria> buscarSubcategoriaDelFormulario(Context ctx) {
    Long id = parsearIdOpcional(ctx.formParam("subcategoriaId"));
    if (id != null) {
      return subcategoriaRepo.buscarPorId(id);
    }
    String nombre = valor(ctx.formParam("subcategoriaNombre"), "").trim();
    if (!nombre.isBlank()) {
      Optional<Subcategoria> oSub = subcategoriaRepo.buscarPorNombre(nombre);
      if (oSub.isPresent()) {
        Categoria cat = parsearEnum(Categoria.class, ctx.formParam("categoria"));
        if (cat != null && oSub.get().getCategoria() != cat) {
          return Optional.empty();
        }
      }
      return oSub;
    }
    return Optional.empty();
  }

  /**
   * 13.c Alta explicita de subcategoria. Dar de alta una entrada del catalogo
   * es un acto deliberado del administrador, no un efecto lateral de un tipeo.
   */
  public void adminCrearSubcategoria(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    String nombre = valor(ctx.formParam("nombre"), "").trim();
    Categoria categoria = parsearEnum(Categoria.class, ctx.formParam("categoria"));

    if (nombre.isBlank() || categoria == null) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("Indicá un nombre y una categoría para la subcategoría."));
      return;
    }

    Optional<Subcategoria> yaExiste = subcategoriaRepo.buscarPorNombre(nombre);
    if (yaExiste.isPresent()) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("Ya existe una subcategoría llamada \"" + nombre + "\"."));
      return;
    }

    boolean requiereEstado = (categoria == Categoria.MOBILIARIO || categoria == Categoria.VESTIMENTA);
    boolean esPerecedero = (categoria == Categoria.ALIMENTOS);
    subcategoriaRepo.guardar(new Subcategoria(nombre, categoria, requiereEstado, esPerecedero));

    ctx.redirect("/admin/deposito?msg=" + urlEncode("Subcategoría \"" + nombre + "\" agregada al catálogo."));
  }

  /**
   * JSON que se incrusta dentro de un <script> con {{{ }}} (sin escapar). Jackson no escapa
   * "<", asi que un nombre cargado como "</script><script>..." cortaria el bloque y ejecutaria
   * codigo ajeno (XSS). Escapado como secuencia unicode, el navegador lo lee igual como "<"
   * pero no corta el script.
   */
  private String jsonParaScript(Object datos) throws com.fasterxml.jackson.core.JsonProcessingException {
    return objectMapper.writeValueAsString(datos)
        .replace("<", "\\u003c")
        .replace(">", "\\u003e")
        .replace("&", "\\u0026");
  }

  /** La entidad confirma lo que recibio: solo donaciones en camino (EN_TRASLADO). */
  private boolean puedeConfirmarRecepcion(Donacion d) {
    return d.getEstado() == EstadoDonacion.EN_TRASLADO;
  }

  private Map<String, Object> modeloBase(Context ctx, String tituloPagina) {
    Map<String, Object> model = new HashMap<>();
    model.put("tituloPagina", tituloPagina);
    model.put("msg", ctx.queryParam("msg"));
    model.put("error", ctx.queryParam("error"));
    model.put("aviso", ctx.queryParam("aviso"));
    // Lo usa el layout para marcar en que seccion esta parada la persona.
    model.put("rutaActual", ctx.path());

    // Protección CSRF: token seguro de sesión inyectado en cada vista HTML
    String csrfToken = sesionManager.obtenerOCrearCsrfToken(ctx);
    model.put("csrfToken", csrfToken);

    sesionManager.obtenerUsuarioActual(ctx).ifPresent(u -> {
      model.put("usuarioActual", u);
      model.put("nombreUsuario", u.getNombreMostrar());
      model.put("emailUsuario", u.getEmail());
      model.put("rolUsuario", u.getRol().name());
      model.put("esAdmin", u.getRol() == RolUsuario.ADMIN);
      model.put("esDonante", u.getRol() == RolUsuario.DONANTE);
      model.put("esEntidad", u.getRol() == RolUsuario.ENTIDAD);
    });
    return model;
  }

  private EntidadBeneficiaria resolverEntidadDeUsuario(Usuario u) {
    if (u.getEntidadBeneficiaria() != null) {
      return entidadRepo.buscarPorId(u.getEntidadBeneficiaria().getId()).orElse(u.getEntidadBeneficiaria());
    }
    List<EntidadBeneficiaria> todas = entidadRepo.obtenerTodas();
    return todas.isEmpty() ? null : todas.get(0);
  }

  private List<Map<String, Object>> armarEntregasEntregadasVista() {
    List<Donacion> entregadas = donacionRepo.buscarPorEstado(EstadoDonacion.ENTREGADA);
    List<LogisticaPort.EntregaVista> entregasLogistica = logisticaPort.obtenerEntregas();

    List<Map<String, Object>> resultado = new ArrayList<>();
    int idx = 0;
    for (Donacion d : entregadas) {
      Map<String, Object> m = mapearDonacionVista(d);
      Optional<LogisticaPort.EntregaVista> evOpt = entregasLogistica.stream()
          .filter(e -> d.getId().equals(e.donacionId()))
          .findFirst();

      // Si la entrega tiene fotos cargadas se muestran esas; si no, una foto ilustrativa
      // para que la tarjeta de la galeria no quede vacia.
      List<String> fotos = evOpt.map(LogisticaPort.EntregaVista::fotosUrl)
          .filter(f -> f != null && !f.isEmpty())
          .orElse(List.of(FOTOS_ILUSTRATIVAS_DONACION.get(idx % FOTOS_ILUSTRATIVAS_DONACION.size())));
      m.put("fotos", fotos);
      m.put("fotoPrincipal", fotos.get(0));
      m.put("patenteCamion", evOpt.map(LogisticaPort.EntregaVista::patenteCamion).orElse(null));
      m.put("fechaEntrega", evOpt.map(LogisticaPort.EntregaVista::fechaHoraEntrega)
          .orElse(d.getFechaRegistro() != null ? d.getFechaRegistro().toString() : "Reciente"));

      double[] coords = coordenadasParaIndice(d.getEntidadAsignada() != null ? d.getEntidadAsignada().getId() : idx);
      m.put("latitud", coords[0]);
      m.put("longitud", coords[1]);
      idx++;
      resultado.add(m);
    }
    return resultado;
  }

  private void poblarModeloSeguimientoMapa(Map<String, Object> model, List<Donacion> activas) {
    List<LogisticaPort.SeguimientoCamionVista> camiones = logisticaPort.obtenerSeguimientoCamiones();
    List<Map<String, Object>> entregasActivasVista = new ArrayList<>();

    for (Donacion d : activas) {
      Map<String, Object> item = mapearDonacionVista(d);
      LogisticaPort.SeguimientoCamionVista camionAsociado = camiones.stream()
          .filter(c -> c.donacionesIds() != null && c.donacionesIds().contains(d.getId()))
          .findFirst()
          .orElse(camiones.isEmpty() ? null : camiones.get(0));

      if (camionAsociado != null) {
        item.put("patenteCamion", camionAsociado.patente());
        item.put("latitudCamion", camionAsociado.latitud());
        item.put("longitudCamion", camionAsociado.longitud());
        item.put("velocidadCamion", camionAsociado.velocidad());
        item.put("ultimaActualizacion", camionAsociado.ultimaActualizacion());
        item.put("avancePorcentaje", camionAsociado.avancePorcentaje());
      } else {
        item.put("patenteCamion", "AE-412-UT");
        item.put("latitudCamion", -34.5986);
        item.put("longitudCamion", -58.4199);
        item.put("velocidadCamion", 36.0);
        item.put("ultimaActualizacion", LocalDate.now().toString());
        item.put("avancePorcentaje", 50);
      }

      double[] destinoCoords = coordenadasParaIndice(
          d.getEntidadAsignada() != null ? d.getEntidadAsignada().getId() : d.getId());
      item.put("latitudDestino", destinoCoords[0]);
      item.put("longitudDestino", destinoCoords[1]);
      entregasActivasVista.add(item);
    }

    model.put("entregasActivas", entregasActivasVista);
    model.put("camionesActivos", camiones);
    try {
      model.put("entregasActivasJson", jsonParaScript(entregasActivasVista));
    } catch (Exception e) {
      model.put("entregasActivasJson", "[]");
    }
  }

  private Map<String, Object> mapearDonacionVista(Donacion d) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", d.getId());
    m.put("cantidad", d.getCantidad());
    m.put("unidadMedida", d.getUnidadMedida());
    m.put("estado", d.getEstado() != null ? d.getEstado().name() : "EN_DEPOSITO");
    m.put("esEnDeposito", d.getEstado() == EstadoDonacion.EN_DEPOSITO);
    m.put("esEntregada", d.getEstado() == EstadoDonacion.ENTREGADA);
    m.put("puedeConfirmar", puedeConfirmarRecepcion(d));
    m.put("fechaRegistro", d.getFechaRegistro() != null ? d.getFechaRegistro().toString() : "");
    m.put("subcategoriaNombre", d.getSubcategoria() != null ? d.getSubcategoria().getNombre() : "General");
    m.put("categoriaNombre", (d.getSubcategoria() != null && d.getSubcategoria().getCategoria() != null)
        ? d.getSubcategoria().getCategoria().name() : "ALIMENTOS");
    m.put("donanteNombre", nombreDeDonante(d.getDonante()));
    m.put("entidadNombre", d.getEntidadAsignada() != null ? d.getEntidadAsignada().getRazonSocial() : "Sin asignar");
    m.put("entidadDireccion", d.getEntidadAsignada() != null ? d.getEntidadAsignada().getDireccion() : "Depósito Central UTN");
    m.put("justificacionFallida", d.getJustificacionFallida());

    List<Map<String, String>> historialVista = d.getHistorial().stream().map(c -> Map.of(
        "estado", c.getEstado().name(),
        "fechaHora", c.getFechaHora() != null
            ? c.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
    )).toList();
    m.put("historial", historialVista);
    return m;
  }

  private Map<String, Object> mapearEntidadVista(EntidadBeneficiaria e) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", e.getId());
    m.put("razonSocial", e.getRazonSocial());
    m.put("direccion", e.getDireccion());
    m.put("telefono", e.getTelefono());
    m.put("emailPrincipal", (e.getEmailsRepresentantes() != null && !e.getEmailsRepresentantes().isEmpty())
        ? e.getEmailsRepresentantes().get(0) : "");

    List<Map<String, Object>> necs = new ArrayList<>();
    int totalPendientes = 0;
    int totalSatisfechas = 0;
    if (e.getNecesidades() != null) {
      for (Necesidad n : e.getNecesidades()) {
        Map<String, Object> nm = new HashMap<>();
        nm.put("id", n.getId());
        nm.put("descripcion", n.getDescripcion());
        nm.put("subcategoriaNombre", n.getSubcategoria() != null ? n.getSubcategoria().getNombre() : "General");
        String catNom = (n.getSubcategoria() != null && n.getSubcategoria().getCategoria() != null)
            ? n.getSubcategoria().getCategoria().name() : "ALIMENTOS";
        nm.put("categoriaNombre", catNom);
        boolean satisfecha = Boolean.TRUE.equals(n.estaSatisfecha());
        nm.put("satisfecha", satisfecha);
        if (satisfecha) {
          totalSatisfechas++;
        } else {
          totalPendientes++;
        }

        int objetivo = 0;
        int recibida = 0;
        if (n instanceof NecesidadExtraordinaria ne) {
          nm.put("tipo", "EXTRAORDINARIA");
          objetivo = ne.getCantidadRequerida();
          recibida = ne.getCantidadRecibida();
          nm.put("detallePeriodo", "Emergencia / Puntual");
        } else if (n instanceof NecesidadRecurrente nr) {
          PeriodoConsumo pc = nr.getPeriodoConsumo();
          nm.put("tipo", "RECURRENTE");
          objetivo = pc != null ? pc.getCantidadObjetivo() : 0;
          recibida = pc != null ? pc.getCantidadRecibida() : 0;
          nm.put("detallePeriodo", pc != null && pc.getDescripcion() != null ? pc.getDescripcion() : "PERIÓDICA");
        }
        nm.put("objetivo", objetivo);
        nm.put("recibida", recibida);
        int faltante = Math.max(0, objetivo - recibida);
        nm.put("faltante", faltante);
        int porcentaje = objetivo > 0 ? Math.min(100, (int) Math.round(((double) recibida / objetivo) * 100)) : 100;
        nm.put("porcentaje", porcentaje);
        necs.add(nm);
      }
    }
    m.put("necesidades", necs);
    m.put("totalNecesidades", necs.size());
    m.put("totalPendientes", totalPendientes);
    m.put("totalSatisfechas", totalSatisfechas);
    return m;
  }

  private Map<String, Object> mapearDonanteVista(PersonaDonante d) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", d.getId());
    m.put("nombre", nombreDeDonante(d));
    m.put("tipo", d instanceof PersonaJuridica ? "JURIDICA" : "HUMANA");
    m.put("email", d.getMedioDeContactoPredeterminado() != null ? d.getMedioDeContactoPredeterminado().getValor() : "");
    m.put("ultimaInteraccion", d.getUltimaInteraccion() != null ? d.getUltimaInteraccion().toString() : "");
    return m;
  }

  private String nombreDeDonante(PersonaDonante d) {
    if (d == null) return "Anónimo";
    if (d instanceof PersonaHumana h) {
      return ((h.getNombre() != null ? h.getNombre() : "") + " " + (h.getApellido() != null ? h.getApellido() : "")).trim();
    }
    if (d instanceof PersonaJuridica j) {
      return j.getRazonSocial() != null ? j.getRazonSocial() : "Organización";
    }
    return "Donante #" + d.getId();
  }

  private double[] coordenadasParaIndice(long seed) {
    double[][] puntos = {
        {-34.5986, -58.4199}, // UTN Medrano / Almagro
        {-34.6595, -58.4679}, // UTN Campus / Villa Lugano
        {-34.7611, -58.3975}, // Hospital Gandulfo / Lomas de Zamora
        {-34.6083, -58.3712}, // Plaza de Mayo / Centro
        {-34.5885, -58.4305}  // Palermo
    };
    int pos = (int) (Math.abs(seed) % puntos.length);
    return puntos[pos];
  }

  private String construirCsv10500Filas() {
    StringBuilder sb = new StringBuilder(650_000);
    sb.append("TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n");
    for (int i = 1; i <= 10500; i++) {
      if (i % 4 == 0) {
        sb.append("JURIDICA,CUIT,30-")
            .append(String.format("%08d", 10000000 + i))
            .append("-9,Fundación Solidaria ")
            .append(i)
            .append(",org")
            .append(i)
            .append("@donatrack.org,+54 11 4000-")
            .append(String.format("%04d", i % 10000))
            .append("\n");
      } else {
        sb.append("HUMANA,DNI,")
            .append(30000000 + i)
            .append(",Donante Solidario ")
            .append(i)
            .append(",donante")
            .append(i)
            .append("@mail.com,+54 11 5000-")
            .append(String.format("%04d", i % 10000))
            .append("\n");
      }
    }
    return sb.toString();
  }

  private String valor(String v, String defecto) {
    return (v != null && !v.isBlank()) ? v : defecto;
  }

  private Integer parsearEntero(String s, int defecto) {
    try {
      return s != null && !s.isBlank() ? Integer.parseInt(s.trim()) : defecto;
    } catch (NumberFormatException e) {
      return defecto;
    }
  }

  private Long parsearLong(String s) {
    try {
      return s != null && !s.isBlank() ? Long.parseLong(s.trim()) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private double parsearDouble(String s, double defecto) {
    try {
      return s != null && !s.isBlank() ? Double.parseDouble(s.trim()) : defecto;
    } catch (NumberFormatException e) {
      return defecto;
    }
  }

  private String urlEncode(String texto) {
    return URLEncoder.encode(texto, StandardCharsets.UTF_8);
  }

  /**
   * Helper generico de paginacion para el Cliente Liviano MVC (Requerimiento oral).
   */
  public static class Paginacion<T> {
    private final List<T> items;
    private final int paginaActual;
    private final int totalPaginas;
    private final int totalItems;
    private final boolean tieneAnterior;
    private final boolean tieneSiguiente;
    private final int paginaAnterior;
    private final int paginaSiguiente;
    private final String urlAnterior;
    private final String urlSiguiente;
    private final boolean mostrarPaginacion;
    private final List<Map<String, Object>> paginas;
    private final int rangoInicio;
    private final int rangoFin;

    public Paginacion(List<T> listaCompleta, int paginaActual, int pageSize, String urlBase, Map<String, String> params) {
      this.totalItems = listaCompleta.size();
      int calculadas = (int) Math.ceil((double) totalItems / pageSize);
      this.totalPaginas = Math.max(1, calculadas);
      this.paginaActual = Math.max(1, Math.min(paginaActual, this.totalPaginas));

      int inicio = (this.paginaActual - 1) * pageSize;
      int fin = Math.min(inicio + pageSize, totalItems);
      this.items = (inicio < totalItems) ? listaCompleta.subList(inicio, fin) : Collections.emptyList();
      this.rangoInicio = totalItems == 0 ? 0 : inicio + 1;
      this.rangoFin = fin;

      this.tieneAnterior = this.paginaActual > 1;
      this.tieneSiguiente = this.paginaActual < this.totalPaginas;
      this.paginaAnterior = this.paginaActual - 1;
      this.paginaSiguiente = this.paginaActual + 1;
      this.mostrarPaginacion = this.totalPaginas > 1;

      this.urlAnterior = armarUrl(urlBase, params, this.paginaAnterior);
      this.urlSiguiente = armarUrl(urlBase, params, this.paginaSiguiente);

      this.paginas = new ArrayList<>();
      for (int i = 1; i <= this.totalPaginas; i++) {
        Map<String, Object> p = new HashMap<>();
        p.put("numero", i);
        p.put("activa", i == this.paginaActual);
        p.put("url", armarUrl(urlBase, params, i));
        this.paginas.add(p);
      }
    }

    private static String armarUrl(String base, Map<String, String> params, int page) {
      StringBuilder sb = new StringBuilder(base).append("?");
      if (params != null) {
        for (Map.Entry<String, String> e : params.entrySet()) {
          if (e.getKey() != null && !e.getKey().equals("page") && e.getValue() != null && !e.getValue().isBlank()) {
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .append("&");
          }
        }
      }
      sb.append("page=").append(page);
      return sb.toString();
    }

    public List<T> getItems() { return items; }
    public int getPaginaActual() { return paginaActual; }
    public int getTotalPaginas() { return totalPaginas; }
    public int getTotalItems() { return totalItems; }
    public boolean isTieneAnterior() { return tieneAnterior; }
    public boolean isTieneSiguiente() { return tieneSiguiente; }
    public int getPaginaAnterior() { return paginaAnterior; }
    public int getPaginaSiguiente() { return paginaSiguiente; }
    public String getUrlAnterior() { return urlAnterior; }
    public String getUrlSiguiente() { return urlSiguiente; }
    public boolean isMostrarPaginacion() { return mostrarPaginacion; }
    public List<Map<String, Object>> getPaginas() { return paginas; }
    public int getRangoInicio() { return rangoInicio; }
    public int getRangoFin() { return rangoFin; }
  }
}
