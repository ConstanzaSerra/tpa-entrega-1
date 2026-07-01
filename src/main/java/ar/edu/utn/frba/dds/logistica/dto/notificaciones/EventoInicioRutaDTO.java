package ar.edu.utn.frba.dds.logistica.dto.notificaciones;

import java.util.List;

public class EventoInicioRutaDTO {
    public Long rutaId;
    public String patenteCamion;
    public String linkMapa;
    public List<EntregaAfectadaDTO> entregasAfectadas;

    public EventoInicioRutaDTO() {}

    public EventoInicioRutaDTO(Long rutaId, String patenteCamion, String linkMapa, List<EntregaAfectadaDTO> entregasAfectadas) {
        this.rutaId = rutaId;
        this.patenteCamion = patenteCamion;
        this.linkMapa = linkMapa;
        this.entregasAfectadas = entregasAfectadas;
    }
}
