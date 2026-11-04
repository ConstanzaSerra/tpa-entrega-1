# Guía Completa para el Coloquio — Entrega 3 (Persistencia) y Entrega 4 (Presentación, GraphQL y Despliegue)

Esta guía está pensada para defender en grupo tanto la **Entrega 3** como la **Entrega 4** desde cero, explicando cada concepto teórico en lenguaje simple, cómo se aplicó en **DonaTrack**, qué archivos abrir si el docente pide ver código y las **preguntas típicas de coloquio con sus respuestas**.

---

# PARTE 1: ENTREGA 3 — PERSISTENCIA Y ORM (Hibernate / JPA)

## 1. Conceptos básicos que tenés que tener clarísimos

### ¿Qué es el "Desajuste de Impedancia" (Impedance Mismatch)?
Es el problema principal que resuelve la Entrega 3:
- En **Java (Objetos)** tenemos herencia, polimorfismo, listas de objetos, referencias en memoria y encapsulamiento.
- En **PostgreSQL (Relacional)** solo existen tablas con filas, columnas de tipos primitivos, claves primarias (`PK`) y claves foráneas (`FK`). No existe la herencia ni las listas dentro de una celda.
- **¿Cómo lo resolvemos?** Usando un **ORM** (*Object-Relational Mapping*): **Hibernate** a través de la especificación estándar **JPA** (`javax.persistence`).

