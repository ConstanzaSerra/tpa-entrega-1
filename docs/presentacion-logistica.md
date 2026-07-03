# Presentacion — Microservicio de Logistica (DonaTrack)

Guia de recorrido para la defensa oral. Cada seccion explica una capa del sistema,
para que temas como hablar y que mostrar en el codigo.

---

## 0. El flujo completo (arrancar siempre por aca)

Antes de entrar en detalle, conviene tener claro como fluye un request de punta a punta.
Cada capa tiene una unica responsabilidad:

```
HTTP request
    → Controller         (recibe el request, valida formato)
        → DTO            (traduce el JSON a un objeto Java)
            → Domain     (aplica la regla de negocio)
            → Repository (guarda o busca objetos de dominio)
            → Puerto     (si necesita hablar con otro sistema externo)
                → Adaptador  (hace el HTTP real hacia ese sistema)
HTTP response
    ← Controller         (devuelve el resultado como JSON via DTO)
```

### Que hace cada capa

**`domain/`** — el centro, las reglas de negocio
Son los objetos puros: `Camion`, `Ruta`, `Entrega`, `PosicionCamion`. No saben nada de
HTTP ni de base de datos. Aca viven las reglas: si intenta confirmar una entrega que no
esta EN_TRASLADO, el dominio tira `IllegalStateException`. El controller la atrapa y
devuelve `400`.

**`repository/`** — el guardado
Interfaz que define como guardar y buscar objetos de dominio (`buscarPorId`, `guardar`,
`buscarTodos`). La implementacion `InMemory*` los guarda en un `Map` en memoria. Si
manana se usa una base de datos, solo se cambia la implementacion, el resto del codigo
no se toca.

**`dto/`** — el traductor de JSON
Cuando llega un JSON por HTTP, no se convierte directo a un objeto de dominio. Primero
va a un DTO (objeto plano, solo datos), y el DTO construye el objeto de dominio. Esto
separa la forma del JSON de las reglas del negocio. Tambien se usa para la respuesta:
el dominio no se serializa directo, pasa por un DTO primero.

**`puertos/`** — los contratos hacia afuera
Son interfaces que el dominio define para hablar con sistemas externos: `DonacionesAPI`,
`PlanificadorExternoAPI`. El dominio solo conoce la interfaz, no sabe si del otro lado
hay HTTP, una base de datos o un mock de test. Esto es lo que permite testear sin
levantar ningun servidor.

**`adaptadores/`** — la tecnologia concreta
Implementan los puertos con HTTP real (`DonacionesHttpAdapter`,
`PlanificadorHttpAdapter`). Son los unicos que saben de `HttpClient`, URLs y JSON. Si
el proveedor externo cambia de API, solo se toca el adaptador.

**`controller/`** — la puerta de entrada
Recibe el request HTTP, lo convierte a DTO, llama al dominio o al repositorio, y
devuelve la respuesta HTTP. No tiene logica de negocio propia: no decide si una entrega
puede confirmarse, eso lo decide `Entrega.confirmar()`.

**`service/`** — orquestacion de casos de uso complejos
Cuando un caso de uso necesita coordinar varias cosas (consultar donaciones al otro
microservicio, armar lotes de 100, llamar al planificador), esa logica va en un service
para no inflar el controller. No es el servidor — es solo una clase Java que coordina.

**`LogisticaApp.java`** — el unico punto que conecta todo
Crea los repositorios, los adaptadores, los servicios y los controllers, y se los
inyecta entre si. Configura Javalin (el servidor HTTP) y registra todas las rutas REST.
Es el unico archivo que conoce las implementaciones concretas; todo lo demas trabaja con
interfaces.

---

## 1. El dominio (el corazon, sin tecnologia)

Archivo clave: `src/main/java/.../logistica/domain/`

### `Camion`
Tiene exactamente los atributos que pide el enunciado: patente, volumen (m3), altura (m)
y capacidad de carga (kg).

### `Ruta` + `EstadoRuta`
Maquina de estados: `PLANIFICADA → EN_CURSO → COMPLETADA`.
- `iniciar()` valida que este `PLANIFICADA`; si no, tira `IllegalStateException`.
- Tiene una lista de `ParadaDeRuta`, cada una con sus `Entrega`s.

### `ParadaDeRuta`
Representa una parada en la ruta: guarda el ID de la entidad beneficiaria y la
direccion. Agrupa las entregas que se hacen en esa direccion.

### `Entrega` + `EstadoEntrega`
Maquina de estados: `PENDIENTE → EN_TRASLADO → ENTREGADA` (o `NO_RECIBIDA → PENDIENTE`).
- Guarda `donacionId` y `entidadBeneficiariaId` como `Long`, no como objetos — la
  frontera entre microservicios se cruza solo con IDs.
- `confirmar(camion, fechaHora)` registra que camion entrego y cuando.
- `retornarADeposito()` limpia el camion y la fecha, dejandola lista para replanificar.

### `PosicionCamion`
Posicion reportada por la app movil del conductor: camionId, latitud, longitud,
velocidad, timestamp.

---

## 2. Los puertos (interfaces) — el borde del hexagono

Archivo clave: `src/main/java/.../logistica/puertos/`

### `DonacionesAPI` (puerto de salida hacia el microservicio de Donaciones)
Define cuatro operaciones:
- `obtenerDonacionesListasParaRepartir()` — consulta sincronica, Logistica necesita los
  datos para continuar.
- `notificarInicioRuta()`, `notificarEntregaConfirmada()`, `notificarEntregaFallida()`
  — notificaciones asincronicas (fire-and-forget), Logistica no espera respuesta.

