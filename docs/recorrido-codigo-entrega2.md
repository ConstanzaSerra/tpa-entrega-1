# Recorrido por el código — Logística (Entrega 2)

Guion para la defensa: cada flujo muestra **qué endpoint se llama, qué clase atiende,
qué hace el dominio y qué sale hacia afuera**, con el código real del proyecto.
Cada sección remata con el argumento de diseño que la conecta con
[`justificaciones-diseno-entrega2.md`](justificaciones-diseno-entrega2.md).

Convención de rutas de archivo: todo está bajo
`src/main/java/ar/edu/utn/frba/dds/logistica/`.

---

## 0. El arranque: `LogisticaApp` (cómo se conecta todo)

Antes de mostrar flujos, conviene abrir `LogisticaApp.java` porque el `main` **es** la
arquitectura hexagonal contada en 6 pasos numerados: repositorios en memoria, adaptadores
HTTP construidos desde config, servicio de planificación, scheduler, controllers, y
recién al final Javalin con las rutas.

```java
// LogisticaApp.java:29-33 — las URLs NO están hardcodeadas
String donacionesUrl = ConfigManager.getProperty("api.donaciones.url", "http://localhost:8081");
String planificadorUrl = ConfigManager.getProperty("api.planificador.url", "http://localhost:8082");

DonacionesAPI donacionesAPI = new DonacionesHttpAdapter(donacionesUrl);
PlanificadorExternoAPI planificadorAPI = new PlanificadorHttpAdapter(planificadorUrl);
```

Punto para remarcar: los controllers y servicios reciben **interfaces**
(`DonacionesAPI`, `CamionRepository`), nunca las implementaciones concretas. Toda la
tecnología (HTTP, memoria) se decide en este único lugar.

> **Justificación asociada:** sección 3 (hexagonal: dominio en el centro, puertos y
> adaptadores en el borde; testeable mockeando los puertos).

---

## 1. Alta de un camión — `POST /camiones`

**Demo:**

```bash
curl -X POST http://localhost:8080/camiones \
  -H "Content-Type: application/json" \
  -d '{ "patente": "AB123CD", "volumenM3": 30, "alturaM": 2.6, "capacidadKg": 5000 }'
```

**Qué pasa por dentro** (`controller/CamionController.java:37-42`):

```java
public void create(Context ctx) {
    CamionDTO dto = ctx.bodyAsClass(CamionDTO.class);   // 1. JSON → DTO
    Camion nuevoCamion = dto.toDomain();                // 2. DTO → objeto de dominio
    Camion guardado = camionRepository.guardar(nuevoCamion); // 3. persiste (asigna id)
    ctx.status(HttpStatus.CREATED).json(CamionDTO.fromDomain(guardado)); // 4. 201 + recurso
}
```

Los cuatro pasos son el patrón de todos los endpoints: el JSON nunca toca el dominio
directamente (siempre media un DTO), el controller no tiene lógica de negocio, y el
código de estado sigue la convención REST (`201 Created`, y el recurso creado en el
cuerpo, como en el tutorial de la cátedra).

El `Camion` (`domain/Camion.java`) tiene exactamente los atributos que pide el
enunciado: *patente, capacidad en volumen (m³), altura (m) y capacidad de carga (kg)*.

El resto del CRUD está en el mismo controller: `getAll`/`getById` (líneas 18-35, con
`404` si no existe), `update` (44-58) y `delete` (60-70, responde `204 No Content`).

**¿Y qué pasa después de agregarlo?** Nada inmediato — pero el camión ya queda
disponible para la **próxima planificación**: `PlanificadorRutasService.planificar()`
hace `camionRepository.buscarTodos()` (línea 39) y manda toda la flota al planificador
externo. Ese es el hilo conductor para pasar al flujo 2.

> **Justificación asociada:** sección 2.1 (convenciones REST) y el bloque "Logística:
> gestión de flota de camiones" de la exposición REST del enunciado.

---

## 2. Planificación de rutas — scheduler → planificador externo → callback

Este es el flujo más rico porque cubre **tres requerimientos de implementación** del
enunciado a la vez. No lo dispara ningún usuario: lo dispara el reloj.

### 2.a El scheduler (ejecución calendarizada)

