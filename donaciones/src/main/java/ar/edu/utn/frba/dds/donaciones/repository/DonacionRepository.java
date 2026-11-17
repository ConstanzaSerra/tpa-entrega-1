package ar.edu.utn.frba.dds.donaciones.repository;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.persistencia.EntityManagerProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Misma interfaz publica que la version en memoria; lo unico que cambio es de
 * donde salen los datos. El EntityManager se inyecta por constructor para que
 * los tests puedan pasarle el de jpa-extras (HSQLDB) y la app el de Postgres.
 */
public class DonacionRepository {
  private final EntityManager entityManager;

  public DonacionRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public DonacionRepository() {
    this(EntityManagerProvider.getEntityManager());
  }

  public void guardar(Donacion donacion) {
    enTransaccion(() -> {
      if (donacion.getSubcategoria() != null && donacion.getSubcategoria().getId() == null) {
        entityManager.persist(donacion.getSubcategoria());
      }
      if (donacion.getId() == null) {
        entityManager.persist(donacion);
      } else {
        entityManager.merge(donacion);
      }
    });
  }

  public List<Donacion> buscarPorEstado(EstadoDonacion estado) {
    return entityManager
        .createQuery("SELECT d FROM Donacion d WHERE d.estado = :estado", Donacion.class)
        .setParameter("estado", estado)
        .getResultList();
  }

  public List<Donacion> buscarPorDonante(Long donanteId) {
    return entityManager
        .createQuery("SELECT d FROM Donacion d WHERE d.donante.id = :donanteId ORDER BY d.id DESC", Donacion.class)
        .setParameter("donanteId", donanteId)
        .getResultList();
  }

  public List<Donacion> buscarPorEntidadAsignada(Long entidadId) {
    return entityManager
        .createQuery("SELECT d FROM Donacion d WHERE d.entidadAsignada.id = :entidadId ORDER BY d.id DESC", Donacion.class)
        .setParameter("entidadId", entidadId)
        .getResultList();
  }

  public Optional<Donacion> buscarPorId(Long id) {
    return Optional.ofNullable(entityManager.find(Donacion.class, id));
  }

  public List<Donacion> obtenerDonacionesEnDeposito() {
    return buscarPorEstado(EstadoDonacion.EN_DEPOSITO);
  }

  public List<Donacion> obtenerTodas() {
    return entityManager
        .createQuery("SELECT d FROM Donacion d", Donacion.class)
        .getResultList();
  }

  // ===========================================================================
  //  Consulta paginada del inventario
  //  Filtrar, ordenar y paginar se resuelven en la consulta. Traer todo a
  //  memoria y recortar despues no ahorra nada: la base igual lee todas las
  //  filas y la aplicacion igual las instancia.
  // ===========================================================================

  /** Columnas por las que se admite ordenar, con su expresion JPQL. */
  private static final Map<String, String> COLUMNAS_ORDEN = Map.of(
      "fecha", "d.fechaRegistro",
      "id", "d.id",
      "cantidad", "d.cantidad",
      "estado", "d.estado",
      "subcategoria", "s.nombre",
      "donante", "d.donante.id"
  );

  public static final String ORDEN_POR_DEFECTO = "fecha";

  /** Una pagina de resultados junto con lo necesario para dibujar el paginador. */
  public record PaginaDonaciones(List<Donacion> items, long total, int pagina,
                                 int tamanio, int totalPaginas) {}

  /**
   * Devuelve una pagina del inventario. Los filtros nulos no se aplican.
   * El criterio de orden NO se concatena desde lo que llega por la URL: se
   * busca en COLUMNAS_ORDEN y, si no esta, se usa el de por defecto. Concatenar
   * el parametro directo seria una inyeccion de JPQL.
   */
  public PaginaDonaciones buscarPaginado(EstadoDonacion estado, Categoria categoria, Long donanteId,
                                         String ordenarPor, boolean descendente,
                                         int pagina, int tamanio) {
    StringBuilder condiciones = new StringBuilder();
    if (estado != null) {
      condiciones.append(" AND d.estado = :estado");
    }
    if (categoria != null) {
      condiciones.append(" AND s.categoria = :categoria");
    }
    if (donanteId != null) {
      condiciones.append(" AND d.donante.id = :donanteId");
    }

    String columna = COLUMNAS_ORDEN.getOrDefault(ordenarPor, COLUMNAS_ORDEN.get(ORDEN_POR_DEFECTO));
    String direccion = descendente ? "DESC" : "ASC";

    long total = aplicarFiltros(entityManager.createQuery(
            "SELECT COUNT(d) FROM Donacion d LEFT JOIN d.subcategoria s WHERE 1 = 1" + condiciones,
            Long.class),
        estado, categoria, donanteId).getSingleResult();

    int tamanioValido = Math.max(1, tamanio);
    int totalPaginas = total == 0 ? 1 : (int) Math.ceil((double) total / tamanioValido);
    int paginaValida = Math.min(Math.max(1, pagina), totalPaginas);

    // El id desempata: sin un criterio estable, dos filas con la misma fecha
    // pueden alternar de orden entre paginas y aparecer repetidas o faltar.
    String jpql = "SELECT d FROM Donacion d LEFT JOIN d.subcategoria s WHERE 1 = 1" + condiciones
        + " ORDER BY " + columna + " " + direccion + " NULLS LAST, d.id DESC";

    List<Donacion> items = aplicarFiltros(entityManager.createQuery(jpql, Donacion.class),
        estado, categoria, donanteId)
        .setFirstResult((paginaValida - 1) * tamanioValido)
        .setMaxResults(tamanioValido)
        .getResultList();

    return new PaginaDonaciones(items, total, paginaValida, tamanioValido, totalPaginas);
  }

  private <T> TypedQuery<T> aplicarFiltros(TypedQuery<T> query, EstadoDonacion estado,
                                           Categoria categoria, Long donanteId) {
    if (estado != null) {
      query.setParameter("estado", estado);
    }
    if (categoria != null) {
      query.setParameter("categoria", categoria);
    }
    if (donanteId != null) {
      query.setParameter("donanteId", donanteId);
    }
    return query;
  }

  public boolean eliminar(Long id) {
    Donacion donacion = entityManager.find(Donacion.class, id);
    if (donacion == null) {
      return false;
    }
    enTransaccion(() -> entityManager.remove(donacion));
    return true;
  }

  // Si ya hay una transaccion abierta (caso tipico en los tests de jpa-extras,
  // que corren todo dentro de una), no se abre otra.
  private void enTransaccion(Runnable accion) {
    if (entityManager.getTransaction().isActive()) {
      accion.run();
      return;
    }
    entityManager.getTransaction().begin();
    try {
      accion.run();
      entityManager.getTransaction().commit();
    } catch (RuntimeException e) {
      entityManager.getTransaction().rollback();
      throw e;
    }
  }
}
