package ar.edu.utn.frba.dds.donaciones.web;

import ar.edu.utn.frba.dds.donaciones.domain.*;
import ar.edu.utn.frba.dds.donaciones.notificaciones.EventoDeNotificacion;
import ar.edu.utn.frba.dds.donaciones.notificaciones.GestorDeNotificaciones;
import ar.edu.utn.frba.dds.donaciones.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Inicializa datos de demostración si la base está vacía para que todas las vistas
 * del Cliente Liviano (Público, Donante, Entidad Beneficiaria y Administradora)
 * puedan recorrerse y probarse de inmediato.
 */
public class DatosInicialesSeeder {

  public static void inicializarSiVacio(SubcategoriaRepository subcategoriaRepo,
                                        DonanteRepository donanteRepo,
                                        EntidadRepository entidadRepo,
                                        DonacionRepository donacionRepo,
                                        UsuarioRepository usuarioRepo,
                                        GestorDeNotificaciones gestor) {
    if (!usuarioRepo.obtenerTodos().isEmpty()) {
      return;
    }

    // 1. Subcategorías base
    Subcategoria fideos = subcategoriaRepo.obtenerOCrear("Fideos secos", Categoria.ALIMENTOS, false, true);
    Subcategoria arroz = subcategoriaRepo.obtenerOCrear("Arroz", Categoria.ALIMENTOS, false, true);
    Subcategoria leche = subcategoriaRepo.obtenerOCrear("Leche larga vida", Categoria.ALIMENTOS, false, true);
    Subcategoria harina = subcategoriaRepo.obtenerOCrear("Harina de trigo", Categoria.ALIMENTOS, false, true);
    Subcategoria aceite = subcategoriaRepo.obtenerOCrear("Aceite y legumbres", Categoria.ALIMENTOS, false, true);
    Subcategoria conservas = subcategoriaRepo.obtenerOCrear("Latas de conserva", Categoria.ALIMENTOS, false, true);

    Subcategoria camperas = subcategoriaRepo.obtenerOCrear("Camperas de abrigo", Categoria.VESTIMENTA, true, false);
    Subcategoria zapatillas = subcategoriaRepo.obtenerOCrear("Zapatillas y calzado escolar", Categoria.VESTIMENTA, true, false);
    Subcategoria ropaNinos = subcategoriaRepo.obtenerOCrear("Ropa y conjuntos infantiles", Categoria.VESTIMENTA, true, false);

    Subcategoria frazadas = subcategoriaRepo.obtenerOCrear("Frazadas y colchones", Categoria.MOBILIARIO, true, false);
    Subcategoria sillas = subcategoriaRepo.obtenerOCrear("Sillas y bancos escolares", Categoria.MOBILIARIO, true, false);
    Subcategoria mesas = subcategoriaRepo.obtenerOCrear("Mesas comunitarias", Categoria.MOBILIARIO, true, false);
    Subcategoria estanterias = subcategoriaRepo.obtenerOCrear("Estanterías y armarios", Categoria.MOBILIARIO, true, false);

    // 2. Personas Donantes
    Email emailAna = new Email("ana@mail.com");
    Telefono telAna = new Telefono("+54 11 5555-5555");
    PersonaHumana ana = new PersonaHumana(
        new ArrayList<>(List.of(emailAna, telAna)),
        emailAna,
        "Ana", "Pérez", 32, 35123456, "Femenino", "Av. Córdoba 2100, CABA"
    );
    donanteRepo.guardar(ana);

    Email emailArcos = new Email("contacto@arcosplateados.com");
    Telefono telArcos = new Telefono("+54 11 4444-4444");
    PersonaJuridica arcos = new PersonaJuridica(
        new ArrayList<>(List.of(emailArcos, telArcos)),
        emailArcos,
        "Arcos Plateados S.A.",
        TipoJuridico.EMPRESA,
        "Gastronomía y Alimentos",
        new ArrayList<>(List.of(new Representante("Martín", "Gómez", 28999111, "martin@arcosplateados.com")))
    );
    donanteRepo.guardar(arcos);

    // 3. Entidades Beneficiarias
    // Comedor Sonrisas con múltiples necesidades de distintas categorías (para probar paginación y filtros)
    NecesidadRecurrente necFideos = new NecesidadRecurrente(
        fideos, "Almuerzos semanales para 120 chicos del barrio", new PeriodoConsumo(100, "SEMANAL", 40)
    );
    NecesidadExtraordinaria necCamperas = new NecesidadExtraordinaria(
        camperas, "Campaña de invierno contra el frío", 50, 20
    );
    NecesidadRecurrente necArroz = new NecesidadRecurrente(
        arroz, "Arroz para guisos y comidas de invierno", new PeriodoConsumo(80, "MENSUAL", 35)
    );
    NecesidadRecurrente necLeche = new NecesidadRecurrente(
        leche, "Copa de leche diaria para el merendero", new PeriodoConsumo(120, "MENSUAL", 120) // Satisfecha
    );
    NecesidadExtraordinaria necAceite = new NecesidadExtraordinaria(
        aceite, "Aceite y legumbres para bolsones de alimentos familiares", 60, 20
    );
    NecesidadExtraordinaria necZapatillas = new NecesidadExtraordinaria(
        zapatillas, "Calzado escolar para niños y niñas de primaria", 40, 15
    );
    NecesidadExtraordinaria necMesas = new NecesidadExtraordinaria(
        mesas, "Mesas reforzadas para el salón comedor", 10, 4
    );
    NecesidadExtraordinaria necRopa = new NecesidadExtraordinaria(
        ropaNinos, "Conjuntos de ropa infantil de invierno", 45, 10
    );

    EntidadBeneficiaria comedorSonrisas = new EntidadBeneficiaria(
        "Comedor Infantil Escobar Sonrisas",
        "Av. Medrano 951, Almagro, CABA",
        "+54 11 4867-7500",
        new ArrayList<>(List.of("comedor@sonrisas.org")),
        new ArrayList<>(List.of(necFideos, necCamperas, necArroz, necLeche, necAceite, necZapatillas, necMesas, necRopa))
    );
    entidadRepo.guardar(comedorSonrisas);

    NecesidadExtraordinaria necSillas = new NecesidadExtraordinaria(
        sillas, "Reposición de mobiliario escolar tras inundación de aulas", 30, 10
    );
    NecesidadRecurrente necLecheEscuela = new NecesidadRecurrente(
        leche, "Copa de leche diaria para alumnos de primaria", new PeriodoConsumo(80, "MENSUAL", 30)
    );
    NecesidadExtraordinaria necEstanterias = new NecesidadExtraordinaria(
        estanterias, "Estanterías metálicas para biblioteca comunitaria", 8, 2
    );
    EntidadBeneficiaria escuelaRural = new EntidadBeneficiaria(
        "Escuela Comunitaria y Hogar N° 10",
        "Mozart 2300, Villa Lugano, CABA",
        "+54 11 4601-2200",
        new ArrayList<>(List.of("direccion@escuela10.org")),
        new ArrayList<>(List.of(necSillas, necLecheEscuela, necEstanterias))
    );
    entidadRepo.guardar(escuelaRural);

    NecesidadExtraordinaria necFrazadas = new NecesidadExtraordinaria(
        frazadas, "Refugio nocturno y asistencia hospitalaria solidaria", 40, 15
    );
    EntidadBeneficiaria hospitalSolidario = new EntidadBeneficiaria(
        "Red Solidaria Hospital Gandulfo",
        "Balcarce 351, Lomas de Zamora",
        "+54 11 4243-5555",
        new ArrayList<>(List.of("solidaria@gandulfo.org")),
        new ArrayList<>(List.of(necFrazadas))
    );
    entidadRepo.guardar(hospitalSolidario);

    NecesidadRecurrente necHarina = new NecesidadRecurrente(
        harina, "Panadería social y talleres de cocina para jóvenes", new PeriodoConsumo(90, "MENSUAL", 25)
    );
    EntidadBeneficiaria merenderoEsperanza = new EntidadBeneficiaria(
        "Merendero Los Chicos de la Esperanza",
        "Calle 14 n° 450, Berazategui",
        "+54 11 4256-8800",
        new ArrayList<>(List.of("merendero@esperanza.org")),
        new ArrayList<>(List.of(necHarina))
    );
    entidadRepo.guardar(merenderoEsperanza);

    NecesidadExtraordinaria necConservas = new NecesidadExtraordinaria(
        conservas, "Refuerzo alimentario de emergencia para familias", 70, 30
    );
    EntidadBeneficiaria hogarSanCayetano = new EntidadBeneficiaria(
        "Hogar Comunitario San Cayetano",
        "Av. San Martín 1200, Lanús Oeste",
        "+54 11 4241-1122",
        new ArrayList<>(List.of("hogar@sancayetano.org")),
        new ArrayList<>(List.of(necConservas))
    );
    entidadRepo.guardar(hogarSanCayetano);

    // 4. Donaciones en distintos estados para demostrar trazabilidad completa
    // Donación #1: ENTREGADA (aparece en el mapa público y galería sin login)
    Donacion d1 = new Donacion(fideos, 60, "paquetes", ana, LocalDate.now().minusDays(5));
    d1.asignarEntidad(comedorSonrisas);
    d1.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
    d1.avanzarHacia(EstadoDonacion.EN_TRASLADO);
    d1.avanzarHacia(EstadoDonacion.ENTREGADA);
    donacionRepo.guardar(d1);

    // Donación #2: EN_TRASLADO (aparece en el mapa en vivo para el donante y la entidad)
    Donacion d2 = new Donacion(camperas, 25, "unidades", ana, LocalDate.now().minusDays(2));
    d2.asignarEntidad(comedorSonrisas);
    d2.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
    d2.avanzarHacia(EstadoDonacion.EN_TRASLADO);
    donacionRepo.guardar(d2);

    // Donación #3: ASIGNACION_REALIZADA (lista para ruta / confirmación de recepción)
    Donacion d3 = new Donacion(arroz, 45, "kg", ana, LocalDate.now().minusDays(1));
    d3.asignarEntidad(comedorSonrisas);
    donacionRepo.guardar(d3);

    // Donación #4: EN_DEPOSITO (lista para ejecutar Matchmaking o marcar Vencida desde Admin)
    Donacion d4 = new Donacion(fideos, 80, "paquetes", arcos, LocalDate.now());
    donacionRepo.guardar(d4);

    // Donación #5: EN_DEPOSITO (mobiliario escolar donado por Arcos Plateados)
    Donacion d5 = new Donacion(sillas, 20, "unidades", arcos, LocalDate.now());
    donacionRepo.guardar(d5);

    // Donación #6: ENTREGADA en Escuela N° 10 (segundo marcador en el mapa público)
    Donacion d6 = new Donacion(leche, 50, "litros", arcos, LocalDate.now().minusDays(3));
    d6.asignarEntidad(escuelaRural);
    d6.avanzarHacia(EstadoDonacion.LISTA_PARA_ENTREGAR);
    d6.avanzarHacia(EstadoDonacion.EN_TRASLADO);
    d6.avanzarHacia(EstadoDonacion.ENTREGADA);
    donacionRepo.guardar(d6);

    // 5. Usuarios de acceso para los 3 roles (Admin, Donante, Entidad Beneficiaria)
    usuarioRepo.guardar(new Usuario(
        "admin@donatrack.org", "admin123", "Administración Depósito UTN",
        RolUsuario.ADMIN, null, null
    ));
    usuarioRepo.guardar(new Usuario(
        "ana@mail.com", "donante123", "Ana Pérez (Donante Humana)",
        RolUsuario.DONANTE, ana, null
    ));
    usuarioRepo.guardar(new Usuario(
        "contacto@arcosplateados.com", "donante123", "Arcos Plateados S.A. (Donante Jurídica)",
        RolUsuario.DONANTE, arcos, null
    ));
    usuarioRepo.guardar(new Usuario(
        "comedor@sonrisas.org", "entidad123", "Comedor Escobar Sonrisas",
        RolUsuario.ENTIDAD, null, comedorSonrisas
    ));
    usuarioRepo.guardar(new Usuario(
        "direccion@escuela10.org", "entidad123", "Escuela Comunitaria N° 10",
        RolUsuario.ENTIDAD, null, escuelaRural
    ));

    // 6. Notificaciones iniciales de demostración
    if (gestor != null) {
      gestor.publicar(new EventoDeNotificacion(
          List.of(comedorSonrisas),
          "Se te asignó la donación #" + d2.getId() + " (25 unidades de Camperas de abrigo). El camión AE-412-UT se encuentra en traslado."
      ));
      gestor.publicar(new EventoDeNotificacion(
          List.of(comedorSonrisas),
          "Confirmación registrada: recepción satisfactoria de la donación #" + d1.getId() + " (60 paquetes de Fideos secos) entregada por camión AE-412-UT."
      ));
      gestor.publicar(new EventoDeNotificacion(
          List.of(ana),
          "¡Tu donación #" + d1.getId() + " (60 paquetes de Fideos secos) fue recibida con éxito por Comedor Infantil Escobar Sonrisas!"
      ));
    }
  }
}