```java
// LogisticaApp.java:42-44
PlanificacionScheduler scheduler = new PlanificacionScheduler(planificadorService);
long periodMinutes = Long.parseLong(ConfigManager.getProperty("scheduler.period.minutes", "1"));
scheduler.iniciar(periodMinutes, periodMinutes, TimeUnit.MINUTES);
```

El período viene de config: en la demo corre cada 1 minuto para poder mostrarlo en vivo,
en producción se configuraría para el horario de baja carga que pide el requerimiento 5.

### 2.b El servicio: pedir donaciones y mandar lotes de ≤ 100

`service/PlanificadorRutasService.java:30-66`, el corazón del flujo:

```java
public void planificar() {
    // 1. Le pide a DONACIONES (otro microservicio) las donaciones con destino asignado
    List<DonacionParaRutaDTO> donacionesListas = donacionesAPI.obtenerDonacionesListasParaRepartir();
    ...
    // 2. Junta la flota local
    List<Camion> camiones = camionRepository.buscarTodos();
    ...
    // 3. Restricción del proveedor: lotes de máximo 100 donaciones
    int lotes = (int) Math.ceil((double) donacionesListas.size() / 100);
    for (int i = 0; i < lotes; i++) {
        ...
        PlanificacionRequestDTO request = new PlanificacionRequestDTO(
                callbackUrl,          // ← acá viaja la URL de callback
                loteDonaciones,
                camionesDTO);
        planificadorExternoAPI.solicitarPlanificacion(request);
    }
}
```

Detrás del puerto `donacionesAPI`, el adaptador hace literalmente el `GET` del contrato
(`adaptadores/DonacionesHttpAdapter.java:35`):

```java
.uri(URI.create(baseUrl + "/donaciones?estado=ASIGNACION_REALIZADA"))
```

Y si Donaciones está caído, devuelve lista vacía en vez de romper (líneas 47-50) —
Logística sigue viva.

### 2.c El callback: el planificador nos responde después

El planificador externo no responde las rutas en el momento: cuando termina, hace `POST`
a la URL de callback que le pasamos. Eso lo atiende
`controller/PlanificadorController.java:29-63`:

```java
public void recibirCallback(Context ctx) {
    PlanificacionCallbackDTO callbackData = ctx.bodyAsClass(PlanificacionCallbackDTO.class);
    ...
    if (callbackData.donacionesNoAsignadas != null && !callbackData.donacionesNoAsignadas.isEmpty()) {
        // req. impl. 3: quedan registradas para volver a planificarse
        System.out.println("Atención: Quedaron " + ... + " donaciones sin asignar.");
    }
    for (PlanificacionCallbackDTO.RutaArmadaDTO rutaDTO : callbackData.rutas) {
        Ruta nuevaRuta = new Ruta(camionOpt.get());          // Ruta nace PLANIFICADA
        for (... paradaDTO : rutaDTO.paradas) {
            ParadaDeRuta parada = new ParadaDeRuta(paradaDTO.entidadBeneficiariaId, paradaDTO.direccion);
            for (Long donacionId : paradaDTO.donacionesIds) {
                Entrega entrega = new Entrega(donacionId, paradaDTO.entidadBeneficiariaId); // nace PENDIENTE
                ...
            }
        }
        rutaRepository.guardar(nuevaRuta);
    }
}
```

Fijate que la `Entrega` guarda `donacionId` (un `Long`), no una `Donacion`: **ningún
objeto de dominio cruza la frontera entre microservicios**, solo IDs.

**Demo del callback** (simulando al planificador con curl):

```bash
curl -X POST http://localhost:8080/planificador/callback \
  -H "Content-Type: application/json" \
  -d '{ "rutas": [ { "camionId": 1, "paradas": [
        { "entidadBeneficiariaId": 5, "direccion": "Av. Siempreviva 742", "donacionesIds": [1, 2] }
      ] } ], "donacionesNoAsignadas": [] }'

curl http://localhost:8080/rutas    # la ruta quedó PLANIFICADA con sus entregas PENDIENTE
```

> **Justificación asociada:** sección 4 completa (callback = req. impl. 1, lotes = req.
> impl. 2, no asignadas = req. impl. 3, scheduler = reqs. dominio 3 y 5) y sección 1.2
> (solo IDs cruzan la frontera).

