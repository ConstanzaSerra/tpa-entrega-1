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

### 2.1 Evolución / Alternativa con Proveedor de Identidad Externo (Keycloak / OIDC)
Como alternativa y evolución del manejo local de credenciales, el diseño permite delegar la autenticación en un proveedor de identidad externo (*Identity Provider - IdP*) como **Keycloak** mediante **OAuth 2.0 / OpenID Connect (OIDC)**:
- **Flujo Authorization Code**: Al ingresar a `/login`, el servidor redirige al endpoint de autorización del *realm* de Keycloak (`/realms/donatrack/protocol/openid-connect/auth`). Luego de autenticar al usuario, Keycloak redirige al callback de nuestra aplicación con un `code` que el backend intercambia por un **ID Token / Access Token (JWT)** firmado por Keycloak.
- **Impacto en el Modelo**: La tabla `usuario` deja de almacenar `password_hash` y pasa a vincular el identificador externo del sujeto (`sub` o `email` validado por Keycloak) y sus roles de *realm* (`ADMIN`, `DONANTE`, `ENTIDAD`) con el `PersonaDonante` o `EntidadBeneficiaria` del dominio.
- **Trade-off**: Delegar en Keycloak aporta SSO, doble factor (MFA), políticas de rotación de contraseñas y recuperación de cuenta sin código propio, a cambio de operar un servicio adicional (contenedor Keycloak + base de datos de identidad) en la infraestructura cloud. Para el despliegue en Railway se priorizó la autenticación liviana integrada para minimizar el consumo de recursos del PaaS.

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

### 7.1 Dónde está desplegado

El sistema está desplegado en **Railway**, un proveedor **PaaS**: se le entrega el código y la plataforma se encarga de construir, ejecutar, reiniciar y publicar los servicios con HTTPS. El proyecto de Railway tiene:

| Servicio en Railway | Carpeta raíz del repo | Cómo se construye | Qué hace |
|---|---|---|---|
| `donaciones` | `donaciones/` | `donaciones/Dockerfile` | Cliente liviano MVC, API GraphQL de donaciones. Único servicio público. |
| `logistica` | `logistica/` | `logistica/Dockerfile` | Camiones, rutas, GPS y API GraphQL de logística. |
| Cron de donaciones | `donaciones/` | `donaciones/Dockerfile.cron` | Dispara el matchmaking nocturno y la detección de inactividad. |
| Cron de logística | `logistica/` | `logistica/Dockerfile.cron` | Dispara la planificación de rutas. |
| Base de datos | — | Servicio administrado | PostgreSQL administrado en Railway (`donatrack_donaciones` y `donatrack_logistica`). |

### 7.2 Interfaz entre el repositorio y Railway

Qué queda definido **en el repositorio** y qué se configura **en el panel de Railway**:

| Qué | Dónde se define |
|---|---|
| Cómo se compila (el equivalente a `npm run build`) | En el repo: etapa 1 de cada `Dockerfile`, `RUN mvn clean package -DskipTests` |
| Cómo se arranca (el equivalente a `npm start`) | En el repo: `ENTRYPOINT ["java", "-jar", "app.jar"]` |
| Imagen final y versión de Java | En el repo: etapa 2, `eclipse-temurin:17-jre-alpine` (solo el JRE y el `.jar`) |
| Qué tareas corre cada cron | En el repo: `docker/ejecutar-tareas.sh` de cada servicio |
| Qué carpeta es cada servicio (*Root Directory*) | Panel de Railway |
| Qué Dockerfile usa el cron (`Dockerfile.cron`) | Panel de Railway (*Dockerfile Path*) |
| Cada cuánto corren los cron (*Cron Schedule*) | Panel de Railway (expresión cron UTC, ej. `0 3 * * *` para 00:00 ART en donaciones y `0 5 * * *` en logística) |
| Variables de entorno y secretos | Panel de Railway (nunca en el repo) |
| Cuándo se redespliega | Panel de Railway: cada push a la rama conectada |

