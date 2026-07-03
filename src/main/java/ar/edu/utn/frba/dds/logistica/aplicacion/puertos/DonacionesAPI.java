package ar.edu.utn.frba.dds.logistica.aplicacion.puertos;

import ar.edu.utn.frba.dds.logistica.infraestructura.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.logistica.infraestructura.dto.notificaciones.EventoInicioRutaDTO;

import java.util.List;

public interface DonacionesAPI {
    List<DonacionParaRutaDTO> obtenerDonacionesListasParaRepartir();

    void informarDonacionPlanificada(Long donacionId);

    void notificarInicioRuta(EventoInicioRutaDTO evento);
    void notificarEntregaConfirmada(EventoEntregaConfirmadaDTO evento);
    void notificarEntregaFallida(EventoEntregaFallidaDTO evento);
}