### ¿Qué diferencia hay entre una Entidad (`@Entity`) y un Objeto de Valor / Embebible (`@Embeddable`)?
Esta es **pregunta fija de coloquio** en DDS:
- **Entidad (`@Entity`)**: Tiene **identidad propia** (`@Id`). Importa *quién es* a lo largo del tiempo aunque cambien sus atributos. Ejemplos: [`Donacion`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Donacion.java), [`PersonaDonante`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/PersonaDonante.java), [`EntidadBeneficiaria`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/EntidadBeneficiaria.java), [`Camion`](../logistica/src/main/java/ar/edu/utn/frba/dds/logistica/dominio/Camion.java).
- **Objeto de Valor / Embebible (`@Embeddable`)**: **NO tiene identidad propia (`@Id`)**. Solo importa el conjunto de sus datos y su ciclo de vida depende 100% de la entidad que lo contiene. Nadie lo busca ni lo referencia desde afuera por ID.
  - **Caso 1: Un único valor embebido (`@Embedded`)**: Sus columnas se guardan **adentro de la misma tabla** de la entidad dueña (sin crear otra tabla ni hacer `JOIN`).
    - *Ejemplo en nuestro código*: [`PeriodoConsumo`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/PeriodoConsumo.java) dentro de [`NecesidadRecurrente`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/NecesidadRecurrente.java) (`periodo_cantidad_objetivo`, `periodo_descripcion`, `periodo_cantidad_recibida`).
  - **Caso 2: Una colección de valores embebidos (`@ElementCollection`)**: Como es una lista (`1..*`), en relacional no puede ir en la misma fila (violaría la 1ra Forma Normal). Entonces JPA crea una **tabla auxiliar sin ID propio**, vinculada por una `FK` hacia la entidad dueña (`@CollectionTable`).
    - *Ejemplos en nuestro código*:
      1. `List<CambioDeEstado> historial` en [`Donacion.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Donacion.java) (tabla `cambio_de_estado` con `@OrderColumn`).
      2. `List<Representante> representantes` en [`PersonaJuridica.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/PersonaJuridica.java) (tabla `representante`).
      3. `List<String> emailsRepresentantes` en [`EntidadBeneficiaria.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/EntidadBeneficiaria.java) y `List<String> fotosUrl` en [`Entrega.java`](../logistica/src/main/java/ar/edu/utn/frba/dds/logistica/dominio/Entrega.java).

---

## 2. Mapeo de Herencia en JPA (¡Pregunta clave del coloquio!)

En JPA existen **3 estrategias** para mapear una jerarquía de herencia a tablas:
1. **`SINGLE_TABLE` (Una sola tabla para toda la jerarquía)**:
   - Crea una única tabla con todas las columnas de la clase padre y de todas las clases hijas, más una **columna discriminadora** (`@DiscriminatorColumn`) que indica de qué subclase es cada fila.
   - *Ventaja*: Es la más rápida para consultas polimórficas (no hace `JOIN` ni `UNION`).
   - *Desventaja*: Las columnas propias de cada hija quedan en `NULL` para las filas de las otras hijas (no se les puede poner `NOT NULL` en la base).
2. **`JOINED` (Una tabla para el padre y una tabla por cada hija)**:
   - La tabla padre guarda los campos comunes y cada tabla hija guarda solo sus campos propios, compartiendo la `PK`/`FK` (`@PrimaryKeyJoinColumn`).
   - *Ventaja*: Diseño normalizado, sin columnas `NULL` de otras subclases; permite poner `NOT NULL` en los atributos de las hijas (ej. `razon_social` en `PersonaJuridica`).
   - *Desventaja*: Requiere hacer `JOIN` entre la tabla padre y la hija al consultar.
3. **`TABLE_PER_CLASS` (Una tabla concreta por cada clase hija)**:
   - Rara vez conviene cuando hay consultas polimórficas porque obliga a hacer `UNION ALL` entre todas las tablas.

### ¿Qué elegimos en cada jerarquía de DonaTrack y por qué?

| Jerarquía en el Dominio | Estrategia Elegida | Archivo donde verlo | Justificación para decir en el coloquio |
| :--- | :--- | :--- | :--- |
| **`PersonaDonante`** (`PersonaHumana` / `PersonaJuridica`) | **`JOINED`** | [`PersonaDonante.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/PersonaDonante.java) | Las dos subclases tienen **muchos atributos propios y totalmente distintos** (`nombre`, `apellido`, `dni`, `edad`, `genero` vs `razonSocial`, `tipo`, `rubro`). Si usábamos `SINGLE_TABLE`, la mitad de la tabla iba a quedar llena de `NULL`s y no podíamos poner `NOT NULL` en `razon_social`. Con `JOINED` cada tabla tiene solo sus columnas. |
| **`Bien`** (`BienSimple` / `BienConEstado` / `BienPerecible`) | **`SINGLE_TABLE`** | [`Bien.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Bien.java) | Las subclases agregan **a lo sumo 1 atributo** (`esNuevo` o `fechaVencimiento`) y `CargaDeDonacion` consulta polimórficamente `List<Bien>`. `SINGLE_TABLE` evita hacer `JOIN` y la tabla casi no desperdicia espacio. |
| **`Necesidad`** (`NecesidadExtraordinaria` / `NecesidadRecurrente`) | **`SINGLE_TABLE`** | [`Necesidad.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Necesidad.java) | `EntidadBeneficiaria` recorre `List<Necesidad>` polimórficamente llamando a `estaSatisfecha()`. Como las hijas agregan pocas columnas (2 enteros una, y el `@Embedded PeriodoConsumo` de 3 columnas la otra), `SINGLE_TABLE` resuelve todo en un solo `SELECT` sin `JOIN`. |
| **`MedioDeContacto`** (`Email` / `Telefono` / `WhatsApp`) | **`SINGLE_TABLE`** | [`MedioDeContacto.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/MedioDeContacto.java) | Las 3 subclases **no agregan ningún atributo nuevo** (todas tienen solo `valor`); solo cambian el comportamiento del método `enviarMensaje()` (patrón *Strategy*). Por lo tanto, `SINGLE_TABLE` con columna discriminadora `tipo_medio` no deja ni una sola columna `NULL`! *(Nota: no puede ser `@Embeddable` porque JPA no permite herencia en embebibles).* |

---

## 3. Relaciones, Cascadas y Base de Datos por Microservicio

- **`@OneToMany` unidireccional con `@JoinColumn`**: En vez de dejar que JPA cree una tabla intermedia innecesaria en relaciones `1 a N` (como `PersonaDonante -> MedioDeContacto` o `Ruta -> ParadaDeRuta`), usamos `@JoinColumn` para que la clave foránea (`donante_id`, `ruta_id`) viva directamente en la tabla del "muchos".
- **`cascade = CascadeType.ALL` y `orphanRemoval = true`**: Se usa en composiciones (por ejemplo, cuando guardás una `EntidadBeneficiaria` con sus `Necesidad`es o una `Ruta` con sus `ParadaDeRuta`). Así, al hacer `persist(entidad)`, Hibernate persiste automáticamente sus hijos en la misma transacción.
- **Database-per-Service (Dos bases de datos separadas)**: Como en la Entrega 2 dividimos el sistema en dos microservicios (`donaciones` y `logistica`), en la Entrega 3 cada microservicio tiene **su propia base de datos PostgreSQL** (`donatrack_donaciones` y `donatrack_logistica`). `logistica` no tiene una Foreign Key física hacia la tabla `donacion` de la otra base, sino que guarda el `donacionId: Long` (referencia por ID entre contextos delimitados).
- **Tests con HSQLDB en memoria vs Producción con PostgreSQL**: En [`persistence.xml`](../donaciones/src/main/resources/META-INF/persistence.xml) hay dos unidades de persistencia: `simple-persistence-unit` (HSQLDB en memoria para que los tests corran rápido y aislados) y `postgres-persistence-unit` (PostgreSQL para el sistema corriendo).

---

# PARTE 2: ENTREGA 4 — PRESENTACIÓN MVC, SESIONES, GRAPHQL Y DESPLIEGUE

## 1. Cliente Liviano vs. Cliente Pesado (¿Por qué elegimos MVC con Handlebars?)

### Teoría básica:
- **Cliente Liviano (Server-Side Rendering - SSR)**: El servidor (Java + Javalin + **Handlebars**) busca los datos en la base, los combina con una plantilla HTML (`.hbs`) y le devuelve al navegador **el HTML ya armado y listo para mostrar**. Cuando el usuario envía un formulario (`<form method="POST">`), el servidor procesa la acción y responde con un *Redirect* (`302`) a la pantalla actualizada (patrón **PRG: Post-Redirect-Get**).
- **Cliente Pesado (SPA - Single Page Application, ej. React/Vue/Angular)**: El servidor solo devuelve JSON y el navegador descarga un paquete grande de JavaScript que dibuja la pantalla en la máquina del cliente.

### ¿Qué responder si preguntan por qué elegimos Cliente Liviano con Handlebars?
1. **Simplicidad y reutilización directa del dominio**: En el servidor ya teníamos todos los repositorios, entidades y servicios (`ProcesadorMatchmaking`, `ImportadorDonantesCSV`, `GestorDeNotificaciones`). Con Handlebars en [`WebPortalController.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/web/WebPortalController.java) renderizamos las 17 pantallas directamente desde Java sin duplicar modelos ni mantener dos proyectos separados.
2. **Menor carga en el navegador y mejor carga inicial**: El navegador recibe HTML puro listo para pintar; solo usamos JavaScript liviano en el cliente para los mapas interactivos (**Leaflet.js**) de seguimiento de entregas y camiones.

