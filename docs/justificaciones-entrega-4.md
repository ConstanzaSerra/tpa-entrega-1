# Justificaciones de Diseño — Entrega 4: Presentación, GraphQL y Despliegue

Este documento detalla las decisiones de arquitectura, diseño de interfaces, manejo de sesiones, comunicación entre microservicios y despliegue adoptadas para la **Entrega 4** de **DonaTrack**.

---

## 1. Arquitectura de Presentación: Cliente Liviano MVC con Motor de Plantillas (Handlebars)

### Opción Seleccionada
Se eligió la **Opción 1: Cliente Liviano con Arquitectura MVC y Motor de Plantillas Server-Side Rendering (Handlebars sobre Javalin 6)**.

### Justificación
1. **Cohesión con el Dominio y Reducción de Complejidad Accidental**: Al renderizar las vistas HTML directamente en el servidor principal (`donaciones`) usando `javalin-rendering` + `handlebars-java`, los controladores web (`WebPortalController`) interactúan directamente con los repositorios, servicios de dominio (`ProcesadorMatchmaking`, `ImportadorDonantesCSV`, `GestorDeNotificaciones`) y el puerto `LogisticaPort`, sin duplicar validaciones ni requerir un pipeline de compilación frontend (Node/Webpack) separado.
2. **Estructura MVC Clara**:
   - **Modelo (Model)**: Entidades de dominio (`Donacion`, `PersonaDonante`, `EntidadBeneficiaria`, `Necesidad`, `Usuario`, `NotificacionWeb`) y mapas de vista preparados para las plantillas.
   - **Vista (View)**: 17 plantillas `.hbs` modulares ubicadas en `donaciones/src/main/resources/templates/`, con un layout base compartido (`layout.hbs`) que adapta dinámicamente la barra de navegación según el rol autenticado (`Público`, `DONANTE`, `ENTIDAD`, `ADMIN`).
   - **Controlador (Controller)**: `WebPortalController`, que procesa las peticiones HTTP `GET`/`POST`, aplica control de acceso por rol mediante `SesionManager`, invoca la lógica de negocio y redirige o renderiza la vista correspondiente (patrón *Post-Redirect-Get* con mensajes de confirmación o error).
3. **Mapas Interactivos en Tiempo Real**: Tanto el mapa público de entregas realizadas (`GET /mapa-entregas`) como los mapas de seguimiento activo para Donantes (`GET /donante/seguimiento`) y Entidades Beneficiarias (`GET /entidad/seguimiento`) utilizan **Leaflet.js + OpenStreetMap**, mostrando marcadores diferenciados para depósitos, entidades beneficiarias y camiones en ruta con su patente, velocidad, coordenadas GPS y fecha/hora de última actualización.

---

## 2. Manejo de Sesiones y Autenticación: Cookies HTTP

### Opción Seleccionada
Se implementó el manejo de sesiones mediante **Cookies HTTP** (`donatrack_session`) gestionadas por `SesionManager` y respaldadas por la entidad persistente `Usuario` (`RolUsuario`: `ADMIN`, `DONANTE`, `ENTIDAD`).

### Justificación
1. **Integración Nativa con Server-Side Rendering (MVC)**: En una arquitectura de Cliente Liviano donde la navegación ocurre mediante enlaces `GET` y envíos de formularios `POST` estándar del navegador, el navegador adjunta automáticamente la cookie `donatrack_session` (`HttpOnly`) en cada request sin requerir JavaScript adicional para inyectar headers `Authorization`.
2. **Seguridad de Credenciales**:
   - Las contraseñas nunca se almacenan en texto plano; la entidad `Usuario` calcula el hash criptográfico **SHA-256** (`Usuario.hashear(password)`).
   - La cookie se configura con `HttpOnly = true` para mitigar accesos desde scripts maliciosos (XSS) y su token se valida en `SesionManager` en cada request protegido.
3. **Autorización Basada en Roles (RBAC)**:
   - Rutas `/donante/*` exigen `RolUsuario.DONANTE` (o `ADMIN`) y filtran los datos exclusivamente para el `PersonaDonante` asociado al usuario en sesión.
   - Rutas `/entidad/*` exigen `RolUsuario.ENTIDAD` (o `ADMIN`) y operan sobre la `EntidadBeneficiaria` asociada al usuario.
   - Rutas `/admin/*` exigen `RolUsuario.ADMIN`.

---

## 3. Reemplazo de HTTP/REST entre Microservicios por GraphQL (Requerimiento Detallado 3b)

