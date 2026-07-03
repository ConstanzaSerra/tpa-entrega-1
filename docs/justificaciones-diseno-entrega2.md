# Justificaciones de Diseño — Entrega 2

Este documento corresponde al entregable **3. Justificaciones de Diseño** de la Entrega 2
(*ENTREGA 2: Arquitectura y Modelado en Objetos — Parte II*). Explica y fundamenta las
decisiones de diseño tomadas por el equipo, con especial atención a la **estrategia de
división en microservicios**, tal como lo pide el enunciado.

Documentos complementarios:

- [`diagrama-clases-entrega2.puml`](diagrama-clases-entrega2.puml) — modelo del dominio de la Entrega 2 (entregable 1).
- [`CONTRATO-DONACIONES.md`](CONTRATO-DONACIONES.md) — contrato HTTP entre los dos servicios.
- [`diagrama-clases-logistica.puml`](diagrama-clases-logistica.puml) — diagrama de clases del servicio de Logística.
- [`presentacion-logistica.md`](presentacion-logistica.md) — recorrido guiado por el diseño de Logística.
- [`recorrido-codigo-entrega2.md`](recorrido-codigo-entrega2.md) — recorrido flujo por flujo con el código real y comandos de demo.

---

## 1. Estrategia de división en microservicios

### 1.1 El disparador: la organización del equipo

El enunciado plantea la división no como un fin técnico sino como una **decisión
organizacional**: *"Debido a que se prevee que el tamaño del equipo de desarrollo crecerá
pronto, se desea organizarlo en dos equipos. Por ello, se solicita que se divida al sistema
en, inicialmente, dos microservicios"*. Esto es una aplicación directa de la **Ley de
Conway**: la estructura del sistema termina reflejando la estructura de comunicación de la
organización que lo construye. Si van a existir dos equipos, conviene que cada uno sea
dueño de una pieza del sistema con **frontera explícita**, para que puedan desarrollar,
probar y desplegar sin bloquearse mutuamente.

### 1.2 El criterio: alta cohesión, bajo acoplamiento

El propio enunciado sugiere el criterio de partición: *"buscar que las particiones del
dominio se definan maximizando la cohesión interna y minimizando el acoplamiento entre
servicios"*. Aplicando ese criterio sobre el alcance de la entrega, el dominio se parte
naturalmente en dos **contextos** con vocabularios y ciclos de vida distintos:

| | **Donaciones** | **Logística** |
|---|---|---|
| Habla de… | donantes, bienes, necesidades, entidades beneficiarias, matchmaking | camiones, rutas, paradas, entregas, posiciones GPS |
| Responde a… | ¿quién donó qué? ¿a quién le corresponde? | ¿cómo y cuándo llega físicamente al destino? |
| Sus reglas cambian cuando… | cambian los algoritmos de asignación o los tipos de necesidad | cambia la flota, el planificador externo o el monitoreo |
| Integra con… | medios de notificación (email/SMS/WhatsApp) | planificador de rutas externo, app móvil del conductor |

La cohesión interna de cada servicio es alta: todo lo que está dentro de Logística
(`Camion`, `Ruta`, `ParadaDeRuta`, `Entrega`, `PosicionCamion`) colabora para un único
propósito — planificar y trazar entregas — y todo lo que está en Donaciones colabora para
captar, segmentar y asignar donaciones.

El acoplamiento entre servicios es bajo y **medible en el contrato**: como se ve en
`CONTRATO-DONACIONES.md`, entre los servicios solo viajan **IDs y datos primitivos**
(`donacionId`, `entidadBeneficiariaId`, `direccionDestino`), nunca objetos de dominio
completos. Logística no conoce qué es un `BienPercibible` ni una `NecesidadRecurrente`;
Donaciones no conoce qué es una `ParadaDeRuta`. Cada servicio es **dueño de sus datos**
y el otro solo puede consultarlos vía API.

### 1.3 La costura elegida: el estado `ASIGNACION_REALIZADA`

La frontera entre ambos servicios coincide con un punto preciso de la máquina de estados
de la donación (requerimiento de trazabilidad de la Entrega 1, extendido en esta entrega):