---

## 3. El chofer inicia su ruta — `POST /rutas/{id}/iniciar`

**Demo:**

```bash
curl -X POST http://localhost:8080/rutas/1/iniciar
```

**Qué pasa por dentro** (`controller/RutaController.java:26-69`) — acá se ven las tres
capas trabajando juntas:

```java
ruta.iniciar();                              // 1. máquina de estados de Ruta

for (ParadaDeRuta parada : ruta.getParadas()) {
    for (Entrega entrega : parada.getEntregas()) {
        entrega.iniciarTraslado();           // 2. TODAS las entregas → EN_TRASLADO
        entregasAfectadas.add(new EntregaAfectadaDTO(...));
    }
}

EventoInicioRutaDTO evento = new EventoInicioRutaDTO(...);
donacionesAPI.notificarInicioRuta(evento);   // 3. aviso a Donaciones (fire-and-forget)
```

1. **La transición la valida el dominio, no el controller.** `Ruta.iniciar()`
   (`domain/Ruta.java:23-28`) tira `IllegalStateException` si la ruta no está
   `PLANIFICADA`; el controller solo la traduce a `400 Bad Request` (línea 66-68).
   Probalo en vivo: ejecutá el mismo curl dos veces — la segunda da
   `400: "Solo se puede iniciar una ruta PLANIFICADA. Estado: EN_CURSO"`.
2. **El pase a `EN_TRASLADO` es automático**, tal cual pide el enunciado
   (*"Automáticamente, las entregas asignadas pasarán al estado 'En traslado'"*): el
   chofer hace una sola acción y el sistema propaga.
3. **La notificación es asincrónica.** En
   `adaptadores/DonacionesHttpAdapter.java:68-90`, `enviarNotificacionAsync` usa
   `httpClient.sendAsync(...)`: no espera respuesta, no reintenta, y si falla solo
   loguea. El chofer no se queda esperando a que Donaciones conteste para arrancar
   el camión.

> **Justificación asociada:** sección 2.2 (sincrónico para consultar, asincrónico para
> avisar; resiliencia) y sección 6 (transición automática a EN_TRASLADO). Donaciones
> recibe este evento y es quien decide a qué donantes/entidades notificar y por qué
> medio — "Logística sabe qué pasó; Donaciones sabe a quién avisarle".

---

## 4. Monitoreo en tiempo real — `POST /camiones/{id}/posicion` y el dashboard

Mientras la ruta está `EN_CURSO`, la **app móvil del conductor** (alternativa elegida)
reporta posiciones:

```bash
curl -X POST http://localhost:8080/camiones/1/posicion \
  -H "Content-Type: application/json" \
  -d '{ "latitud": -34.6037, "longitud": -58.3816, "velocidad": 45.5,
        "timestamp": "2026-07-02T14:30:00Z" }'
```

El enunciado dice que la plataforma debe *"recibir, validar y procesar"* — y eso es
exactamente la estructura de `controller/GpsController.java:19-56`:

```java
if (camionRepository.buscarPorId(camionId).isEmpty()) { ... 404 ... }   // recibir
if (request.latitud < -90 || request.latitud > 90)   { ... 400 ... }    // validar
if (request.longitud < -180 || request.longitud > 180) { ... 400 ... }
if (request.velocidad < 0)   { ... 400 ... }
if (request.timestamp == null) { ... 400 ... }

PosicionCamion posicion = new PosicionCamion(camionId, ...);            // procesar
gpsRepository.guardarPosicion(posicion);
```

Demo del rechazo: mandá `"latitud": 200` y mostrá el `400 Latitud invalida`.

El dashboard administrativo consume lo acumulado:

```bash
curl http://localhost:8080/dashboard/camiones
# → última posición conocida de cada camión, para pintar el mapa
```

> **Justificación asociada:** sección 5 (por qué app móvil y no GPS fijo: sin hardware
> extra, reporta solo con ruta activa, la validación es nuestra responsabilidad según
> el enunciado).

---

## 5. La entidad confirma la recepción — `POST /entregas/{id}/confirmar`

**Demo:**

