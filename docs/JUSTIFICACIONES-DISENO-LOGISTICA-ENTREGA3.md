# Justificaciones de Diseño — Servicio **Logística** (Entrega 3: Persistencia)

> **DonaTrack** — Microservicio de Logística (flota de camiones, planificación de rutas, entregas y monitoreo GPS).
> Alcance de este documento: **solo Logística**. El esquema de Donaciones se documenta por separado.
> DER físico complementario: [`der-logistica.puml`](./der-logistica.puml).

---

## 1. Estrategia de mapeo objeto–relacional

Se eligió **anotar directamente las clases de dominio** con JPA (la "Opción A" del plan de la entrega), en lugar de mantener entidades de persistencia separadas con *mappers*.

**Por qué:**
- Es el estilo que enseña y espera la cátedra; `jpa-extras` está pensado para esto.
- Mucho menos código y menos superficie de bugs que duplicar cada clase en una "entidad de datos".
- La lógica de negocio (máquinas de estado de `Ruta` y `Entrega`, validaciones de `Camion`) **quedó intacta**: las anotaciones son declarativas y no tocan los métodos.

**Consecuencias asumidas:**
- A cada `@Entity` se le agregó un **constructor sin argumentos** requerido por JPA (`public Ruta()`, `public Entrega()`, `public Camion() {}`, etc.), conviviendo con los constructores de negocio.
- Las colecciones se inicializan mutables (`new ArrayList<>()`) en el constructor para que Hibernate pueda poblarlas.
- Se acepta un acoplamiento del dominio a `javax.persistence`, considerado razonable para el TP.

---

## 2. Entidades del esquema y su mapeo

| Entidad | Rol | Puntos de mapeo relevantes |
|---|---|---|
| `Camion` | Unidad de flota | `@Id IDENTITY`; atributos simples (`patente`, `volumenM3`, `alturaM`, `capacidadKg`). Validaciones en el constructor de negocio. |
| `Ruta` | Plan de reparto | `@ManyToOne Camion`; `@OneToMany(cascade = ALL) List<ParadaDeRuta>`; `estado` enum STRING; `linkMapa`. |
| `ParadaDeRuta` | Parada de una ruta | `@OneToMany(cascade = ALL) List<Entrega>`; `entidadBeneficiariaId` (id opaco), `direccion`. |
| `Entrega` | Entrega puntual a una entidad | `estado` enum STRING; `@ManyToOne Camion camionQueEntrego` (nullable); `@ElementCollection List<String> fotosUrl`; `donacionId` y `entidadBeneficiariaId` (ids opacos). |
| `PosicionCamion` | Telemetría GPS | `@Id IDENTITY`; `camionId` como **Long plano** (sin relación); lat/lon, `velocidad` (nullable), `timestamp`. |

---

## 3. Decisiones relacionales y sus justificaciones

### 3.1. IDs autogenerados — `@GeneratedValue(strategy = IDENTITY)`
Todas las entidades usan una clave subrogada `Long id` autoincremental. Reemplaza el viejo `setId(proximoId++)` en memoria. Se eligió `IDENTITY` (autoincremento delegado a la base) por ser el estándar de la cátedra y el más simple de leer en el DER.

### 3.2. Enums persistidos como texto — `@Enumerated(EnumType.STRING)`
`EstadoRuta` (`PLANIFICADA`, `EN_CURSO`, `COMPLETADA`) y `EstadoEntrega` (`PENDIENTE`, `EN_TRASLADO`, `ENTREGADA`, `NO_RECIBIDA`) se guardan como **string**, nunca como ordinal.

**Por qué STRING y no ORDINAL:**
- **Robustez:** si mañana se agrega o reordena un valor del enum, los datos ya guardados no se corrompen. Con ordinal, insertar un estado en el medio reasignaría el significado numérico de las filas existentes.
- **Legibilidad:** la columna `estado = 'EN_TRASLADO'` se entiende directo en la base, sin decodificar un número.

