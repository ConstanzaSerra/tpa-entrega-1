package ar.edu.utn.frba.dds.donaciones.web;

import ar.edu.utn.frba.dds.donaciones.domain.RolUsuario;
import ar.edu.utn.frba.dds.donaciones.domain.Usuario;
import ar.edu.utn.frba.dds.donaciones.repository.UsuarioRepository;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manejo de sesión y autenticación basado en Cookies HTTP (Entrega 4).
 * Asocia el token de la cookie HTTP "donatrack_session" con el Usuario autenticado.
 */
public class SesionManager {

  public static final String COOKIE_SESION = "donatrack_session";
  private static final int DURACION_SEGUNDOS = 60 * 60 * 24; // 24 horas

  private final Map<String, Long> sesionesActivas = new ConcurrentHashMap<>();
  private final UsuarioRepository usuarioRepository;

  public SesionManager(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public void iniciarSesion(Context ctx, Usuario usuario) {
    String token = UUID.randomUUID().toString();
    sesionesActivas.put(token, usuario.getId());

    Cookie cookie = new Cookie(COOKIE_SESION, token);
    cookie.setPath("/");
    cookie.setMaxAge(DURACION_SEGUNDOS);
    cookie.setHttpOnly(true);
    cookie.setSameSite(SameSite.LAX);
    ctx.cookie(cookie);
  }

  public void cerrarSesion(Context ctx) {
    String token = ctx.cookie(COOKIE_SESION);
    if (token != null) {
      sesionesActivas.remove(token);
    }
    ctx.removeCookie(COOKIE_SESION, "/");
  }

  public Optional<Usuario> obtenerUsuarioActual(Context ctx) {
    String token = ctx.cookie(COOKIE_SESION);
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    Long usuarioId = sesionesActivas.get(token);
    if (usuarioId == null) {
      return Optional.empty();
    }
    return usuarioRepository.buscarPorId(usuarioId);
  }

  public Usuario exigirRol(Context ctx, RolUsuario... rolesPermitidos) {
    Optional<Usuario> oUsuario = obtenerUsuarioActual(ctx);
    if (oUsuario.isEmpty()) {
      ctx.redirect("/login?msg=Iniciá+sesión+para+acceder+a+esta+sección");
      return null;
    }
    Usuario u = oUsuario.get();
    if (rolesPermitidos != null && rolesPermitidos.length > 0) {
      for (RolUsuario rol : rolesPermitidos) {
        if (u.getRol() == rol) {
          return u;
        }
      }
      ctx.redirect("/?error=No+tenés+permisos+para+acceder+a+esa+pantalla");
      return null;
    }
    return u;
  }
}