---

## 2. Manejo de Sesiones y Autenticación (Cookies HTTP vs. JWT)

### Teoría básica:
HTTP es un protocolo **stateless** (sin estado): cada vez que hacés clic en un link, el servidor no se acuerda quién sos a menos que el navegador le mande una credencial en cada petición.
El enunciado daba dos opciones:
1. **Cookies HTTP (la que elegimos)**
2. **JWT (JSON Web Tokens)**

### ¿Cómo funciona nuestra autenticación con Cookies HTTP?
1. Creamos la entidad persistente [`Usuario.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/Usuario.java) que guarda:
   - `email`
   - `passwordHash` (nunca guardamos la contraseña en texto plano; aplicamos hash criptográfico **SHA-256**)
   - `rol` ([`RolUsuario`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/RolUsuario.java): `ADMIN`, `DONANTE` o `ENTIDAD`)
   - Referencia opcional a su `PersonaDonante` o a su `EntidadBeneficiaria`.
2. Cuando el usuario envía el formulario `POST /login`:
   - [`WebPortalController`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/web/WebPortalController.java) busca el `Usuario` por email y verifica el hash de la contraseña.
   - [`SesionManager.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/web/SesionManager.java) genera un token de sesión en el servidor y envía al navegador la cookie `Set-Cookie: donatrack_session=<token>; HttpOnly`.
3. En cada clic siguiente (`GET /donante/donaciones`, `POST /entidad/necesidades`, etc.):
   - El navegador **adjunta sola y automáticamente** la cookie `donatrack_session`.
   - `SesionManager` recupera qué `Usuario` está logueado y verifica que tenga el **rol correspondiente** (si una Entidad intenta entrar a `/admin/deposito`, el servidor le bloquea el paso).
4. **¿Por qué Cookies HTTP es ideal para MVC Server-Side Rendering?** Porque en un cliente liviano donde navegás con links `<a href="...">` y formularios `<form>`, el navegador envía las cookies de forma nativa en cada petición, mientras que con JWT en `localStorage` necesitarías interceptar cada clic con JavaScript para agregar el header `Authorization: Bearer ...`.

---

## 3. Reemplazo de HTTP/REST por GraphQL entre Microservicios (Req. Detallado 3b)

