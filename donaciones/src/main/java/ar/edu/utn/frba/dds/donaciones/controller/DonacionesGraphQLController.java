package ar.edu.utn.frba.dds.donaciones.controller;

import ar.edu.utn.frba.dds.donaciones.domain.Donacion;
import ar.edu.utn.frba.dds.donaciones.domain.Email;
import ar.edu.utn.frba.dds.donaciones.domain.EstadoDonacion;
import ar.edu.utn.frba.dds.donaciones.domain.Notificable;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.DonacionRepository;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entrega 4 - Requerimiento detallado 3b:
 * Exposición del contrato de Donaciones mediante el protocolo GraphQL (/graphql)
 * como reemplazo de HTTP/REST para la comunicación con el microservicio de Logística.
 */
public class DonacionesGraphQLController {

  private static final String SDL = """
      type DonacionParaRuta {
        donacionId: ID!
        entidadBeneficiariaId: ID!
        direccionDestino: String
      }

      input EntregaAfectadaInput {
        entregaId: ID
        donacionId: ID!
        entidadId: ID
      }

      type Query {
        donacionesPorEstado(estado: String!): [DonacionParaRuta!]!
      }

      type Mutation {
        informarDonacionPlanificada(donacionId: ID!): Boolean!
        notificarInicioRuta(rutaId: ID, patenteCamion: String, linkMapa: String, entregasAfectadas: [EntregaAfectadaInput!]!): Boolean!
        notificarEntregaConfirmada(entregaId: ID, donacionId: ID!, patenteCamion: String, fechaHora: String): Boolean!
        notificarEntregaFallida(entregaId: ID, donacionId: ID!, motivo: String): Boolean!
      }
      """;

  private final DonacionRepository donacionRepository;
  private final GestorDeNotificaciones gestor;
  private final GraphQL graphQL;

  public DonacionesGraphQLController(DonacionRepository donacionRepository, GestorDeNotificaciones gestor) {
    this.donacionRepository = donacionRepository;
    this.gestor = gestor;
    this.graphQL = construirGraphQL();
  }

  private GraphQL construirGraphQL() {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(SDL);

    RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
        .type("Query", builder -> builder
            .dataFetcher("donacionesPorEstado", env -> {
              String estadoStr = env.getArgument("estado");
              EstadoDonacion estado = EstadoDonacion.valueOf(estadoStr);
              return donacionRepository.buscarPorEstado(estado).stream()
                  .filter(d -> d.getEntidadAsignada() != null)
                  .map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("donacionId", d.getId());
                    item.put("entidadBeneficiariaId", d.getEntidadAsignada().getId());
                    item.put("direccionDestino", d.getEntidadAsignada().getDireccion());
                    return item;
                  })
                  .collect(Collectors.toList());
            }))
        .type("Mutation", builder -> builder
            .dataFetcher("informarDonacionPlanificada", env -> {
              Long donacionId = Long.valueOf(env.getArgument("donacionId").toString());
              var oDonacion = donacionRepository.buscarPorId(donacionId);
              if (oDonacion.isEmpty()) {
                return false;
              }
              Donacion d = oDonacion.get();
              try {
                d.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
                donacionRepository.guardar(d);
                return true;
              } catch (IllegalStateException e) {
                return false;
              }
            })
            .dataFetcher("notificarInicioRuta", env -> {
              String linkMapa = env.getArgument("linkMapa");
              List<Map<String, Object>> entregas = env.getArgument("entregasAfectadas");
              if (entregas != null) {
                for (Map<String, Object> entrega : entregas) {
                  Long donacionId = Long.valueOf(entrega.get("donacionId").toString());
                  donacionRepository.buscarPorId(donacionId).ifPresent(d -> {
                    try {
                      if (d.getEstado() == EstadoDonacion.ASIGNACION_REALIZADA) {
                        d.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
                      }
                      if (d.getEstado() == EstadoDonacion.LISTA_PARA_ENTREGAR) {
                        d.avanzarHacia(EstadoDonacion.EN_TRASLADO);
                      }
                      donacionRepository.guardar(d);
                    } catch (IllegalStateException ignored) {}
                    notificarPartes(d, "Tu entrega inició su recorrido. Seguila en el mapa: " + (linkMapa != null ? linkMapa : "/"));
                  });
                }
              }
              return true;
            })
            .dataFetcher("notificarEntregaConfirmada", env -> {
              Long donacionId = Long.valueOf(env.getArgument("donacionId").toString());
              String patenteCamion = env.getArgument("patenteCamion");
              String fechaHora = env.getArgument("fechaHora");
              donacionRepository.buscarPorId(donacionId).ifPresent(d -> {
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
                  donacionRepository.guardar(d);
                } catch (IllegalStateException ignored) {}
                notificarPartes(d, "Entrega confirmada con éxito. Fecha: " + fechaHora + " | Camión: " + patenteCamion);
              });
              return true;
            })
            .dataFetcher("notificarEntregaFallida", env -> {
              Long donacionId = Long.valueOf(env.getArgument("donacionId").toString());
              String motivo = env.getArgument("motivo");
              donacionRepository.buscarPorId(donacionId).ifPresent(d -> {
                try {
                  d.marcarEntregaFallida(motivo);
                  donacionRepository.guardar(d);
                } catch (IllegalStateException ignored) {}
                notificarPartes(d, "La entrega no pudo concretarse. Motivo: " + motivo);
                Notificable admin = () -> new Email("admin@donatrack.org");
                gestor.publicar(new EventoDeNotificacion(List.of(admin),
                    "Entrega fallida de la donacion " + d.getId() + ": " + motivo));
              });
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

  private void notificarPartes(Donacion d, String mensaje) {
    List<Notificable> destinatarios = new ArrayList<>();
    if (d.getDonante() != null) {
      destinatarios.add(d.getDonante());
    }
    if (d.getEntidadAsignada() != null) {
      destinatarios.add(d.getEntidadAsignada());
    }
    if (!destinatarios.isEmpty()) {
      gestor.publicar(new EventoDeNotificacion(destinatarios, mensaje));
    }
  }
}
