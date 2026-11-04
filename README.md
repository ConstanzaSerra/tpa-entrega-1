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
Ambos servicios detectan automáticamente si PostgreSQL está disponible en `localhost:5432`; si no lo está, utilizan **HSQLDB en memoria** automáticamente para que puedas levantar y probar toda la interfaz web de inmediato:

```bash
mvn clean package
java -jar logistica/target/tpa-logistica-1.0-SNAPSHOT.jar
java -jar donaciones/target/tpa-donaciones-1.0-SNAPSHOT.jar
```
Luego abrir en el navegador: **http://localhost:8080**

### Opción B: Con Docker Compose (PostgreSQL + ambos microservicios)
```bash
docker compose up --build
```
- Portal Web MVC + API Donaciones: `http://localhost:8080`
- Servicio de Logística + GraphQL: `http://localhost:8081/graphql`
- pgAdmin 4: `http://localhost:5050`

### Opción C: Despliegue en Nube (Render / Railway / Neon PostgreSQL)
Se incluye el blueprint [`render.yaml`](render.yaml) y los `Dockerfile` multi-stage en [`donaciones/Dockerfile`](donaciones/Dockerfile) y [`logistica/Dockerfile`](logistica/Dockerfile). Variables de entorno soportadas:
- `PORT`: puerto HTTP del servicio (`8080` / `8081`).
- `JDBC_DATABASE_URL`, `DB_USER`, `DB_PASSWORD`: conexión JDBC a PostgreSQL cloud.
- `LOGISTICA_URL` (en `donaciones`) y `DONACIONES_URL` (en `logistica`).

---

## Documentación y Diagramas (`docs/`)

- [`docs/justificaciones-entrega-4.md`](docs/justificaciones-entrega-4.md): Justificaciones de arquitectura MVC + Handlebars, sesiones por Cookies HTTP, reemplazo de HTTP por GraphQL, bandeja de notificaciones web, importación CSV >10.000 filas y despliegue.
- [`docs/diagrama-de-clases-donaciones.puml`](docs/diagrama-de-clases-donaciones.puml): Diagrama de clases actualizado del servicio de Donaciones.
- [`docs/diagrama-clases-logistica.puml`](docs/diagrama-clases-logistica.puml): Diagrama de clases actualizado del servicio de Logística.
- [`docs/diagrama-despliegue.puml`](docs/diagrama-despliegue.puml): Diagrama de despliegue actualizado con cliente liviano MVC, GraphQL y PostgreSQL cloud.
- [`docs/der-fisico.puml`](docs/der-fisico.puml): Modelo de datos físico (DER) de ambas bases relacionales.
