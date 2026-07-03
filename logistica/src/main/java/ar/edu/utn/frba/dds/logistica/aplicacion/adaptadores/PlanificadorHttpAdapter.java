package ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores;

import ar.edu.utn.frba.dds.logistica.infraestructura.dto.PlanificacionRequestDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.PlanificadorExternoAPI;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlanificadorHttpAdapter implements PlanificadorExternoAPI {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PlanificadorHttpAdapter(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void solicitarPlanificacion(PlanificacionRequestDTO requestDTO) {
        try {
            String jsonBody = objectMapper.writeValueAsString(requestDTO);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/planificar"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200 && response.statusCode() != 202) {
                            System.err.println("Error al solicitar planificacion. Status: " + response.statusCode());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
