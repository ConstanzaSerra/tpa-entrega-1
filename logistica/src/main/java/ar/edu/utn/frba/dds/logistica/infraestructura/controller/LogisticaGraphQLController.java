package ar.edu.utn.frba.dds.logistica.infraestructura.controller;

import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.dominio.Camion;
import ar.edu.utn.frba.dds.logistica.dominio.Entrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoEntrega;
import ar.edu.utn.frba.dds.logistica.dominio.EstadoRuta;
import ar.edu.utn.frba.dds.logistica.dominio.ParadaDeRuta;
import ar.edu.utn.frba.dds.logistica.dominio.PosicionCamion;
import ar.edu.utn.frba.dds.logistica.dominio.Ruta;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DashboardCamionDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EntregaAfectadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.CamionRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.EntregaRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.GpsRepository;
import ar.edu.utn.frba.dds.logistica.infraestructura.repository.RutaRepository;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.javalin.http.Context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entrega 4 - Requerimiento detallado 3b:
 * Exposición GraphQL (/graphql) del servicio de Logística (servicio extraído en Entrega 2),
 * permitiendo consultar y operar flota de camiones, rutas, entregas, fotos y seguimiento GPS
 * desde el servidor principal / cliente liviano mediante GraphQL en reemplazo de HTTP REST.
 */
public class LogisticaGraphQLController {

    private static final String SDL = """
        type CamionGQL {
          id: ID!
          patente: String!
          volumenM3: Float!
          alturaM: Float!
          capacidadKg: Float!
        }

        type EntregaGQL {
          id: ID!
          donacionId: ID!
          entidadBeneficiariaId: ID!
          direccionDestino: String
          estado: String!
          patenteCamion: String
          camionId: ID
          fechaHoraEntrega: String
          fotosUrl: [String!]!
        }

        type RutaGQL {
          id: ID!
          camionId: ID
          patenteCamion: String
          estado: String!
          linkMapa: String
          donacionesIds: [ID!]!
        }

        type SeguimientoCamionGQL {
          camionId: ID!
          patente: String!
          latitud: Float
          longitud: Float
          velocidad: Float
          ultimaActualizacion: String
          rutaId: ID
          estadoRuta: String
          avancePorcentaje: Int
          entregasTotal: Int
          entregasFinalizadas: Int
          donacionesIds: [ID!]!
        }

        type Query {
          camiones: [CamionGQL!]!
          entregas: [EntregaGQL!]!
          rutas: [RutaGQL!]!
          seguimientoCamiones: [SeguimientoCamionGQL!]!
        }

        type Mutation {
          crearCamion(patente: String!, volumenM3: Float!, alturaM: Float!, capacidadKg: Float!): CamionGQL!
          eliminarCamion(id: ID!): Boolean!
          crearRutaDirecta(camionId: ID!, entidadBeneficiariaId: ID!, direccion: String!, donacionId: ID!): RutaGQL
          iniciarRuta(id: ID!): Boolean!
          confirmarEntregaPorDonacion(donacionId: ID!, entidadBeneficiariaId: ID, direccionDestino: String, fotoUrl: String): Boolean!
          reportarPosicion(camionId: ID!, latitud: Float!, longitud: Float!, velocidad: Float): Boolean!
        }
        """;

    private final CamionRepository camionRepository;
    private final RutaRepository rutaRepository;
    private final EntregaRepository entregaRepository;
    private final GpsRepository gpsRepository;
    private final DonacionesAPI donacionesAPI;
    private final GraphQL graphQL;

    public LogisticaGraphQLController(CamionRepository camionRepository,
                                      RutaRepository rutaRepository,
                                      EntregaRepository entregaRepository,
                                      GpsRepository gpsRepository,
                                      DonacionesAPI donacionesAPI) {
        this.camionRepository = camionRepository;
        this.rutaRepository = rutaRepository;
        this.entregaRepository = entregaRepository;
        this.gpsRepository = gpsRepository;
        this.donacionesAPI = donacionesAPI;
        this.graphQL = construirGraphQL();
    }

