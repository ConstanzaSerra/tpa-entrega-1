package ar.edu.utn.frba.dds.donaciones.puertos;

import java.util.List;

/**
 * Puerto de comunicación desde el servidor principal (Donaciones + Cliente Liviano MVC)
 * hacia el microservicio de Logística (servicio extraído en Entrega 2).
 */
public interface LogisticaPort {

  record CamionVista(Long id, String patente, double volumenM3, double alturaM, double capacidadKg) {}

  record EntregaVista(
      Long id,
      Long donacionId,
      Long entidadBeneficiariaId,
      String direccionDestino,
      String estado,
      String patenteCamion,
      String fechaHoraEntrega,
      List<String> fotosUrl
  ) {}

  record RutaVista(
      Long id,
      Long camionId,
      String patenteCamion,
      String estado,
      String linkMapa,
      List<Long> donacionesIds
  ) {}

  record SeguimientoCamionVista(
      Long camionId,
      String patente,
      Double latitud,
      Double longitud,
      Double velocidad,
      String ultimaActualizacion,
      Long rutaId,
      String estadoRuta,
      int avancePorcentaje,
      int entregasTotal,
      int entregasFinalizadas,
      List<Long> donacionesIds
  ) {}

  List<CamionVista> obtenerCamiones();

  CamionVista crearCamion(String patente, double volumenM3, double alturaM, double capacidadKg);

  boolean eliminarCamion(Long id);

  List<EntregaVista> obtenerEntregas();

  List<RutaVista> obtenerRutas();

  List<SeguimientoCamionVista> obtenerSeguimientoCamiones();

  boolean crearRutaDirecta(Long camionId, Long entidadId, String direccion, Long donacionId);

  boolean iniciarRuta(Long rutaId);

  boolean confirmarEntregaConFoto(Long donacionId, Long entidadId, String direccion, String fotoUrl);

  boolean reportarPosicionCamion(Long camionId, double latitud, double longitud, double velocidad);
}