```
EN_DEPOSITO → ASIGNACION_REALIZADA → LISTA_PARA_ENTREGAR → EN_TRASLADO → ENTREGADA
└────────── Donaciones ──────────┘   └───────────── Logística ─────────────┘
```

- Todo lo que ocurre **hasta** que la donación tiene destino asignado (carga, segmentación,
  matchmaking, confirmación del administrador) es responsabilidad de **Donaciones**.
- Todo lo que ocurre **después** (planificación de rutas, inicio de ruta, traslado,
  confirmación o rechazo de la recepción) es responsabilidad de **Logística**.

Esta costura es la de menor fricción porque el traspaso entre ambos mundos es un único
evento de negocio ("la donación ya sabe a dónde va, hay que llevarla"), y el enunciado
mismo la insinúa: el planificador externo *"recibirá, por ejecución, un conjunto de
donaciones en estado Asignación Realizada"*.

### 1.4 Alternativas consideradas y descartadas

- **Notificaciones como tercer microservicio.** Se descartó porque el enunciado pide
  *inicialmente dos* servicios, y porque Notificaciones no tiene entidades propias ni
  reglas de negocio: es una capacidad transversal (Strategy `MedioDeContacto` de la
  Entrega 1). Separarla agregaría un salto de red a cada evento sin ganar autonomía de
  equipo. Queda como candidata natural si el sistema sigue creciendo.
- **Partir por entidades (Donantes / Beneficiarios).** Se descartó porque donantes y
  beneficiarios participan de los mismos casos de uso (una asignación involucra a ambos);
  partirlos generaría un acoplamiento altísimo — cada operación cruzaría la frontera.
- **Partir por capas técnicas (API / dominio / datos).** Contradice la idea misma de
  microservicios: cada cambio funcional tocaría todos los servicios y los dos equipos
  quedarían acoplados en cada feature.

La división elegida (por **subdominio de negocio**) es la única de las analizadas donde
un requerimiento nuevo típico cae completo dentro de un solo equipo.

### 1.5 Correspondencia con la agrupación de endpoints del enunciado

El enunciado agrupa la exposición REST en tres bloques: dos de *Donaciones* (gestión de
donantes/donaciones y gestión de beneficiarios/necesidades) y uno de *Logística* (flota,
rutas y entregas). Nuestra partición respeta exactamente esa agrupación: los dos primeros
bloques son endpoints del servicio de **Donaciones** y el tercero del servicio de
**Logística**, lo que confirma que la costura elegida coincide con la vista funcional
que la cátedra propone.

---

## 2. Integración entre los servicios

### 2.1 REST sobre HTTP, siguiendo las convenciones del material

El enunciado exige que la integración use *"el protocolo HTTP y siguiendo las convenciones
REST"* y referencia el tutorial de HTTP provisto (sección *14. Recursos*). Aplicamos esas
convenciones tal como las define el material:

- **Rutas orientadas a recursos, no a acciones**: `GET /rutas/{id}`, `GET /entregas`,
  `GET /camiones/{id}`, `GET /donaciones?estado=ASIGNACION_REALIZADA` (filtrado por
  *query param*, como en la sección *3. Parámetros* del tutorial).
- **Semántica de verbos**: `GET` sin efectos, `POST` para crear/registrar eventos.
- **Códigos de estado con significado**: `200`/`202` para aceptación, `400` ante datos
  inválidos (validación de latitud/longitud/velocidad en `GpsController`), `404` cuando
  el recurso no existe. Una colección vacía devuelve `[]` con `200`, no `404`.

### 2.2 Estilos de comunicación: sincrónico para consultar, asincrónico para avisar

Distinguimos dos necesidades distintas y les dimos dos estilos distintos:

- **Consulta sincrónica (request/response):** cuando Logística necesita las donaciones a
  planificar hace `GET /donaciones?estado=ASIGNACION_REALIZADA` y espera la respuesta,
  porque sin esos datos no puede continuar.
- **Notificación asincrónica (fire-and-forget):** cuando cambia el estado de una entrega,
  Logística hace un `POST /notificaciones/...` a Donaciones **sin esperar ni reintentar**.
  Esto responde al objetivo de la entrega de *"incorporar flujos de trabajo asincrónicos"*
  y a una decisión de resiliencia: si Donaciones está caído, Logística **igual arranca y
  opera** (las notificaciones fallan en silencio y se loguean). Un camión debe poder
  repartir aunque el otro servicio esté en mantenimiento.

