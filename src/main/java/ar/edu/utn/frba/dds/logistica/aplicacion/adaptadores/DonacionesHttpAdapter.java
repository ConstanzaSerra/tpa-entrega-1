package ar.edu.utn.frba.dds.logistica.aplicacion.adaptadores;

import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;
import ar.edu.utn.frba.dds.logistica.aplicacion.puertos.DonacionesAPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
        this.objectMapper.registerModule(new JavaTimeModule()); // Para soportar LocalDateTime
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

    @Override
    public void notificarInicioRuta(EventoInicioRutaDTO evento) {
        enviarNotificacionAsync("/notificaciones/inicio-ruta", evento);
    }

    @Override
    public void notificarEntregaConfirmada(EventoEntregaConfirmadaDTO evento) {
        enviarNotificacionAsync("/notificaciones/entrega-confirmada", evento);
    }

    @Override
    public void notificarEntregaFallida(EventoEntregaFallidaDTO evento) {
        enviarNotificacionAsync("/notificaciones/entrega-fallida", evento);
    }

    private void enviarNotificacionAsync(String endpoint, Object payload) {
        try {
            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            System.err.println("Fallo al notificar " + endpoint + ". Status: " + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Excepcion al notificar " + endpoint + ": " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Error serializando payload para " + endpoint + ": " + e.getMessage());
        }
    }
}