```bash
curl -X POST http://localhost:8080/entregas/1/confirmar \
  -H "Content-Type: application/json" \
  -d '{ "camionId": 1, "fechaHora": "2026-07-02T15:10:00" }'
```

**Qué pasa por dentro** (`controller/EntregaController.java:32-68`):

```java
entrega.confirmar(camion, fechaHora);        // dominio: EN_TRASLADO → ENTREGADA

EventoEntregaConfirmadaDTO evento = new EventoEntregaConfirmadaDTO(
        entrega.getId(),
        entrega.getDonacionId(),
        camion.getPatente(),                 // ← queda registrado QUÉ camión entregó
        fechaHora);
donacionesAPI.notificarEntregaConfirmada(evento);
```

Y en el dominio (`domain/Entrega.java:34-41`):

```java
public void confirmar(Camion camion, LocalDateTime fechaHora) {
    if (estado != EstadoEntrega.EN_TRASLADO) {
        throw new IllegalStateException("Solo se puede confirmar una entrega EN_TRASLADO. ...");
    }
    this.estado = EstadoEntrega.ENTREGADA;
    this.camionQueEntrego = camion;          // requisito: "quedará registrado qué camión"
    this.fechaHoraEntrega = fechaHora;
}
```

El evento lleva patente + fecha/hora porque el enunciado pide que la notificación de
entrega exitosa incluya *"un comprobante de entrega, indicando fecha, hora y camión
responsable"* — Logística aporta esos datos y Donaciones arma y envía el comprobante.

Después la entidad carga las fotos que pide el enunciado:

```bash
curl -X POST http://localhost:8080/entregas/1/fotos \
  -H "Content-Type: application/json" -d '{ "url": "https://fotos/donacion1.jpg" }'
```

---

## 6. La entrega falla y se replanifica — `rechazar` / `retornar`

Camino triste, en dos pasos que mapean 1:1 con el enunciado:

```bash
# La entidad informa que NO recibió la entrega → NO_RECIBIDA
curl -X POST http://localhost:8080/entregas/1/rechazar \
  -H "Content-Type: application/json" -d '{ "motivo": "Nadie en el domicilio" }'

# La donación regresa al depósito → vuelve a PENDIENTE
curl -X POST http://localhost:8080/entregas/1/retornar
```

`rechazar` (`EntregaController.java:70-97`) notifica a Donaciones con el **motivo**
(`EventoEntregaFallidaDTO`), para que allá se avise a entidad, donante y administradores
como exige el enunciado. `retornarADeposito` (`domain/Entrega.java:50-57`) además
**limpia el camión y la fecha**:

```java
public void retornarADeposito() {
    if (estado != EstadoEntrega.NO_RECIBIDA) { throw new IllegalStateException(...); }
    this.estado = EstadoEntrega.PENDIENTE;
    this.camionQueEntrego = null;   // la entrega queda "como nueva"
    this.fechaHoraEntrega = null;
}
```

Y acá se cierra el círculo: una entrega `PENDIENTE` vuelve a ser candidata en la
**próxima corrida del scheduler** (flujo 2) — *"podrá generarse una nueva asignación de
ruta para la donación en cuestión"*.

> **Justificación asociada:** sección 6 (trazabilidad: el ciclo completo
> PENDIENTE → EN_TRASLADO → ENTREGADA / NO_RECIBIDA → PENDIENTE).

---

## Mapa mental para la defensa (1 minuto)

1. **Todo entra por un controller REST** que valida y traduce; **todo estado vive en el
   dominio** (`Ruta`, `Entrega`), que se defiende solo con `IllegalStateException`.
2. **Todo lo que sale** hacia otro sistema pasa por un **puerto** (`DonacionesAPI`,
   `PlanificadorExternoAPI`); el HTTP concreto vive en los adaptadores.
3. **Consultas: sincrónicas. Avisos: asincrónicos** (`sendAsync`, sin reintentos) —
   Logística nunca se bloquea por Donaciones.
4. **Entre servicios viajan solo IDs**, según `CONTRATO-DONACIONES.md`.
5. **El tiempo también es un actor**: el scheduler dispara la planificación sin
   intervención humana, en lotes de ≤ 100, y el resultado vuelve por callback.