Railway no necesita un comando de build aparte: **si encuentra un `Dockerfile` en la carpeta del servicio, siempre construye con él**. Por eso el `Dockerfile` es el contrato entre el repo y la plataforma. Es el mismo archivo que usa `docker compose` en local, así que lo que anda en local se construye igual en la nube.

**Build multi-etapa.** La primera etapa usa una imagen con Maven para compilar. La segunda copia únicamente el `.jar` sobre una imagen con solo el JRE. Así la imagen que corre en producción no incluye Maven ni el código fuente: es más liviana y expone menos superficie.

**Variables de entorno:**

| Variable | Servicio | Para qué |
|---|---|---|
| `PORT` | ambos | Puerto HTTP (Railway lo asigna) |
| `JDBC_DATABASE_URL`, `DB_USER`, `DB_PASSWORD` | ambos | Conexión a PostgreSQL. Si faltan, el servicio no arranca (*fail fast*) |
| `LOGISTICA_URL` | donaciones y su cron | Dirección del servicio de logística |
| `DONACIONES_URL` | logística y cron de donaciones | Dirección del servicio de donaciones |
| `CRON_API_KEY` | servicios y cron | Clave que exigen los endpoints `/api/internal/cron/*` |
| `MOSTRAR_ACCESOS_DEMO` | donaciones | Opcional. Con `true` el login muestra los accesos rápidos de demo. Por defecto están ocultos, porque exponen contraseñas |

**Tareas programadas.** El contenedor cron no tiene un demonio `cron` adentro: Railway lo enciende a la hora configurada, el script hace un `POST` al endpoint interno con la clave `CRON_API_KEY` y el contenedor termina. Así el servicio principal no necesita un temporizador propio que se pierda en cada reinicio.

**¿Por qué no hay un `railway.json` en el repo?** Railway marcó como obsoleto su archivo de configuración (`railway.json` / `railway.toml`) y deja de leerlo el 1/12/2026. El reemplazo es un archivo de infraestructura como código en TypeScript (`.railway/railway.ts`), que no tiene sentido en un proyecto Java. Por eso lo que el `Dockerfile` no cubre se documenta en esta sección.

### 7.3 Comparación de opciones evaluadas

| Opción | Tipo | A favor | En contra |
|---|---|---|---|
| **Railway** + PostgreSQL administrado (elegida) | PaaS | Despliegue directo desde GitHub, construye con nuestro `Dockerfile`, HTTPS incluido, tareas programadas nativas | Plan con crédito limitado. Menos control sobre la máquina. Dependencia de un proveedor extranjero |
| Render + Neon / Supabase | PaaS | Similar a Railway, con plan gratuito | En el plan gratuito el servicio se duerme por inactividad (*cold start*) |
| Planes gratuitos de AWS / Azure / Oracle | IaaS | Control total de la máquina virtual | Hay que instalar y mantener el sistema operativo, Java, certificados y seguridad. Suelen pedir tarjeta de crédito |
| Servidores del DISILAB | Infraestructura de la facultad | Los datos quedan en la universidad | Depende de coordinar acceso y disponibilidad |
| Local + Cloudflare Tunnel | Equipo propio | Máxima **soberanía digital**: los datos no salen de nuestro equipo | Si se apaga la máquina, se cae el sistema. La cátedra lo acepta solo bajo condiciones |

Elegimos un PaaS porque el objetivo de la entrega es el diseño del sistema, no administrar servidores. A cambio, los datos quedan en infraestructura de una empresa extranjera, bajo sus condiciones y su jurisdicción. Esa es la contracara en términos de privacidad y soberanía digital frente a las opciones locales o de la facultad.

### 7.4 Ejecución local

- `docker-compose.yml` levanta la arquitectura completa (`db`, `pgadmin`, `donaciones`, `logistica`) en una red interna. `donaciones` y `logistica` se comunican vía GraphQL (`http://logistica:8081/graphql` y `http://donaciones:8080/graphql`).
- Sin Docker, los servicios necesitan una base configurada: si no hay conexión, no arrancan (*fail fast*). Para pruebas rápidas se puede elegir explícitamente la unidad de persistencia en memoria (`PERSISTENCE_UNIT=simple-persistence-unit`).
