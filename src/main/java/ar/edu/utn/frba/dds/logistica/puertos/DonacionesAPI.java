package ar.edu.utn.frba.dds.logistica.puertos;

import ar.edu.utn.frba.dds.logistica.dto.DonacionParaRutaDTO;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoEntregaConfirmadaDTO;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoEntregaFallidaDTO;
import ar.edu.utn.frba.dds.logistica.dto.notificaciones.EventoInicioRutaDTO;

import java.util.List;

public interface DonacionesAPI {
    List<DonacionParaRutaDTO> obtenerDonacionesListasParaRepartir();
    
    void notificarInicioRuta(EventoInicioRutaDTO evento);
    void notificarEntregaConfirmada(EventoEntregaConfirmadaDTO evento);
    void notificarEntregaFallida(EventoEntregaFallidaDTO evento);
}
