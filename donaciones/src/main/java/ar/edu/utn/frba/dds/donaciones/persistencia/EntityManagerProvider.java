package ar.edu.utn.frba.dds.donaciones.persistencia;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Punto unico donde se decide contra que base corre la aplicacion.
 *
 * El orden de resolucion de la unidad es deliberado:
 *   1. la system property persistence.unit  -> la fija surefire, de modo que
 *      TODOS los tests van a HSQLDB sin que haya que acordarse en cada uno;
 *   2. la variable de entorno PERSISTENCE_UNIT -> para levantar la app contra
 *      otra base sin recompilar;
 *   3. postgres-persistence-unit -> el despliegue local por defecto.
 *
 * Ademas mantiene UN EntityManager por hilo. Sin esto, cada repositorio creado
 * con new se armaba su propio contexto de persistencia, y una entidad guardada
 * por un repositorio quedaba detached para los demas: guardabas la Subcategoria
 * con un repo y al guardar la Donacion con otro, Hibernate la veia como una
 * instancia transitoria. El EntityManager no es thread-safe, por eso es uno por
 * hilo y no uno global.
 */
public class EntityManagerProvider {
  public static final String PROPIEDAD_UNIDAD = "persistence.unit";
  public static final String VARIABLE_UNIDAD = "PERSISTENCE_UNIT";
  private static final String UNIDAD_POR_DEFECTO = "postgres-persistence-unit";

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
      factory = Persistence.createEntityManagerFactory(unidadDePersistencia());
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
