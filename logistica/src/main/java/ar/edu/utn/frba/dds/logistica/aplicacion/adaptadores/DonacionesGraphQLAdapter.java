package ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores;

import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entrega 4 - Requerimiento detallado 3b:
 * Adaptador GraphQL que reemplaza a DonacionesHttpAdapter para las comunicaciones
 * entre el microservicio de Logística y el de Donaciones.
 */
public class DonacionesGraphQLAdapter implements DonacionesAPI {
    private final String graphqlEndpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DonacionesGraphQLAdapter(String baseUrl) {
        String normalizada = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.graphqlEndpoint = normalizada.endsWith("/graphql") ? normalizada : normalizada + "/graphql";
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public List<DonacionParaRutaDTO> obtenerDonacionesListasParaRepartir() {
        String query = """
            query($estado: String!) {
              donacionesPorEstado(estado: $estado) {
                donacionId
                entidadBeneficiariaId
                direccionDestino
              }
            }
            """;
        Map<String, Object> variables = Map.of("estado", "ASIGNACION_REALIZADA");

        try {
            String body = objectMapper.writeValueAsString(Map.of("query", query, "variables", variables));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphqlEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root.path("data").path("donacionesPorEstado");
                if (items.isArray()) {
                    List<DonacionParaRutaDTO> resultado = new ArrayList<>();
                    for (JsonNode n : items) {
                        resultado.add(new DonacionParaRutaDTO(
                                n.path("donacionId").asLong(),
                                n.path("entidadBeneficiariaId").asLong(),
                                n.path("direccionDestino").isNull() ? null : n.path("direccionDestino").asText()
                        ));
                    }
                    return resultado;
                }
            }
        } catch (Exception e) {
            System.err.println("Error GraphQL obteniendo donaciones: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void informarDonacionPlanificada(Long donacionId) {
        String mutation = """
            mutation($donacionId: ID!) {
              informarDonacionPlanificada(donacionId: $donacionId)
            }
            """;
        ejecutarMutacionAsync(mutation, Map.of("donacionId", donacionId));
    }

    @Override
    public void notificarInicioRuta(EventoInicioRutaDTO evento) {
        String mutation = """
            mutation($rutaId: ID, $patenteCamion: String, $linkMapa: String, $entregasAfectadas: [EntregaAfectadaInput!]!) {
              notificarInicioRuta(rutaId: $rutaId, patenteCamion: $patenteCamion, linkMapa: $linkMapa, entregasAfectadas: $entregasAfectadas)
            }
            """;
        List<Map<String, Object>> entregas = new ArrayList<>();
        if (evento.entregasAfectadas != null) {
            evento.entregasAfectadas.forEach(ea -> {
                Map<String, Object> m = new HashMap<>();
                m.put("entregaId", ea.entregaId);
                m.put("donacionId", ea.donacionId);
                m.put("entidadId", ea.entidadId);
                entregas.add(m);
            });
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("rutaId", evento.rutaId);
        vars.put("patenteCamion", evento.patenteCamion);
        vars.put("linkMapa", evento.linkMapa);
        vars.put("entregasAfectadas", entregas);
        ejecutarMutacionAsync(mutation, vars);
    }

    @Override
    public void notificarEntregaConfirmada(EventoEntregaConfirmadaDTO evento) {
        String mutation = """
            mutation($entregaId: ID, $donacionId: ID!, $patenteCamion: String, $fechaHora: String) {
              notificarEntregaConfirmada(entregaId: $entregaId, donacionId: $donacionId, patenteCamion: $patenteCamion, fechaHora: $fechaHora)
            }
            """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("entregaId", evento.entregaId);
        vars.put("donacionId", evento.donacionId);
        vars.put("patenteCamion", evento.patenteCamion);
        vars.put("fechaHora", evento.fechaHora != null ? evento.fechaHora.toString() : "");
        ejecutarMutacionAsync(mutation, vars);
    }

    @Override
    public void notificarEntregaFallida(EventoEntregaFallidaDTO evento) {
        String mutation = """
            mutation($entregaId: ID, $donacionId: ID!, $motivo: String) {
              notificarEntregaFallida(entregaId: $entregaId, donacionId: $donacionId, motivo: $motivo)
            }
            """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("entregaId", evento.entregaId);
        vars.put("donacionId", evento.donacionId);
        vars.put("motivo", evento.motivo);
        ejecutarMutacionAsync(mutation, vars);
    }

    private void ejecutarMutacionAsync(String mutation, Map<String, Object> variables) {
        try {
            String jsonBody = objectMapper.writeValueAsString(Map.of("query", mutation, "variables", variables));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphqlEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            System.err.println("Fallo mutacion GraphQL. Status: " + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Excepcion en mutacion GraphQL: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Error serializando mutacion GraphQL: " + e.getMessage());
        }
    }
}
