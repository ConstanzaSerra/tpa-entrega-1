# DonaTrack — TP Anual Diseño de Sistemas de Información (Entrega 4)

Sistema integral de trazabilidad de donaciones, asignación inteligente (*matchmaking*) y logística de reparto con seguimiento satelital en tiempo real.

---

## Arquitectura del Sistema (Entrega 4)

El sistema está compuesto por dos microservicios Java 17 + Javalin 6 + Hibernate/JPA que se comunican mediante **GraphQL (`POST /graphql`)**:

1. **`donaciones` (`:8080`)**:
   - **Cliente Liviano Web MVC** renderizado en servidor con **Handlebars** (`donaciones/src/main/resources/templates/*.hbs`).
   - **Autenticación y Sesiones** mediante **Cookies HTTP (`donatrack_session`)** con tres roles: `ADMIN`, `DONANTE` y `ENTIDAD` (además de navegación pública sin sesión).
   - **API GraphQL (`POST /graphql`)** y API REST para gestión de donantes, donaciones, entidades beneficiarias, necesidades, matchmaking y notificaciones (Observer + bandeja web).
2. **`logistica` (`:8081`)**:
   - Gestión de flota de camiones, planificación de rutas por lotes, entregas con evidencia fotográfica y monitoreo GPS en tiempo real.
   - **API GraphQL (`POST /graphql`)** consumida por `donaciones` a través de `LogisticaGraphQLAdapter`, y adaptador `DonacionesGraphQLAdapter` para consultar donaciones asignadas y reportar inicios de ruta, entregas confirmadas y entregas fallidas a `donaciones`.

---

## Credenciales de Prueba Precargadas (`DatosInicialesSeeder`)

Al iniciar `DonacionesApp`, si la base está vacía se cargan automáticamente datos de demostración y usuarios para probar los 4 perfiles del enunciado:

| Rol | Email / Usuario | Contraseña | Vistas Principales |
| :--- | :--- | :--- | :--- |
| **Persona Administradora** | `admin@donatrack.org` | `admin123` | `/admin/deposito`, `/admin/matchmaking`, `/admin/camiones`, `/admin/importar-csv` |
| **Persona Donante (Humana)** | `ana@mail.com` | `donante123` | `/donante/donaciones`, `/donante/entidades`, `/donante/seguimiento` |
| **Persona Donante (Jurídica)** | `contacto@arcosplateados.com` | `donante123` | `/donante/donaciones`, `/donante/entidades`, `/donante/seguimiento` |
| **Entidad Beneficiaria** | `comedor@sonrisas.org` | `entidad123` | `/entidad/necesidades`, `/entidad/donaciones`, `/entidad/notificaciones`, `/entidad/seguimiento` |
| **Entidad Beneficiaria** | `direccion@escuela10.org` | `entidad123` | `/entidad/necesidades`, `/entidad/donaciones`, `/entidad/notificaciones`, `/entidad/seguimiento` |

*(También están disponibles los botones de acceso rápido con 1 clic en la pantalla `GET /login`).*

---

## Cómo Ejecutar el Proyecto

### Opción A: Ejecución Directa (sin requerir Docker instalado)
Los servicios necesitan una base PostgreSQL: si no pueden conectarse, **no arrancan** (*fail fast*), para que nunca quede un servicio funcionando sobre una base equivocada. Para probar sin PostgreSQL hay que pedir explícitamente la base en memoria (HSQLDB):

```bash
mvn clean package
PERSISTENCE_UNIT=simple-persistence-unit java -jar logistica/target/tpa-logistica-1.0-SNAPSHOT.jar
PERSISTENCE_UNIT=simple-persistence-unit java -jar donaciones/target/tpa-donaciones-1.0-SNAPSHOT.jar
```
Luego abrir en el navegador: **http://localhost:8080**

### Opción B: Con Docker Compose (PostgreSQL + ambos microservicios)
```bash
docker compose up --build
```
- Portal Web MVC + API Donaciones: `http://localhost:8080`
- Servicio de Logística + GraphQL: `http://localhost:8081/graphql`
- pgAdmin 4: `http://localhost:5050`

### Opción C: Despliegue en Nube (Railway)
El sistema está desplegado en **Railway** (PaaS). Cada servicio se construye con su `Dockerfile` multi-etapa ([`donaciones/Dockerfile`](donaciones/Dockerfile), [`logistica/Dockerfile`](logistica/Dockerfile)): la etapa de build corre `mvn clean package` y la imagen final solo lleva el JRE y el `.jar`. Las tareas programadas usan `Dockerfile.cron` con el *Cron Schedule* de Railway.

La interfaz completa entre el repositorio y Railway (qué define el repo, qué se configura en el panel y qué variables usa cada servicio) está en la [sección 7 de las justificaciones](docs/justificaciones-entrega-4.md#7-despliegue-en-nube--contenedores-requerimiento-detallado-4). Variables principales:
- `PORT`: puerto HTTP del servicio (`8080` / `8081`).
- `JDBC_DATABASE_URL`, `DB_USER`, `DB_PASSWORD`: conexión JDBC a PostgreSQL.
- `LOGISTICA_URL` (en `donaciones`) y `DONACIONES_URL` (en `logistica`).
- `CRON_API_KEY`: clave de los endpoints internos `/api/internal/cron/*`.
- `MOSTRAR_ACCESOS_DEMO` (opcional): con `true`, el login muestra los botones de acceso rápido de demo. En producción quedan ocultos.

---

## Documentación y Diagramas (`docs/`)

- [`docs/justificaciones-entrega-4.md`](docs/justificaciones-entrega-4.md): Justificaciones de arquitectura MVC + Handlebars, sesiones por Cookies HTTP, reemplazo de HTTP por GraphQL, bandeja de notificaciones web, importación CSV >10.000 filas y despliegue.
- [`docs/diagrama-de-clases-donaciones.puml`](docs/diagrama-clases-entrega-4.puml): Diagrama de clases actualizado del servicio de Donaciones.
- [`docs/diagrama-clases-logistica.puml`](docs/diagrama-clases-logistica.puml): Diagrama de clases actualizado del servicio de Logística.
- [`docs/diagrama-despliegue.puml`](docs/diagrama-despliegue.puml): Diagrama de despliegue actualizado con cliente liviano MVC, GraphQL y PostgreSQL cloud.
- [`docs/der-fisico.puml`](docs/der-fisico.puml): Modelo de datos físico (DER) de ambas bases relacionales.