### 2.3 El contrato como documento compartido, no como código compartido

Lo único que ambos equipos comparten es el **contrato HTTP** (`CONTRATO-DONACIONES.md`):
nombres exactos de campos JSON, verbos y rutas. Deliberadamente **no** se comparte ninguna
clase Java entre servicios (la interfaz `DonacionesAPI` es interna de Logística). Esto
evita el acoplamiento por librería compartida, que obligaría a ambos equipos a coordinar
cada versión — justo lo que la división en microservicios quería evitar.

Las URLs de los otros servicios no están hardcodeadas: se configuran por
`application.properties` / variables de entorno, de modo que el despliegue (local, demo,
distinto host) no requiere recompilar.

---

## 3. Arquitectura interna de Logística: puertos y adaptadores

Dentro del servicio de Logística se aplicó una arquitectura **hexagonal** (puertos y
adaptadores), justificada por la cantidad de integraciones externas que el enunciado
impone a este servicio (planificador externo, servicio de Donaciones, app móvil del
conductor):

- **Dominio puro en el centro**: `Camion`, `Ruta`, `ParadaDeRuta`, `Entrega`,
  `PosicionCamion` y sus máquinas de estado no conocen HTTP ni JSON.
- **Puertos de salida (interfaces)**: `DonacionesAPI` y `PlanificadorExternoAPI` declaran
  *qué* necesita el dominio de los sistemas externos, sin decir *cómo*.
- **Adaptadores**: `DonacionesHttpAdapter` y `PlanificadorHttpAdapter` implementan esos
  puertos con HTTP concreto. Si mañana el planificador cambia de proveedor, solo se
  reescribe el adaptador.
- **Puertos de entrada**: los controllers REST (`RutaController`, `EntregaController`,
  `CamionController`, `GpsController`, `PlanificadorController`) traducen HTTP a llamadas
  al dominio, y los **DTOs** aíslan la forma del JSON del modelo interno.

El beneficio principal es la **testeabilidad** (objetivo de la Unidad 8 — Validación del
Diseño): los tests de `PlanificadorRutasService` mockean los puertos sin levantar ningún
servidor, y los tests de controllers validan el contrato REST de forma aislada.

**Persistencia:** los repositorios (`RutaRepository`, `EntregaRepository`, etc.) son
interfaces con implementación en memoria (`InMemory*`). La cátedra no prescribe tecnología
de persistencia para esta entrega, así que se optó por la implementación más simple que
cumple los requerimientos, detrás de una interfaz que permite cambiarla sin tocar dominio
ni controllers.

---

## 4. Integración con el planificador externo de rutas

El enunciado impone tres restricciones de implementación que moldearon el diseño:

1. **URL de callback** (`POST /planificador/callback`): la planificación es un proceso
   largo del lado del proveedor, por eso el flujo es asincrónico — Logística envía la
   solicitud, el proveedor responde después por el callback, y recién ahí se registran
   las rutas y se actualizan las entregas asignadas (`PlanificadorController`).
2. **Lotes de hasta 100 donaciones**: `PlanificadorRutasService` particiona el conjunto
   de donaciones en lotes de a 100 antes de enviarlos, porque *"cada ejecución procesa
   como máximo 100 donaciones a entregar"*.
3. **Donaciones sin asignar**: el callback puede devolver donaciones no asignadas en un
   campo aparte; el servicio las retiene para volver a incluirlas en la siguiente
   planificación, cumpliendo con que *"será responsabilidad de nuestro sistema volver a
   planificarlas"*.

**Ejecución calendarizada:** el requerimiento pide generar los planes *"en horarios de
baja carga"*. `PlanificacionScheduler` dispara la planificación de la jornada siguiente
en un horario configurable, cumpliendo el objetivo de la entrega de *"incorporar nociones
de ejecuciones de tareas asincrónicas y/o calendarizadas"* sin acoplar esa lógica al
servicio (el scheduler solo invoca a `PlanificadorRutasService`, que también puede
ejecutarse a demanda).

---

## 5. Monitoreo de camiones en tiempo real: alternativa elegida

