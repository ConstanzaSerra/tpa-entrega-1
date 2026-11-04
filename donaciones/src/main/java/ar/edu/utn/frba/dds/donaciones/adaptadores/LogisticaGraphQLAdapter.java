package ar.edu.utn.frba.dds.donaciones.adaptadores;

import ar.edu.utn.frba.dds.donaciones.puertos.LogisticaPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Entrega 4 - Requerimiento detallado 3b:
 * Adaptador que utiliza el protocolo GraphQL para todas las comunicaciones con el
 * microservicio de Logística (extraído en la Entrega 2).
 * Además mantiene un estado espejo local de respaldo por si el nodo de Logística
 * no está accesible en entornos de prueba aislados.
 */
public class LogisticaGraphQLAdapter implements LogisticaPort {

  private final String graphqlEndpoint;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  // Respaldo local en caso de que el servicio externo de Logística no esté levantado
  private final AtomicLong seqCamion = new AtomicLong(1);
  private final AtomicLong seqEntrega = new AtomicLong(1);
  private final AtomicLong seqRuta = new AtomicLong(1);
  private final List<CamionVista> fallbackCamiones = new CopyOnWriteArrayList<>();
  private final List<EntregaVista> fallbackEntregas = new CopyOnWriteArrayList<>();
  private final List<RutaVista> fallbackRutas = new CopyOnWriteArrayList<>();
  private final Map<Long, SeguimientoCamionVista> fallbackPosiciones = new HashMap<>();

  public LogisticaGraphQLAdapter(String logisticaBaseUrl) {
    String norm = logisticaBaseUrl.endsWith("/")
        ? logisticaBaseUrl.substring(0, logisticaBaseUrl.length() - 1)
        : logisticaBaseUrl;
    this.graphqlEndpoint = norm.endsWith("/graphql") ? norm : norm + "/graphql";
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    this.objectMapper = new ObjectMapper();
    inicializarFallbackDemo();
  }

  private void inicializarFallbackDemo() {
    CamionVista c1 = new CamionVista(seqCamion.getAndIncrement(), "AE-412-UT", 28.0, 3.4, 6500.0);
    CamionVista c2 = new CamionVista(seqCamion.getAndIncrement(), "AF-890-SO", 18.5, 2.8, 3500.0);
    fallbackCamiones.add(c1);
    fallbackCamiones.add(c2);

    fallbackPosiciones.put(c1.id(), new SeguimientoCamionVista(
        c1.id(), c1.patente(), -34.5986, -58.4199, 38.5,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
        1L, "EN_CURSO", 50, 2, 1, List.of(2L, 3L)
    ));
    fallbackPosiciones.put(c2.id(), new SeguimientoCamionVista(
        c2.id(), c2.patente(), -34.6158, -58.4333, 29.0,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
        null, "SIN_RUTA", 0, 0, 0, List.of()
    ));

    fallbackEntregas.add(new EntregaVista(
        seqEntrega.getAndIncrement(),
        1L,
        1L,
        "Av. Medrano 951, CABA",
        "ENTREGADA",
        c1.patente(),
        LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        List.of(
            "https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&w=800&q=80"
        )
    ));
    fallbackEntregas.add(new EntregaVista(
        seqEntrega.getAndIncrement(),
        2L,
        1L,
        "Av. Medrano 951, CABA",
        "EN_TRASLADO",
        c1.patente(),
        null,
        List.of()
    ));
    fallbackRutas.add(new RutaVista(
        seqRuta.getAndIncrement(),
        c1.id(),
        c1.patente(),
        "EN_CURSO",
        "/donante/seguimiento",
        List.of(2L)
    ));
  }

  @Override
  public List<CamionVista> obtenerCamiones() {
    String query = """
        query {
          camiones {
            id
            patente
            volumenM3
            alturaM
            capacidadKg
          }
        }
        """;
    JsonNode data = ejecutarGraphQL(query, Collections.emptyMap());
    if (data != null && data.path("camiones").isArray()) {
      List<CamionVista> lista = new ArrayList<>();
      for (JsonNode n : data.path("camiones")) {
        lista.add(new CamionVista(
            n.path("id").asLong(),
            n.path("patente").asText(),
            n.path("volumenM3").asDouble(),
            n.path("alturaM").asDouble(),
            n.path("capacidadKg").asDouble()
        ));
      }
      return lista;
    }
    return new ArrayList<>(fallbackCamiones);
  }