### Opción Seleccionada
Se eligió la **Opción 2: GraphQL** (`com.graphql-java:graphql-java`) para reemplazar la comunicación HTTP/REST tradicional entre el microservicio de **Donaciones** (`:8080`) y el microservicio de **Logística** (`:8081`).

### Justificación y Diseño
1. **Desacoplamiento mediante Puertos y Adaptadores (Arquitectura Hexagonal)**:
   - En **Logística**, el dominio y el `PlanificadorRutas` dependen únicamente de la interfaz puerto `DonacionesAPI`. Se implementó `DonacionesGraphQLAdapter`, que envía *queries* y *mutations* GraphQL a `POST http://donaciones:8080/graphql` (`DonacionesGraphQLController`).
   - En **Donaciones**, el `WebPortalController` depende de la interfaz puerto `LogisticaPort`. Se implementó `LogisticaGraphQLAdapter`, que consulta camiones, rutas, entregas y posiciones GPS, y ejecuta mutaciones (alta/baja de camiones, planificación de rutas, confirmación de recepción con fotos y reporte de posición GPS) contra `POST http://logistica:8081/graphql` (`LogisticaGraphQLController`).
2. **Selección Exacta de Campos (Sin Over-fetching ni Under-fetching)**:
   - Cuando el portal web necesita mostrar el mapa de seguimiento de entregas activas, una única *query* GraphQL obtiene simultáneamente `entregas`, `rutas` y `ultimasPosiciones` con únicamente los campos requeridos por la vista.
   - Cuando Logística planifica rutas o confirma una entrega, ejecuta mutaciones tipadas (`donacionesPorEstado`, `informarDonacionPlanificada`, `notificarInicioRuta`, `notificarEntregaConfirmada`, `notificarEntregaFallida`) sobre un único endpoint `/graphql`.
3. **Tolerancia a Fallos (Graceful Fallback)**:
   - `LogisticaGraphQLAdapter` cuenta con estado local de respaldo en caso de que el microservicio `logistica` no se encuentre levantado durante una prueba aislada del servicio `donaciones`, garantizando que todas las vistas del cliente liviano sigan siendo operativas.

---

## 4. Bandeja de Notificaciones Web (Patrón Observer Extendido)

Para cumplir con el requerimiento de que la Entidad Beneficiaria pueda **recibir y visualizar notificaciones en la plataforma web** cuando se le asigna una donación o se confirma una recepción:
- Se mantuvo intacto el patrón **Observer** (`GestorDeNotificaciones` + `ObservadorDeNotificaciones`).
- Se agregó un nuevo observador concreto: `RegistradorNotificacionesWeb`, que implementa `ObservadorDeNotificaciones` y persiste cada evento en la tabla `notificacion_web` (`NotificacionWebRepository`) asociado a la `EntidadBeneficiaria` o `PersonaDonante` destinataria.
- Así, cuando ocurre una asignación o confirmación de entrega, el sistema dispara **en simultáneo** el envío por el canal externo preferido (`EnviadorPorMedioPreferido`: Email/SMS/WhatsApp) y el registro en la bandeja web consultable en `GET /entidad/notificaciones`.

---

## 5. Importación Masiva de Donantes (>10.000 filas CSV) con Otorgamiento de Usuario

Para el requerimiento 17 de la Persona Administradora:
- Se implementó `ImportadorDonantesCSV.importarMasivoConUsuarios(Reader, UsuarioRepository)` que procesa archivos CSV de más de **10.000 filas** en una única transacción JPA por lotes y precarga en memoria un índice `Map<String, PersonaDonante>` y `Set<String>` de emails existentes para evitar $O(N)$ consultas SQL individuales.
- Por cada fila importada, si el donante no poseía cuenta de usuario en la plataforma, se le crea automáticamente su entidad `Usuario` con `RolUsuario.DONANTE` y contraseña inicial igual a su documento.
- La pantalla `GET /admin/importar-csv` incluye además un generador descargable (`GET /admin/importar-csv/ejemplo-10000.csv`) que produce al vuelo un archivo CSV válido con **10.050 filas** listo para probar la carga masiva desde el navegador.

---

## 6. Navegabilidad y Maquetado de las 17 Pantallas