El enunciado ofrece dos alternativas y exige elegir una y *"definir el contrato de
integración correspondiente"*. Se eligió la **alternativa 2: aplicación móvil utilizada
por el conductor**, que reporta la geolocalización mientras la ruta está activa.

Justificación:

- **Sin hardware adicional**: el chofer ya interactúa con el sistema desde su aplicación
  (inicia la ruta desde ella, según el flujo de trazabilidad); reutilizar ese mismo canal
  evita instalar y mantener dispositivos GPS en cada camión de la flota.
- **El reporte acompaña el ciclo de vida de la ruta**: la app reporta *"mientras la ruta
  esté activa"*, que es exactamente la ventana en la que el negocio necesita monitoreo.
  Un GPS fijo reportaría siempre, generando datos irrelevantes fuera de servicio.
- **La responsabilidad queda bien repartida**: la configuración del dispositivo/app es
  del equipo externo (como dice el enunciado); nuestra plataforma solo debe *"recibir,
  validar y procesar"*. Eso es lo que hace `GpsController`: expone
  `POST /camiones/{id}/posicion`, **valida** rangos de latitud/longitud, velocidad no
  negativa y presencia de timestamp (rechaza con `400`), verifica que el camión exista
  (`404` si no) y persiste la `PosicionCamion`.
- **Dashboard**: `GET /dashboard/camiones` devuelve la última posición conocida de cada
  camión, que es lo que consume el dashboard administrativo requerido.

---

## 6. Trazabilidad de las entregas

El requerimiento 1 exige *"garantizar la trazabilidad de los estados de las donaciones,
desde su recepción hasta su entrega"*. La Entrega 2 extiende la máquina de estados de la
Entrega 1 con el ciclo logístico, modelado en `EstadoEntrega` y `EstadoRuta`:

- El chofer informa el inicio de ruta → las entregas de esa ruta pasan **automáticamente**
  a `EN_TRASLADO` (requerimiento 6; la transición la dispara el cambio de estado de la
  `Ruta`, no un update manual entrega por entrega).
- La entidad beneficiaria confirma la recepción → la entrega pasa a `ENTREGADA` y **queda
  registrado qué camión la realizó** (dato que además viaja en la notificación de entrega
  exitosa, que debe incluir *"fecha, hora y camión responsable"*).
- Si la entidad informa que no la recibió → `NO_RECIBIDA`, caso a revisar por
  administradores; si la donación vuelve al depósito, la entrega vuelve a `PENDIENTE` y
  puede replanificarse — tal cual el flujo descripto en el enunciado.

Cada transición dispara la notificación correspondiente hacia Donaciones (sección 2.2),
que es quien conoce a los donantes y beneficiarios y decide por qué medio contactarlos
(reutilizando el Strategy `MedioDeContacto` de la Entrega 1). Esta separación también es
consecuencia de la partición: **Logística sabe qué pasó; Donaciones sabe a quién avisarle.**

---

## 7. Resumen de decisiones

| Decisión | Justificación (fuente: enunciado E2) |
|---|---|
| 2 microservicios: Donaciones y Logística | División por subdominio, máx. cohesión / mín. acoplamiento (sugerencia del enunciado); un servicio por equipo (Conway) |
| Frontera en `ASIGNACION_REALIZADA` | Es el evento de negocio donde termina "decidir destino" y empieza "entregar"; el planificador consume donaciones en ese estado |
| Integración REST/HTTP con contrato de solo IDs | Exigencia de exposición REST; datos propios de cada servicio, sin clases compartidas |
| Notificaciones entre servicios fire-and-forget | Objetivo de flujos asincrónicos; resiliencia ante caída del otro servicio |
| Hexagonal (puertos/adaptadores) en Logística | Aísla 3 integraciones externas; testeable con mocks (Unidad 8) |
| Callback + lotes de 100 + replanificación | Requerimientos de implementación 1, 2 y 3 |
| Scheduler en horario de baja carga | Requerimientos de dominio 3 y 5 |
| App móvil del conductor para geolocalización | Sin hardware extra; reporta solo con ruta activa; la plataforma valida y procesa (responsabilidad que fija el enunciado) |
| Repositorios en memoria detrás de interfaces | La cátedra no prescribe persistencia; se mantiene reemplazable |
