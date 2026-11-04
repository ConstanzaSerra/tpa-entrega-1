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
    Map<String, Object> model = modeloBase(ctx, "Galería de Donaciones Entregadas - DonaTrack");
    model.put("entregas", armarEntregasEntregadasVista());
    ctx.render("galeria.hbs", model);
  }

  /** 2. Mapa interactivo público de donaciones ya entregadas */
  public void mapaEntregasPublico(Context ctx) {
    Map<String, Object> model = modeloBase(ctx, "Mapa de Donaciones Entregadas - DonaTrack");
    List<Map<String, Object>> entregas = armarEntregasEntregadasVista();
    model.put("entregas", entregas);
    try {
      model.put("marcadoresJson", objectMapper.writeValueAsString(entregas));
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
    String password = valor(ctx.formParam("password"), "donante123");
    String telefonoStr = valor(ctx.formParam("telefono"), "+54 11 0000-0000");
    String whatsappStr = valor(ctx.formParam("whatsapp"), "");
    String medioPreferido = valor(ctx.formParam("medioPreferido"), "EMAIL");

    if (emailStr.isBlank()) {
      ctx.redirect("/registro?error=" + urlEncode("El correo electrónico es obligatorio"));
      return;
    }
    if (usuarioRepo.buscarPorEmail(emailStr).isPresent()) {
      ctx.redirect("/registro?error=" + urlEncode("Ya existe un usuario registrado con el correo " + emailStr));
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
    String password = valor(ctx.formParam("password"), "entidad123");

    if (razonSocial.isBlank() || emailStr.isBlank()) {
      ctx.redirect("/registro?error=" + urlEncode("Completá razón social y correo electrónico"));
      return;
    }
    if (usuarioRepo.buscarPorEmail(emailStr).isPresent()) {
      ctx.redirect("/registro?error=" + urlEncode("Ya existe un usuario con el correo " + emailStr));
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

  /** 5. Filtrar sus donaciones realizadas por estado y categoría o subcategoría/s */
  public void donanteMisDonaciones(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    String filtroEstado = valor(ctx.queryParam("estado"), "");
    String filtroCategoria = valor(ctx.queryParam("categoria"), "");
    String filtroSubcategoria = valor(ctx.queryParam("subcategoria"), "").trim();

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

    Map<String, Object> model = modeloBase(ctx, "Mis Donaciones - DonaTrack");
    model.put("donaciones", filtradas.stream().map(this::mapearDonacionVista).toList());
    model.put("filtroEstado", filtroEstado);
    model.put("filtroCategoria", filtroCategoria);
    model.put("filtroSubcategoria", filtroSubcategoria);
    model.put("subcategoriasDisponibles", subcategoriaRepo.obtenerTodas());
    ctx.render("donante-donaciones.hbs", model);
  }

  /** 6. Navegar por las entidades beneficiarias registradas */
  public void donanteEntidades(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.DONANTE, RolUsuario.ADMIN);
    if (u == null) return;

    String busqueda = valor(ctx.queryParam("q"), "").trim();
    List<EntidadBeneficiaria> entidades = entidadRepo.obtenerTodas().stream()
        .filter(e -> busqueda.isBlank()
            || (e.getRazonSocial() != null && e.getRazonSocial().toLowerCase().contains(busqueda.toLowerCase()))
            || (e.getDireccion() != null && e.getDireccion().toLowerCase().contains(busqueda.toLowerCase())))
        .toList();

    Map<String, Object> model = modeloBase(ctx, "Entidades Beneficiarias - DonaTrack");
    model.put("entidades", entidades.stream().map(this::mapearEntidadVista).toList());
    model.put("busqueda", busqueda);
    ctx.render("donante-entidades.hbs", model);
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

  /** 8. Vista y registro de necesidades materiales concretas */
  public void entidadNecesidades(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    EntidadBeneficiaria entidad = resolverEntidadDeUsuario(u);
    Map<String, Object> model = modeloBase(ctx, "Necesidades Materiales - DonaTrack");
    if (entidad != null) {
      model.put("entidad", mapearEntidadVista(entidad));
    }
    model.put("subcategorias", subcategoriaRepo.obtenerTodas());
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
    String categoriaStr = valor(ctx.formParam("categoria"), "ALIMENTOS");
    String subcategoriaNombre = valor(ctx.formParam("subcategoriaNombre"), "Alimentos generales").trim();
    String descripcion = valor(ctx.formParam("descripcion"), "Necesidad registrada en plataforma").trim();
    int cantidad = Math.max(1, parsearEntero(ctx.formParam("cantidad"), 10));
    String periodoStr = valor(ctx.formParam("periodo"), "MENSUAL");

    if (descripcion.isBlank()) {
      descripcion = "Demanda de " + subcategoriaNombre;
    }

    Categoria cat;
    try {
      cat = Categoria.valueOf(categoriaStr);
    } catch (Exception e) {
      cat = Categoria.ALIMENTOS;
    }

    Subcategoria sub = subcategoriaRepo.obtenerOCrear(
        subcategoriaNombre, cat, cat != Categoria.ALIMENTOS, cat == Categoria.ALIMENTOS
    );

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

  /** 9 y 10. Ver estado de las donaciones asignadas y confirmar recepción con fotos */
  public void entidadDonaciones(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ENTIDAD, RolUsuario.ADMIN);
    if (u == null) return;

    EntidadBeneficiaria entidad = resolverEntidadDeUsuario(u);
    List<Donacion> asignadas = (entidad != null)
        ? donacionRepo.buscarPorEntidadAsignada(entidad.getId())
        : donacionRepo.obtenerTodas();

    Map<String, Object> model = modeloBase(ctx, "Donaciones Asignadas - DonaTrack");
    model.put("entidadNombre", entidad != null ? entidad.getRazonSocial() : "Todas las Entidades");
    model.put("donaciones", asignadas.stream().map(this::mapearDonacionVista).toList());
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
    String fotoFinal = valor(ctx.formParam("fotoUrl"), "").trim();

    UploadedFile archivo = ctx.uploadedFile("fotoArchivo");
    if (archivo != null && archivo.size() > 0) {
      try {
        byte[] bytes = archivo.content().readAllBytes();
        String mime = archivo.contentType() != null ? archivo.contentType() : "image/jpeg";
        fotoFinal = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
      } catch (Exception ignored) {}
    }

    if (fotoFinal.isBlank()) {
      fotoFinal = "https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=800&q=80";
    }

    // Avanzar estado de la donación hasta ENTREGADA garantizando trazabilidad
    try {
      if (d.getEstado() == EstadoDonacion.ASIGNACION_REALIZADA) {
        d.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
      }
      if (d.getEstado() == EstadoDonacion.LISTA_PARA_ENTREGAR) {
        d.avanzarHacia(EstadoDonacion.EN_TRASLADO);
      }
      if (d.getEstado() == EstadoDonacion.EN_TRASLADO) {
        d.avanzarHacia(EstadoDonacion.ENTREGADA);
      }
      donacionRepo.guardar(d);
    } catch (IllegalStateException ignored) {}

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

    Map<String, Object> model = modeloBase(ctx, "Administración de Depósito - DonaTrack");
    model.put("donantes", donanteRepo.obtenerTodos().stream().map(this::mapearDonanteVista).toList());
    model.put("donaciones", donacionRepo.obtenerTodas().stream()
        .sorted(Comparator.comparing(Donacion::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::mapearDonacionVista)
        .toList());
    model.put("subcategorias", subcategoriaRepo.obtenerTodas());
    ctx.render("admin-deposito.hbs", model);
  }

  /** 13.a POST Registrar persona donante desde el panel administrador */
  public void adminCrearDonante(Context ctx) {
    Usuario u = sesionManager.exigirRol(ctx, RolUsuario.ADMIN);
    if (u == null) return;

    String tipo = valor(ctx.formParam("tipo"), "HUMANA");
    String emailStr = valor(ctx.formParam("email"), "").trim().toLowerCase();
    String telefonoStr = valor(ctx.formParam("telefono"), "+54 11 0000-0000");

    if (emailStr.isBlank()) {
      ctx.redirect("/admin/deposito?error=" + urlEncode("El email del donante es obligatorio"));
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
    String categoriaStr = valor(ctx.formParam("categoria"), "ALIMENTOS");
    String subcategoriaNombre = valor(ctx.formParam("subcategoriaNombre"), "Alimentos").trim();
    int cantidad = Math.max(1, parsearEntero(ctx.formParam("cantidad"), 10));
    String unidadMedida = valor(ctx.formParam("unidadMedida"), "unidades");
    String estadoBienStr = valor(ctx.formParam("esNuevo"), "true");
    String fechaVencStr = valor(ctx.formParam("fechaVencimiento"), "");

    Categoria cat;
    try {
      cat = Categoria.valueOf(categoriaStr);
    } catch (Exception e) {
      cat = Categoria.ALIMENTOS;
    }

    boolean requiereEstado = (cat == Categoria.MOBILIARIO || cat == Categoria.VESTIMENTA);
    boolean esPerecedero = (cat == Categoria.ALIMENTOS && !fechaVencStr.isBlank());
    Subcategoria sub = subcategoriaRepo.obtenerOCrear(subcategoriaNombre, cat, requiereEstado, esPerecedero);

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

    Map<String, Object> model = modeloBase(ctx, "Matchmaking y Asignación - DonaTrack");
    model.put("donacionesPendientes", pendientesVista);
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
    logisticaPort.iniciarRuta(rutaId);
    donacionRepo.buscarPorEstado(EstadoDonacion.LISTA_PARA_ENTREGAR).forEach(d -> {
      try {
        d.avanzarHacia(EstadoDonacion.EN_TRASLADO);
        donacionRepo.guardar(d);
      } catch (IllegalStateException ignored) {}
    });
    ctx.redirect("/admin/camiones?msg=" + urlEncode("Ruta #" + rutaId + " iniciada. Las entregas pasaron a EN_TRASLADO."));
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

      String resumen = String.format(
          "Importación finalizada en %d ms: %d filas procesadas (%d donantes nuevos, %d actualizados, %d usuarios otorgados).",
          ms, res.filasLeidas(), res.donantesCreados(), res.donantesActualizados(), res.usuariosOtorgados()
      );
      ctx.redirect("/admin/importar-csv?msg=" + urlEncode(resumen));
    } catch (Exception e) {
      ctx.redirect("/admin/importar-csv?error=" + urlEncode("Error procesando CSV: " + e.getMessage()));
    }
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

  private Map<String, Object> modeloBase(Context ctx, String tituloPagina) {
    Map<String, Object> model = new HashMap<>();
    model.put("tituloPagina", tituloPagina);
    model.put("msg", ctx.queryParam("msg"));
    model.put("error", ctx.queryParam("error"));

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

      List<String> fotos = evOpt.map(LogisticaPort.EntregaVista::fotosUrl)
          .filter(f -> !f.isEmpty())
          .orElse(List.of("https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=800&q=80"));
      m.put("fotos", fotos);
      m.put("fotoPrincipal", fotos.get(0));
      m.put("patenteCamion", evOpt.map(LogisticaPort.EntregaVista::patenteCamion).orElse("AE-412-UT"));
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
      model.put("entregasActivasJson", objectMapper.writeValueAsString(entregasActivasVista));
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
    m.put("puedeConfirmar", d.getEstado() != EstadoDonacion.ENTREGADA && d.getEstado() != EstadoDonacion.VENCIDA);
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
    if (e.getNecesidades() != null) {
      for (Necesidad n : e.getNecesidades()) {
        Map<String, Object> nm = new HashMap<>();
        nm.put("id", n.getId());
        nm.put("descripcion", n.getDescripcion());
        nm.put("subcategoriaNombre", n.getSubcategoria() != null ? n.getSubcategoria().getNombre() : "General");
        nm.put("categoriaNombre", (n.getSubcategoria() != null && n.getSubcategoria().getCategoria() != null)
            ? n.getSubcategoria().getCategoria().name() : "ALIMENTOS");
        nm.put("satisfecha", Boolean.TRUE.equals(n.estaSatisfecha()));
        if (n instanceof NecesidadExtraordinaria ne) {
          nm.put("tipo", "EXTRAORDINARIA");
          nm.put("objetivo", ne.getCantidadRequerida());
          nm.put("recibida", ne.getCantidadRecibida());
          nm.put("detallePeriodo", "Emergencia / Puntual");
        } else if (n instanceof NecesidadRecurrente nr) {
          PeriodoConsumo pc = nr.getPeriodoConsumo();
          nm.put("tipo", "RECURRENTE");
          nm.put("objetivo", pc != null ? pc.getCantidadObjetivo() : 0);
          nm.put("recibida", pc != null ? pc.getCantidadRecibida() : 0);
          nm.put("detallePeriodo", pc != null && pc.getDescripcion() != null ? pc.getDescripcion() : "PERIÓDICA");
        }
        necs.add(nm);
      }
    }
    m.put("necesidades", necs);
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
}