| Perfil | Pantalla / Requerimiento | Ruta HTTP | Plantilla Handlebars |
| :--- | :--- | :--- | :--- |
| **No autenticado** | 1. Landing page institucional + enlaces públicos | `GET /`, `GET /galeria`, `GET /legal` | `landing.hbs`, `galeria.hbs`, `legal.hbs` |
| **No autenticado** | 2. Mapa público de donaciones entregadas con fotos | `GET /mapa-entregas` | `mapa-entregas.hbs` |
| **No autenticado** | 3. Registro de donantes (humana/jurídica) y entidades | `GET /registro`, `POST /registro/donante`, `POST /registro/entidad` | `registro.hbs` |
| **No autenticado** | 4. Inicio y cierre de sesión (Cookies HTTP) | `GET /login`, `POST /login`, `POST /logout` | `login.hbs` |
| **Persona Donante** | 5. Filtrar mis donaciones por estado, categoría y subcategoría | `GET /donante/donaciones` | `donante-donaciones.hbs` |
| **Persona Donante** | 6. Navegar por las entidades beneficiarias registradas | `GET /donante/entidades` | `donante-entidades.hbs` |
| **Persona Donante** | 7. Mapa de seguimiento de entregas activas y camiones GPS | `GET /donante/seguimiento` | `seguimiento-mapa.hbs` |
| **Entidad Beneficiaria** | 8. Registrar necesidades materiales (extraordinarias/recurrentes) | `GET /entidad/necesidades`, `POST /entidad/necesidades` | `entidad-necesidades.hbs` |
| **Entidad Beneficiaria** | 9. Ver estado de donaciones asignadas a la entidad | `GET /entidad/donaciones` | `entidad-donaciones.hbs` |
| **Entidad Beneficiaria** | 10. Confirmar recepción de donación cargando fotos | `POST /entidad/donaciones/{id}/confirmar` | `entidad-donaciones.hbs` |
| **Entidad Beneficiaria** | 11. Bandeja de notificaciones web (asignación y recepción) | `GET /entidad/notificaciones` | `entidad-notificaciones.hbs` |
| **Entidad Beneficiaria** | 12. Mapa de seguimiento de entregas en camino a la entidad | `GET /entidad/seguimiento` | `seguimiento-mapa.hbs` |
| **Administradora** | 13. Registrar donantes y donaciones recibidas en depósito | `GET /admin/deposito`, `POST /admin/donantes`, `POST /admin/donaciones` | `admin-deposito.hbs` |
| **Administradora** | 14. Marcar como vencidas donaciones disponibles en depósito | `POST /admin/donaciones/{id}/vencer` | `admin-deposito.hbs` |
| **Administradora** | 15. Ejecutar algoritmos de matchmaking y seleccionar entidad final | `GET /admin/matchmaking`, `POST /admin/donaciones/{id}/matchmaking`, `POST /admin/donaciones/{id}/asignar` | `admin-matchmaking.hbs` |
| **Administradora** | 16. Administrar flota de camiones, rutas y posición GPS | `GET /admin/camiones`, `POST /admin/camiones`, `POST /admin/camiones/{id}/eliminar` | `admin-camiones.hbs` |
| **Administradora** | 17. Importar >10.000 donantes desde CSV otorgándoles usuario | `GET /admin/importar-csv`, `POST /admin/importar-csv` | `admin-csv.hbs` |

---

## 7. Despliegue en Nube / Contenedores (Requerimiento Detallado 4)

Se preparó el sistema para las dos modalidades admitidas por la cátedra:
1. **Despliegue Cloud PaaS (Render / Railway + Neon PostgreSQL)**:
   - Se incluyeron `donaciones/Dockerfile`, `logistica/Dockerfile` (multi-stage build con Maven 3.9 + Eclipse Temurin JRE 17 Alpine) y el blueprint `render.yaml`.
   - `EntityManagerProvider` en ambos microservicios lee dinámicamente `JDBC_DATABASE_URL`, `DB_USER` y `DB_PASSWORD`, permitiendo conectarse a cualquier base PostgreSQL administrada en la nube (Neon.tech, Supabase, Render PostgreSQL) o usar automáticamente HSQLDB en memoria cuando se ejecuta localmente sin Docker.
2. **Despliegue Local Contenerizado + Exposición Pública (Docker Compose / Cloudflare Tunnels)**:
   - `docker-compose.yml` levanta la arquitectura completa (`db`, `pgadmin`, `donaciones`, `logistica`) en una red interna donde `donaciones` y `logistica` se comunican vía GraphQL (`http://logistica:8081/graphql` y `http://donaciones:8080/graphql`).
   - Puede exponerse públicamente al instante mediante `cloudflared tunnel --url http://localhost:8080`.
