package ar.edu.utn.frba.dds.logistica.puertos;

import ar.edu.utn.frba.dds.logistica.dto.DonacionParaRutaDTO;
import java.util.List;

public interface DonacionesAPI {
    List<DonacionParaRutaDTO> obtenerDonacionesListasParaRepartir();
}