### 3.3. **Frontera entre microservicios: sin claves foráneas cruzadas** *(decisión central)*
`Entrega` guarda `donacionId` y `entidadBeneficiariaId`, y `ParadaDeRuta` guarda `entidadBeneficiariaId`. **Esas entidades no viven en Logística: son de Donaciones.** Se persisten como `Long` **opacos**, sin `@ManyToOne` ni FK.

**Por qué:**
- Cada servicio tiene **su propio esquema** y no comparte entidades (requisito de la entrega). Una FK a una tabla de otro esquema/otra base rompería la independencia de los servicios.
- La integridad referencial *cruzada* es responsabilidad de la **capa de aplicación / HTTP** (los adaptadores que hablan con Donaciones), no del motor de base.
- Es una **desnormalización deliberada**: se acepta guardar un id "suelto" a cambio de desacoplar los despliegues. Los dos servicios pueden migrar su esquema sin coordinar.

### 3.4. Composición `Ruta → ParadaDeRuta → Entrega` con `CascadeType.ALL`
La ruta es dueña de sus paradas y cada parada de sus entregas: forman un agregado con ciclo de vida dependiente. Por eso ambos `@OneToMany` usan `cascade = ALL` — al persistir/eliminar una `Ruta` se propaga a sus paradas y entregas, sin tener que guardarlas una por una.

> **Consideración de modelado (mejora futura):** ambos `@OneToMany` son **unidireccionales y sin `@JoinColumn`**. Por defecto, JPA/Hibernate materializa esa relación con una **tabla intermedia de join** (`Ruta_ParadaDeRuta`, `ParadaDeRuta_Entrega`) en lugar de una FK en la tabla hija. Funciona correctamente, pero si se quisiera un DER más plano se podría agregar `@JoinColumn` para forzar una FK directa (`ruta_id` en `ParadaDeRuta`, `parada_id` en `Entrega`). Se dejó la forma por defecto por simplicidad. Ver estas tablas en el DER.

### 3.5. Fotos de la entrega — `@ElementCollection`
`Entrega.fotosUrl` es una `List<String>` (URLs de las fotos del comprobante). Al ser una **colección de valores simples** (no entidades con identidad propia), se mapeó con `@ElementCollection`, que genera una tabla secundaria (`Entrega_fotosUrl`) con la FK a la entrega. No ameritaba una entidad `Foto` completa.

### 3.6. `PosicionCamion` como entidad separada con `camionId` plano
El monitoreo GPS es un **flujo de telemetría**: muchas posiciones por camión a lo largo del tiempo. Se modeló como `@Entity` propia (una fila por reporte) y **no** como `@Embeddable` dentro de `Camion` (que solo permitiría una posición).

Además, `camionId` es un `Long` plano y **no** un `@ManyToOne Camion`. Aunque `Camion` está en el mismo esquema (una FK acá *sería* posible), se optó por la referencia por id para mantener la telemetría como un registro **append-only** desacoplado: se insertan posiciones sin cargar ni bloquear el agregado `Camion`.

### 3.7. `camionQueEntrego` distinto del camión de la ruta
`Entrega` tiene su propio `@ManyToOne Camion camionQueEntrego` (nullable), separado del `Camion` asignado a la `Ruta`. Registra **qué camión efectivamente concretó la entrega** en el momento de confirmarla, dato que puede diferir de la asignación planificada y que solo tiene sentido una vez entregada (por eso es nullable, junto con `fechaHoraEntrega`).

### 3.8. `linkMapa` como dato desnormalizado
`Ruta.linkMapa` es un `String` con el link al mapa de la ruta, provisto por el planificador externo. Es un **dato calculado/externo cacheado** en la fila de la ruta: se guarda tal cual llega en vez de recomputarlo, porque el ruteo está delegado a un servicio externo. Esto también sustenta por qué **no se usa PostGIS**: solo se almacenan coordenadas (lat/lon) y un link, no geometría consultable.

---

## 4. Infraestructura de persistencia