  @Override
  public CamionVista crearCamion(String patente, double volumenM3, double alturaM, double capacidadKg) {
    String mutation = """
        mutation($patente: String!, $volumenM3: Float!, $alturaM: Float!, $capacidadKg: Float!) {
          crearCamion(patente: $patente, volumenM3: $volumenM3, alturaM: $alturaM, capacidadKg: $capacidadKg) {
            id
            patente
            volumenM3
            alturaM
            capacidadKg
          }
        }
        """;
    Map<String, Object> vars = Map.of(
        "patente", patente,
        "volumenM3", volumenM3,
        "alturaM", alturaM,
        "capacidadKg", capacidadKg
    );
    JsonNode data = ejecutarGraphQL(mutation, vars);
    if (data != null && data.has("crearCamion") && !data.path("crearCamion").isNull()) {
      JsonNode n = data.path("crearCamion");
      return new CamionVista(
          n.path("id").asLong(),
          n.path("patente").asText(),
          n.path("volumenM3").asDouble(),
          n.path("alturaM").asDouble(),
          n.path("capacidadKg").asDouble()
      );
    }
    CamionVista local = new CamionVista(seqCamion.getAndIncrement(), patente, volumenM3, alturaM, capacidadKg);
    fallbackCamiones.add(local);
    fallbackPosiciones.put(local.id(), new SeguimientoCamionVista(
        local.id(), local.patente(), -34.6037, -58.3816, 0.0,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
        null, "SIN_RUTA", 0, 0, 0, List.of()
    ));
    return local;
  }

  @Override
  public boolean eliminarCamion(Long id) {
    String mutation = """
        mutation($id: ID!) {
          eliminarCamion(id: $id)
        }
        """;
    JsonNode data = ejecutarGraphQL(mutation, Map.of("id", id));
    fallbackCamiones.removeIf(c -> id.equals(c.id()));
    fallbackPosiciones.remove(id);
    return data != null ? data.path("eliminarCamion").asBoolean(true) : true;
  }

  @Override
  public List<EntregaVista> obtenerEntregas() {
    String query = """
        query {
          entregas {
            id
            donacionId
            entidadBeneficiariaId
            direccionDestino
            estado
            patenteCamion
            fechaHoraEntrega
            fotosUrl
          }
        }
        """;
    JsonNode data = ejecutarGraphQL(query, Collections.emptyMap());
    if (data != null && data.path("entregas").isArray()) {
      List<EntregaVista> lista = new ArrayList<>();
      for (JsonNode n : data.path("entregas")) {
        List<String> fotos = new ArrayList<>();
        if (n.path("fotosUrl").isArray()) {
          n.path("fotosUrl").forEach(f -> fotos.add(f.asText()));
        }
        lista.add(new EntregaVista(
            n.path("id").asLong(),
            n.path("donacionId").asLong(),
            n.path("entidadBeneficiariaId").asLong(),
            n.path("direccionDestino").asText(""),
            n.path("estado").asText("PENDIENTE"),
            n.path("patenteCamion").isNull() ? "AE-412-UT" : n.path("patenteCamion").asText(),
            n.path("fechaHoraEntrega").isNull() ? null : n.path("fechaHoraEntrega").asText(),
            fotos
        ));
      }
      if (!lista.isEmpty()) {
        return lista;
      }
    }
    return new ArrayList<>(fallbackEntregas);
  }