### `PlanificadorExternoAPI` (puerto de salida hacia el planificador)
- `solicitarPlanificacion(request)` — envia un lote de donaciones y camiones. El
  planificador responde despues por el callback, no en el mismo request.

---

## 3. Los adaptadores — la tecnologia concreta

Archivo clave: `src/main/java/.../logistica/adaptadores/`

### `DonacionesHttpAdapter implements DonacionesAPI`
- El `GET` usa `httpClient.send()` (sincronico): espera la respuesta.
- Los `POST` de notificacion usan `httpClient.sendAsync()`: no bloquean, y si fallan
  solo loguean. Logistica sigue funcionando aunque Donaciones este caido.

### `PlanificadorHttpAdapter implements PlanificadorExternoAPI`
- Envia el request con la URL de callback incluida para que el planificador sepa donde
  devolver el resultado.

---

## 4. Los repositorios — persistencia

Archivo clave: `src/main/java/.../logistica/repository/`

Cuatro interfaces (`CamionRepository`, `RutaRepository`, `EntregaRepository`,
`GpsRepository`) con implementaciones en memoria (`InMemory*`). Guardan en `Map<Long,
objeto>` y asignan IDs autoincrementales. La catedra no prescribe tecnologia de
persistencia para esta entrega; la interfaz permite cambiarla sin tocar dominio ni
controllers.

---

## 5. Los controllers — puerto de entrada (endpoints REST)

Archivo clave: `src/main/java/.../logistica/controller/`

| Controller | Endpoints |
|---|---|
| `CamionController` | `GET /camiones`, `GET /camiones/{id}`, `POST`, `PUT`, `DELETE` |
| `RutaController` | `GET /rutas`, `GET /rutas/{id}`, `POST /rutas/{id}/iniciar` |
| `EntregaController` | `GET /entregas`, `GET /entregas/{id}`, confirmar, rechazar, retornar, fotos |
| `GpsController` | `POST /camiones/{id}/posicion`, `GET /dashboard/camiones` |
| `PlanificadorController` | `POST /planificador/callback` |

Patron comun de todos los controllers:
1. Extraer parametros del request (`pathParam`, `bodyAsClass`).
2. Buscar el recurso en el repositorio; si no existe, `404`.
3. Llamar al metodo del dominio; si tira `IllegalStateException`, `400`.
4. Si necesita avisar al otro microservicio, llamar al puerto (asincronica o
   sincronicamente segun el caso).
5. Responder con el codigo HTTP correcto.

---

## 6. Los servicios y el scheduler — la planificacion automatica

### `PlanificadorRutasService`
Orquesta el caso de uso de planificacion:
1. Llama a `DonacionesAPI` para obtener las donaciones con estado `ASIGNACION_REALIZADA`.
2. Junta la flota de camiones del repositorio local.
3. Parte las donaciones en lotes de maximo 100 (restriccion del proveedor).
4. Por cada lote llama a `PlanificadorExternoAPI` con la URL de callback incluida.

### `PlanificacionScheduler`
Ejecuta `PlanificadorRutasService.planificar()` periodicamente con un
`ScheduledExecutorService`. El periodo se configura en `application.properties`
(`scheduler.period.minutes`). En la demo corre cada 1 minuto; en produccion se
configuraria para el horario de baja carga que pide el enunciado.

---

## 7. El armado (`LogisticaApp`) — como se conecta todo

```
1. Repositorios en memoria
2. Adaptadores HTTP (URLs desde config, no hardcodeadas)
3. PlanificadorRutasService (recibe los puertos por constructor)
4. PlanificacionScheduler (recibe el service)
5. Controllers (reciben repositorios y puertos por constructor)
6. Javalin: registra todas las rutas y arranca en el puerto configurado
```

Ningun controller o service hace `new DonacionesHttpAdapter()` — siempre reciben las
dependencias por constructor. Esto es lo que permite reemplazarlas por mocks en los
tests sin levantar ningun servidor.

---

## Elevator pitch (1 minuto)

El microservicio de Logistica es responsable de todo lo que pasa con una donacion
despues de que Donaciones le asigna un destino: planifica las rutas, traza las entregas
y monitorea los camiones en tiempo real.

Por dentro aplica arquitectura hexagonal: el dominio en el centro con sus maquinas de
estado (`Ruta`, `Entrega`), puertos como interfaces hacia los sistemas externos
(Donaciones, planificador externo, app movil), y adaptadores HTTP que implementan esos
puertos. Los controllers son la puerta de entrada REST. Todo se conecta en
`LogisticaApp`, que es el unico punto que conoce las implementaciones concretas.

La integracion con Donaciones usa dos estilos: consultas sincronicas cuando necesita
datos para continuar, y notificaciones asincronicas (fire-and-forget) para los eventos,
de modo que Logistica sigue operando aunque Donaciones este caido.

---

## Preguntas frecuentes

**Por que los puertos son interfaces y no clases?**
Para que los tests puedan pasar un mock en vez del adaptador HTTP real. Sin eso, cada
test levantaria un servidor.

**Por que `Entrega` guarda `donacionId` como Long y no como objeto `Donacion`?**
Porque `Donacion` pertenece al microservicio de Donaciones. Pasar el objeto implicaria
compartir codigo entre servicios, que es exactamente el acoplamiento que la division en
microservicios quiere evitar.

**Por que las notificaciones son asincronicas?**
Para que el chofer no espere a que Donaciones conteste antes de que el sistema confirme
el inicio de ruta. Si Donaciones esta caido, Logistica sigue funcionando.

**Por que lotes de 100?**
Es una restriccion del proveedor externo de planificacion (requerimiento de
implementacion 2 del enunciado). `PlanificadorRutasService` lo maneja con
`Math.ceil(size / 100)` iteraciones.