### 4.1. Dos unidades de persistencia (`persistence.xml`)
| Unidad | Motor | `hbm2ddl.auto` | Uso |
|---|---|---|---|
| `simple-persistence-unit` | HSQLDB en memoria (`jdbc:hsqldb:mem:logistica`, modo sintaxis PG) | `create-drop` | Tests: la base se crea y se borra en cada corrida. |
| `postgres-persistence-unit` | PostgreSQL (`donatrack_logistica`) | `update` | Despliegue local (docker-compose): mantiene los datos entre arranques. |

JPA abstrae el dialecto, así que cambiar de motor no toca el código de dominio ni los repositorios.

### 4.2. Selección de la unidad en runtime — `EntityManagerProvider`
El orden de resolución es deliberado:
1. **system property** `persistence.unit` (la fija Surefire → todos los tests van a HSQLDB sin acordarse en cada uno);
2. **variable de entorno** `PERSISTENCE_UNIT` (levantar la app contra otra base sin recompilar);
3. **default** `postgres-persistence-unit` (despliegue local).

### 4.3. Un `EntityManager` por hilo (`ThreadLocal`)
`EntityManagerProvider` mantiene **un `EntityManager` por hilo**. Es una decisión con motivo concreto: si cada repositorio se creara su propio contexto de persistencia, una entidad guardada por un repo quedaría *detached* para los demás (guardás la entrega con un repo y, al guardar la ruta con otro, Hibernate la ve como transitoria). Compartir el contexto por hilo evita ese problema. Como el `EntityManager` **no es thread-safe**, es uno por hilo y no uno global.

### 4.4. Transacciones `RESOURCE_LOCAL` explícitas
Los `Jpa*Repository` implementan las **interfaces de repositorio ya existentes** (`EntregaRepository`, `RutaRepository`, `CamionRepository`, `GpsRepository`), de modo que el resto de la app no se entera del cambio de in-memory a JPA. Cada escritura corre dentro de un helper `enTransaccion(...)` que hace `begin/commit` y `rollback` ante excepción; si ya hay una transacción activa, se cuelga de ella (evita transacciones anidadas).

---

## 5. DER físico del esquema `logistica`

Diagrama renderizable: [`der-logistica.puml`](./der-logistica.puml).

**Tablas de entidad:** `Camion`, `Ruta`, `ParadaDeRuta`, `Entrega`, `PosicionCamion`.
**Tablas de colección/join generadas por JPA:** `Entrega_fotosUrl` (`@ElementCollection`), `Ruta_ParadaDeRuta` y `ParadaDeRuta_Entrega` (join de los `@OneToMany` unidireccionales).

**Relaciones con FK real (mismo esquema):**
- `Ruta.camion_id` → `Camion.id` (N:1, opcional).
- `Entrega.camionQueEntrego_id` → `Camion.id` (N:1, opcional).

**Referencias sin FK (por diseño):**
- `Entrega.donacionId`, `Entrega.entidadBeneficiariaId`, `ParadaDeRuta.entidadBeneficiariaId` → ids opacos de **Donaciones** (otro servicio).
- `PosicionCamion.camionId` → id de `Camion` guardado como valor plano (telemetría append-only).

---

## 6. Consideraciones abiertas / mejoras pendientes (honestidad para el coloquio)

- **`@OneToMany` con tabla de join:** como se explicó en 3.4, la forma por defecto genera tablas intermedias. Es correcta pero podría aplanarse con `@JoinColumn` si se busca un DER con FK directas.
- **Faltan tests de integración de persistencia en Logística:** la suite actual valida controllers/servicios (in-memory y mocks). Convendría sumar 1–2 tests que ejerciten un `Jpa*Repository` real contra HSQLDB (guardar → leer → verificar), como pide la *Definition of Done* de la entrega.
- **Cableado hardcodeado a JPA:** `LogisticaApp` instancia directamente los `Jpa*Repository`. El plan proponía un switch `PERSISTENCIA=memoria|jpa` con fallback in-memory; hoy ese fallback existe en las clases `InMemory*` pero no está cableado en el arranque.
- **Esquema no fijado explícitamente:** las unidades no declaran un `default_schema`; las tablas se crean en el esquema por defecto de la conexión. Si se quisiera aislar por nombre de esquema (`logistica`), se agregaría esa propiedad.
