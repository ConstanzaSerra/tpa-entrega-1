package ar.edu.utn.frba.dds.logistica.adaptadores;

import ar.edu.utn.frba.dds.logistica.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.puertos.DonacionesAPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class DonacionesHttpAdapter implements DonacionesAPI {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DonacionesHttpAdapter(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<DonacionParaRutaDTO> obtenerDonacionesListasParaRepartir() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/donaciones?estado=ASIGNACION_REALIZADA"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<DonacionParaRutaDTO>>() {});
            } else {
                System.err.println("Error al obtener donaciones, status code: " + response.statusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
