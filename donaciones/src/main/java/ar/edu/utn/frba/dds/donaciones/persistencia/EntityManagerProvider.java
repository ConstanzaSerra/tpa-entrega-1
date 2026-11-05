package ar.edu.utn.frba.dds.donaciones.persistencia;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * Punto unico donde se decide contra que base corre la aplicacion.
 *
 * Orden de resolucion:
 *   1. System property persistence.unit (usada por surefire en tests -> HSQLDB)
 *   2. Variable de entorno PERSISTENCE_UNIT
 *   3. postgres-persistence-unit (con soporte para variables cloud JDBC_DATABASE_URL / DB_USER / DB_PASSWORD
 *      y fallback automatico a simple-persistence-unit si PostgreSQL no está levantado localmente).
 */
public class EntityManagerProvider {
  public static final String PROPIEDAD_UNIDAD = "persistence.unit";
  public static final String VARIABLE_UNIDAD = "PERSISTENCE_UNIT";
  private static final String UNIDAD_POR_DEFECTO = "postgres-persistence-unit";
  private static final String UNIDAD_MEMORIA = "simple-persistence-unit";

  private static EntityManagerFactory factory;
  private static final ThreadLocal<EntityManager> ENTITY_MANAGER_DEL_HILO = new ThreadLocal<>();

  private EntityManagerProvider() {}

  public static String unidadDePersistencia() {
    String desdePropiedad = System.getProperty(PROPIEDAD_UNIDAD);
    if (desdePropiedad != null && !desdePropiedad.isBlank()) {
      return desdePropiedad;
    }
    String desdeEntorno = System.getenv(VARIABLE_UNIDAD);
    if (desdeEntorno != null && !desdeEntorno.isBlank()) {
      return desdeEntorno;
    }
    return UNIDAD_POR_DEFECTO;
  }

  public static synchronized EntityManagerFactory getFactory() {
    if (factory == null) {
      String unidad = unidadDePersistencia();
      Map<String, String> overrides = new HashMap<>();
      String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
      if (jdbcUrl != null && !jdbcUrl.isBlank()) {
        overrides.put("javax.persistence.jdbc.url", jdbcUrl);
      }
      String dbUser = System.getenv("DB_USER");
      if (dbUser != null && !dbUser.isBlank()) {
        overrides.put("javax.persistence.jdbc.user", dbUser);
      }
      String dbPassword = System.getenv("DB_PASSWORD");
      if (dbPassword != null && !dbPassword.isBlank()) {
        overrides.put("javax.persistence.jdbc.password", dbPassword);
      }

      try {
        factory = overrides.isEmpty()
            ? Persistence.createEntityManagerFactory(unidad)
            : Persistence.createEntityManagerFactory(unidad, overrides);
      } catch (Exception e) {
        // Fail fast: si se pidio una base real y no se puede conectar, el
        // sistema NO arranca. Antes caia a HSQLDB en memoria, con lo cual en
        // produccion la app levantaba igual y los datos se perdian en cada
        // reinicio sin que nadie se enterara
        // El fallback a memoria queda solo para la unidad de memoria misma.
        throw new IllegalStateException(
            "[EntityManagerProvider] No se pudo conectar a la base de la unidad '" + unidad
                + "'. Revisar JDBC_DATABASE_URL, DB_USER y DB_PASSWORD. Causa: "
                + e.getMessage(), e);
      }
    }
    return factory;
  }

  /** El EntityManager compartido por todos los repositorios de este hilo. */
  public static EntityManager getEntityManager() {
    EntityManager entityManager = ENTITY_MANAGER_DEL_HILO.get();
    if (entityManager == null || !entityManager.isOpen()) {
      entityManager = getFactory().createEntityManager();
      ENTITY_MANAGER_DEL_HILO.set(entityManager);
    }
    return entityManager;
  }

  /** Un EntityManager nuevo e independiente. Usarlo solo si hace falta aislar. */
  public static EntityManager crearEntityManager() {
    return getFactory().createEntityManager();
  }

  public static void cerrarEntityManagerDelHilo() {
    EntityManager entityManager = ENTITY_MANAGER_DEL_HILO.get();
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
    }
    ENTITY_MANAGER_DEL_HILO.remove();
  }

  public static synchronized void cerrar() {
    cerrarEntityManagerDelHilo();
    if (factory != null && factory.isOpen()) {
      factory.close();
    }
    factory = null;
  }
}