### Teoría básica: ¿Qué es GraphQL y en qué se diferencia de REST?
- En **REST tradicional**: Tenés muchos endpoints fijos (`GET /donaciones`, `POST /notificaciones/inicio-ruta`, `GET /camiones`, `GET /entregas`), y cada endpoint devuelve una estructura JSON fija decidida por el servidor (a veces trae datos de más = *over-fetching*, o tenés que hacer 3 llamadas seguidas para armar una pantalla = *under-fetching*).
- En **GraphQL**:
  - Hay **un único endpoint** (`POST /graphql`) con un **Esquema tipado (SDL)**.
  - Existen dos tipos de operaciones:
    - **`Query` (Lectura)**: El cliente pide **exactamente qué campos** necesita (y puede pedir varios recursos en una sola llamada, por ejemplo `camiones`, `rutas`, `entregas` y `ultimasPosiciones` todo junto para armar el mapa de seguimiento).
    - **`Mutation` (Escritura / Cambio de estado)**: Para crear camiones, informar que una ruta inició o confirmar una entrega con fotos.

### ¿Cómo quedó implementado GraphQL en nuestro proyecto?
Gracias a que teníamos **Arquitectura Hexagonal (Puertos y Adaptadores)**, el dominio ni se enteró del cambio de protocolo:
1. **De `logistica` hacia `donaciones`**:
   - En la Entrega 2 `logistica` tenía la interfaz puerto [`DonacionesAPI.java`](../logistica/src/main/java/ar/edu/utn/frba/dds/logistica/aplicacion/puertos/DonacionesAPI.java).
   - En la Entrega 4 creamos [`DonacionesGraphQLAdapter.java`](../logistica/src/main/java/ar/edu/utn/frba/dds/logistica/aplicacion/adaptadores/DonacionesGraphQLAdapter.java), que implementa esa misma interfaz `DonacionesAPI` pero enviando *queries* (`donacionesPorEstado`) y *mutations* (`notificarInicioRuta`, `notificarEntregaConfirmada`, `notificarEntregaFallida`) al endpoint [`DonacionesGraphQLController.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/controller/DonacionesGraphQLController.java) (`POST /graphql` en `donaciones`).
2. **De `donaciones` hacia `logistica`**:
   - Creamos el puerto [`LogisticaPort.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/puertos/LogisticaPort.java) y su adaptador [`LogisticaGraphQLAdapter.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/adaptadores/LogisticaGraphQLAdapter.java), que se comunica por GraphQL con [`LogisticaGraphQLController.java`](../logistica/src/main/java/ar/edu/utn/frba/dds/logistica/infraestructura/controller/LogisticaGraphQLController.java) (`POST /graphql` en `logistica`).

---

## 4. Extensión del Patrón Observer para Notificaciones Web (Req. 11)

- En entregas anteriores teníamos el patrón **Observer**: [`GestorDeNotificaciones`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/notificaciones/GestorDeNotificaciones.java) publicaba un `EventoDeNotificacion` y [`EnviadorPorMedioPreferido`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/notificaciones/EnviadorPorMedioPreferido.java) mandaba el Email/SMS/WhatsApp.
- En la Entrega 4 nos pidieron que la Entidad Beneficiaria **también vea sus notificaciones adentro de la página web** cuando le asignan una donación o confirma la recepción.
- **¿Cómo lo hicimos sin tocar la lógica existente (Principio Abierto/Cerrado - OCP)?**
  Agregamos un segundo observador: [`RegistradorNotificacionesWeb.java`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/notificaciones/RegistradorNotificacionesWeb.java) que implementa `ObservadorDeNotificaciones` y guarda una [`NotificacionWeb`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/domain/NotificacionWeb.java) en la base de datos. Cuando `GestorDeNotificaciones.publicar(...)` se ejecuta, dispara ambos observadores automáticamente.

---

## 5. Importación Masiva de CSV (>10.000 filas) con creación de Usuarios (Req. 17)

- **Pregunta típica de coloquio**: *"¿Qué pasa si hacés 10.000 `SELECT` y 10.000 transacciones individuales al importar el CSV?"*
- **Respuesta**: Tarda minutos o colapsa la conexión a la base de datos. Por eso en [`ImportadorDonantesCSV.importarMasivoConUsuarios`](../donaciones/src/main/java/ar/edu/utn/frba/dds/donaciones/service/ImportadorDonantesCSV.java) hicimos dos optimizaciones clave:
  1. Precargamos en memoria un `Map<String, PersonaDonante>` y un `Set<String>` con los emails existentes en 2 únicas consultas iniciales ($O(1)$ búsqueda en memoria por cada fila del CSV).
  2. Ejecutamos toda la inserción/actualización de los 10.000+ donantes y sus respectivos `Usuario`s dentro de **una única transacción** (`repo.ejecutarEnTransaccion(...)`), tardando apenas ~2 segundos para 10.050 filas.