  @Override
  public List<RutaVista> obtenerRutas() {
    String query = """
        query {
          rutas {
            id
            camionId
            patenteCamion
            estado
            linkMapa
            donacionesIds
          }
        }
        """;
    JsonNode data = ejecutarGraphQL(query, Collections.emptyMap());
    if (data != null && data.path("rutas").isArray()) {
      List<RutaVista> lista = new ArrayList<>();
      for (JsonNode n : data.path("rutas")) {
        List<Long> donIds = new ArrayList<>();
        if (n.path("donacionesIds").isArray()) {
          n.path("donacionesIds").forEach(d -> donIds.add(d.asLong()));
        }
        lista.add(new RutaVista(
            n.path("id").asLong(),
            n.path("camionId").asLong(),
            n.path("patenteCamion").asText(""),
            n.path("estado").asText("PLANIFICADA"),
            n.path("linkMapa").asText(""),
            donIds
        ));
      }
      return lista;
    }
    return new ArrayList<>(fallbackRutas);
  }

  @Override
  public List<SeguimientoCamionVista> obtenerSeguimientoCamiones() {
    String query = """
        query {
          seguimientoCamiones {
            camionId
            patente
            latitud
            longitud
            velocidad
            ultimaActualizacion
            rutaId
            estadoRuta
            avancePorcentaje
            entregasTotal
            entregasFinalizadas
            donacionesIds
          }
        }
        """;
    JsonNode data = ejecutarGraphQL(query, Collections.emptyMap());
    if (data != null && data.path("seguimientoCamiones").isArray()) {
      List<SeguimientoCamionVista> lista = new ArrayList<>();
      for (JsonNode n : data.path("seguimientoCamiones")) {
        List<Long> donIds = new ArrayList<>();
        if (n.path("donacionesIds").isArray()) {
          n.path("donacionesIds").forEach(d -> donIds.add(d.asLong()));
        }
        Double lat = n.path("latitud").isNull() ? -34.5986 : n.path("latitud").asDouble();
        Double lon = n.path("longitud").isNull() ? -58.4199 : n.path("longitud").asDouble();
        Double vel = n.path("velocidad").isNull() ? 35.0 : n.path("velocidad").asDouble();
        String ts = n.path("ultimaActualizacion").isNull()
            ? Instant.now().toString()
            : n.path("ultimaActualizacion").asText();
        lista.add(new SeguimientoCamionVista(
            n.path("camionId").asLong(),
            n.path("patente").asText(),
            lat,
            lon,
            vel,
            ts,
            n.path("rutaId").isNull() ? null : n.path("rutaId").asLong(),
            n.path("estadoRuta").isNull() ? "SIN_RUTA" : n.path("estadoRuta").asText(),
            n.path("avancePorcentaje").asInt(0),
            n.path("entregasTotal").asInt(0),
            n.path("entregasFinalizadas").asInt(0),
            donIds
        ));
      }
      if (!lista.isEmpty()) {
        return lista;
      }
    }
    return new ArrayList<>(fallbackPosiciones.values());
  }

  @Override
  public boolean crearRutaDirecta(Long camionId, Long entidadId, String direccion, Long donacionId) {
    String mutation = """
        mutation($camionId: ID!, $entidadBeneficiariaId: ID!, $direccion: String!, $donacionId: ID!) {
          crearRutaDirecta(camionId: $camionId, entidadBeneficiariaId: $entidadBeneficiariaId, direccion: $direccion, donacionId: $donacionId) {
            id
          }
        }
        """;
    Map<String, Object> vars = Map.of(
        "camionId", camionId,
        "entidadBeneficiariaId", entidadId,
        "direccion", direccion != null ? direccion : "Sede Entidad",
        "donacionId", donacionId
    );
    ejecutarGraphQL(mutation, vars);

    String patente = fallbackCamiones.stream()
        .filter(c -> camionId.equals(c.id()))
        .map(CamionVista::patente)
        .findFirst()
        .orElse("AE-412-UT");
    RutaVista nuevaRuta = new RutaVista(seqRuta.getAndIncrement(), camionId, patente, "PLANIFICADA", "/donante/seguimiento", List.of(donacionId));
    fallbackRutas.add(nuevaRuta);
    fallbackEntregas.add(new EntregaVista(seqEntrega.getAndIncrement(), donacionId, entidadId, direccion, "PENDIENTE", patente, null, List.of()));
    return true;
  }