    private GraphQL construirGraphQL() {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(SDL);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", builder -> builder
                .dataFetcher("camiones", env -> camionRepository.buscarTodos().stream()
                    .map(this::mapearCamion)
                    .collect(Collectors.toList()))
                .dataFetcher("entregas", env -> entregaRepository.buscarTodas().stream()
                    .map(this::mapearEntrega)
                    .collect(Collectors.toList()))
                .dataFetcher("rutas", env -> rutaRepository.buscarTodas().stream()
                    .map(this::mapearRuta)
                    .collect(Collectors.toList()))
                .dataFetcher("seguimientoCamiones", env -> {
                    Map<Long, PosicionCamion> posiciones = gpsRepository.obtenerTodasLasPosiciones();
                    List<Ruta> todasLasRutas = rutaRepository.buscarTodas();
                    return camionRepository.buscarTodos().stream().map(camion -> {
                        PosicionCamion pos = posiciones.get(camion.getId());
                        Optional<Ruta> rutaActiva = todasLasRutas.stream()
                            .filter(r -> r.getEstado() == EstadoRuta.EN_CURSO
                                && r.getCamion() != null
                                && camion.getId().equals(r.getCamion().getId()))
                            .findFirst();
                        if (rutaActiva.isEmpty()) {
                            rutaActiva = todasLasRutas.stream()
                                .filter(r -> r.getEstado() == EstadoRuta.PLANIFICADA
                                    && r.getCamion() != null
                                    && camion.getId().equals(r.getCamion().getId()))
                                .findFirst();
                        }
                        DashboardCamionDTO dto = DashboardCamionDTO.build(camion, pos, rutaActiva.orElse(null));
                        Map<String, Object> m = new HashMap<>();
                        m.put("camionId", camion.getId());
                        m.put("patente", camion.getPatente());
                        if (pos != null) {
                            m.put("latitud", pos.getLatitud());
                            m.put("longitud", pos.getLongitud());
                            m.put("velocidad", pos.getVelocidad());
                            m.put("ultimaActualizacion", pos.getTimestamp() != null ? pos.getTimestamp().toString() : null);
                        }
                        if (rutaActiva.isPresent()) {
                            Ruta r = rutaActiva.get();
                            m.put("rutaId", r.getId());
                            m.put("estadoRuta", r.getEstado().name());
                            m.put("avancePorcentaje", dto.ruta != null ? dto.ruta.avancePorcentaje : 0);
                            m.put("entregasTotal", dto.ruta != null ? dto.ruta.entregasTotal : 0);
                            m.put("entregasFinalizadas", dto.ruta != null ? dto.ruta.entregasFinalizadas : 0);
                            List<Long> donIds = r.getParadas().stream()
                                .flatMap(p -> p.getEntregas().stream())
                                .map(Entrega::getDonacionId)
                                .collect(Collectors.toList());
                            m.put("donacionesIds", donIds);
                        } else {
                            m.put("donacionesIds", Collections.emptyList());
                        }
                        return m;
                    }).collect(Collectors.toList());
                }))
            .type("Mutation", builder -> builder
                .dataFetcher("crearCamion", env -> {
                    String patente = env.getArgument("patente");
                    double volumenM3 = ((Number) env.getArgument("volumenM3")).doubleValue();
                    double alturaM = ((Number) env.getArgument("alturaM")).doubleValue();
                    double capacidadKg = ((Number) env.getArgument("capacidadKg")).doubleValue();
                    Camion c = camionRepository.guardar(new Camion(patente, volumenM3, alturaM, capacidadKg));
                    return mapearCamion(c);
                })
                .dataFetcher("eliminarCamion", env -> {
                    Long id = Long.valueOf(env.getArgument("id").toString());
                    camionRepository.eliminar(id);
                    return true;
                })
                .dataFetcher("crearRutaDirecta", env -> {
                    Long camionId = Long.valueOf(env.getArgument("camionId").toString());
                    Long entidadId = Long.valueOf(env.getArgument("entidadBeneficiariaId").toString());
                    String direccion = env.getArgument("direccion");
                    Long donacionId = Long.valueOf(env.getArgument("donacionId").toString());

                    Optional<Camion> camionOpt = camionRepository.buscarPorId(camionId);
                    if (camionOpt.isEmpty()) {
                        return null;
                    }
                    Entrega entrega = entregaRepository.buscarPorDonacionId(donacionId).stream()
                        .findFirst()
                        .orElseGet(() -> entregaRepository.guardar(new Entrega(donacionId, entidadId, direccion)));

                    Ruta ruta = new Ruta(camionOpt.get());
                    ruta.setLinkMapa("/donante/seguimiento");
                    ParadaDeRuta parada = new ParadaDeRuta(entidadId, direccion);
                    parada.agregarEntrega(entrega);
                    ruta.agregarParada(parada);
                    Ruta guardada = rutaRepository.guardar(ruta);
                    donacionesAPI.informarDonacionPlanificada(donacionId);
                    return mapearRuta(guardada);
                })
                .dataFetcher("iniciarRuta", env -> {
                    Long id = Long.valueOf(env.getArgument("id").toString());
                    Optional<Ruta> rutaOpt = rutaRepository.buscarPorId(id);
                    if (rutaOpt.isEmpty()) {
                        return false;
                    }
                    Ruta ruta = rutaOpt.get();
                    try {
                        List<Entrega> entregas = ruta.iniciar();
                        for (Entrega e : entregas) {
                            entregaRepository.guardar(e);
                        }
                        rutaRepository.guardar(ruta);
                        List<EntregaAfectadaDTO> afectadas = entregas.stream()
                            .map(e -> new EntregaAfectadaDTO(e.getId(), e.getDonacionId(), e.getEntidadBeneficiariaId()))
                            .collect(Collectors.toList());
                        donacionesAPI.notificarInicioRuta(new EventoInicioRutaDTO(
                            ruta.getId(),
                            ruta.getCamion() != null ? ruta.getCamion().getPatente() : "CAMION-01",
                            ruta.getLinkMapa() != null ? ruta.getLinkMapa() : "/donante/seguimiento",
                            afectadas
                        ));
                        return true;
                    } catch (IllegalStateException e) {
                        return false;
                    }
                })
                .dataFetcher("confirmarEntregaPorDonacion", env -> {
                    Long donacionId = Long.valueOf(env.getArgument("donacionId").toString());
                    Long entidadId = env.getArgument("entidadBeneficiariaId") != null
                        ? Long.valueOf(env.getArgument("entidadBeneficiariaId").toString()) : 1L;
                    String direccion = env.getArgument("direccionDestino") != null
                        ? env.getArgument("direccionDestino") : "";
                    String fotoUrl = env.getArgument("fotoUrl");

                    Camion camion = camionRepository.buscarTodos().stream().findFirst()
                        .orElseGet(() -> camionRepository.guardar(new Camion("AD-100-UTN", 25.0, 3.2, 5000.0)));

                    Entrega entrega = entregaRepository.buscarPorDonacionId(donacionId).stream()
                        .findFirst()
                        .orElseGet(() -> entregaRepository.guardar(new Entrega(donacionId, entidadId, direccion)));

                    if (entrega.getEstado() == EstadoEntrega.PENDIENTE) {
                        entrega.iniciarTraslado();
                    }
                    if (entrega.getEstado() == EstadoEntrega.EN_TRASLADO) {
                        entrega.confirmar(camion, LocalDateTime.now());
                    }
                    if (fotoUrl != null && !fotoUrl.isBlank()) {
                        entrega.agregarFoto(fotoUrl);
                    }
                    entregaRepository.guardar(entrega);
                    donacionesAPI.notificarEntregaConfirmada(new EventoEntregaConfirmadaDTO(
                        entrega.getId(),
                        donacionId,
                        camion.getPatente(),
                        entrega.getFechaHoraEntrega() != null ? entrega.getFechaHoraEntrega() : LocalDateTime.now()
                    ));
                    return true;
                })
                .dataFetcher("reportarPosicion", env -> {
                    Long camionId = Long.valueOf(env.getArgument("camionId").toString());
                    double lat = ((Number) env.getArgument("latitud")).doubleValue();
                    double lon = ((Number) env.getArgument("longitud")).doubleValue();
                    Double vel = env.getArgument("velocidad") != null
                        ? ((Number) env.getArgument("velocidad")).doubleValue() : 42.0;
                    gpsRepository.guardarPosicion(new PosicionCamion(camionId, lat, lon, vel, Instant.now()));
                    return true;
                }))
            .build();

        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        return GraphQL.newGraphQL(schema).build();
    }

    @SuppressWarnings("unchecked")
    public void ejecutar(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String query = (String) body.get("query");
        Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables", Collections.emptyMap());

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .variables(variables != null ? variables : Collections.emptyMap())
            .build();

        ExecutionResult result = graphQL.execute(executionInput);
        ctx.json(result.toSpecification());
    }

    public Map<String, Object> ejecutarDirecto(String query, Map<String, Object> variables) {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .variables(variables != null ? variables : Collections.emptyMap())
            .build();
        return graphQL.execute(executionInput).toSpecification();
    }

    private Map<String, Object> mapearCamion(Camion c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("patente", c.getPatente());
        m.put("volumenM3", c.getVolumenM3());
        m.put("alturaM", c.getAlturaM());
        m.put("capacidadKg", c.getCapacidadKg());
        return m;
    }

    private Map<String, Object> mapearEntrega(Entrega e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("donacionId", e.getDonacionId());
        m.put("entidadBeneficiariaId", e.getEntidadBeneficiariaId());
        m.put("direccionDestino", e.getDireccionDestino());
        m.put("estado", e.getEstado() != null ? e.getEstado().name() : "PENDIENTE");
        m.put("patenteCamion", e.getCamionQueEntrego() != null ? e.getCamionQueEntrego().getPatente() : null);
        m.put("camionId", e.getCamionQueEntrego() != null ? e.getCamionQueEntrego().getId() : null);
        m.put("fechaHoraEntrega", e.getFechaHoraEntrega() != null ? e.getFechaHoraEntrega().toString() : null);
        m.put("fotosUrl", e.getFotosUrl() != null ? new ArrayList<>(e.getFotosUrl()) : Collections.emptyList());
        return m;
    }

    private Map<String, Object> mapearRuta(Ruta r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("camionId", r.getCamion() != null ? r.getCamion().getId() : null);
        m.put("patenteCamion", r.getCamion() != null ? r.getCamion().getPatente() : null);
        m.put("estado", r.getEstado() != null ? r.getEstado().name() : "PLANIFICADA");
        m.put("linkMapa", r.getLinkMapa());
        List<Long> donIds = r.getParadas() != null
            ? r.getParadas().stream()
                .flatMap(p -> p.getEntregas().stream())
                .map(Entrega::getDonacionId)
                .collect(Collectors.toList())
            : Collections.emptyList();
        m.put("donacionesIds", donIds);
        return m;
    }
}