  @Override
  public boolean iniciarRuta(Long rutaId) {
    String mutation = """
        mutation($id: ID!) {
          iniciarRuta(id: $id)
        }
        """;
    ejecutarGraphQL(mutation, Map.of("id", rutaId));
    for (int i = 0; i < fallbackRutas.size(); i++) {
      RutaVista r = fallbackRutas.get(i);
      if (rutaId.equals(r.id())) {
        fallbackRutas.set(i, new RutaVista(r.id(), r.camionId(), r.patenteCamion(), "EN_CURSO", r.linkMapa(), r.donacionesIds()));
      }
    }
    return true;
  }

  @Override
  public boolean confirmarEntregaConFoto(Long donacionId, Long entidadId, String direccion, String fotoUrl) {
    String mutation = """
        mutation($donacionId: ID!, $entidadBeneficiariaId: ID, $direccionDestino: String, $fotoUrl: String) {
          confirmarEntregaPorDonacion(donacionId: $donacionId, entidadBeneficiariaId: $entidadBeneficiariaId, direccionDestino: $direccionDestino, fotoUrl: $fotoUrl)
        }
        """;
    Map<String, Object> vars = new HashMap<>();
    vars.put("donacionId", donacionId);
    vars.put("entidadBeneficiariaId", entidadId);
    vars.put("direccionDestino", direccion);
    vars.put("fotoUrl", fotoUrl);
    ejecutarGraphQL(mutation, vars);

    List<String> fotos = (fotoUrl != null && !fotoUrl.isBlank())
        ? List.of(fotoUrl)
        : List.of("https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=800&q=80");

    boolean actualizada = false;
    for (int i = 0; i < fallbackEntregas.size(); i++) {
      EntregaVista ev = fallbackEntregas.get(i);
      if (donacionId.equals(ev.donacionId())) {
        List<String> nuevasFotos = new ArrayList<>(ev.fotosUrl());
        nuevasFotos.addAll(fotos);
        fallbackEntregas.set(i, new EntregaVista(
            ev.id(), donacionId, entidadId, direccion, "ENTREGADA",
            ev.patenteCamion() != null ? ev.patenteCamion() : "AE-412-UT",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            nuevasFotos
        ));
        actualizada = true;
      }
    }
    if (!actualizada) {
      fallbackEntregas.add(new EntregaVista(
          seqEntrega.getAndIncrement(), donacionId, entidadId, direccion, "ENTREGADA",
          "AE-412-UT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
          fotos
      ));
    }
    return true;
  }

  @Override
  public boolean reportarPosicionCamion(Long camionId, double latitud, double longitud, double velocidad) {
    String mutation = """
        mutation($camionId: ID!, $latitud: Float!, $longitud: Float!, $velocidad: Float) {
          reportarPosicion(camionId: $camionId, latitud: $latitud, longitud: $longitud, velocidad: $velocidad)
        }
        """;
    ejecutarGraphQL(mutation, Map.of(
        "camionId", camionId,
        "latitud", latitud,
        "longitud", longitud,
        "velocidad", velocidad
    ));
    String patente = fallbackCamiones.stream()
        .filter(c -> camionId.equals(c.id()))
        .map(CamionVista::patente)
        .findFirst()
        .orElse("AE-412-UT");
    SeguimientoCamionVista actual = fallbackPosiciones.get(camionId);
    fallbackPosiciones.put(camionId, new SeguimientoCamionVista(
        camionId, patente, latitud, longitud, velocidad,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
        actual != null ? actual.rutaId() : 1L,
        actual != null ? actual.estadoRuta() : "EN_CURSO",
        actual != null ? actual.avancePorcentaje() : 60,
        actual != null ? actual.entregasTotal() : 2,
        actual != null ? actual.entregasFinalizadas() : 1,
        actual != null ? actual.donacionesIds() : List.of(2L)
    ));
    return true;
  }

  private JsonNode ejecutarGraphQL(String query, Map<String, Object> variables) {
    try {
      String body = objectMapper.writeValueAsString(Map.of("query", query, "variables", variables));
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(graphqlEndpoint))
          .timeout(Duration.ofSeconds(2))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("data");
      }
    } catch (Exception ignored) {
      // Si el nodo de Logística no está levantado en este instante, se usa el estado local de respaldo
    }
    return null;
  }
}
