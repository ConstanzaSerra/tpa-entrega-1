# Contrato de integración — Logística ↔ Donaciones

Este documento define el **contrato HTTP** entre los microservicios de **Logística** y
**Donaciones**. No es código de ninguno de los dos: es el **acuerdo** sobre qué endpoints,
qué verbos y qué forma de JSON se usan para que se puedan hablar.

> Regla de oro: **los nombres de los campos del JSON deben coincidir exactamente**
> (`donacionId`, no `idDonacion` ni `donacion_id`). El mapeo JSON es por nombre.

## Puertos (según `application.properties`)

| Servicio | URL base |
|---|---|
| Logística | `http://localhost:8080` |
| Donaciones | `http://localhost:8081` |
| Planificador (externo) | `http://localhost:8082` |

Las URLs no están hardcodeadas: se configuran vía `application.properties` o variables de
entorno (`API_DONACIONES_URL`, etc.).

---

## Parte A — Endpoints que Donaciones debe EXPONER (los consume Logística)

Logística le pega a estos endpoints en `http://localhost:8081`. **El equipo de Donaciones
tiene que implementarlos.**

### A.1 — Consultar donaciones listas para repartir

```
GET /donaciones?estado=ASIGNACION_REALIZADA
```

**Respuesta** `200 OK` — una **lista** de donaciones:

```json
[
  { "donacionId": 1, "entidadBeneficiariaId": 5, "direccionDestino": "Av. Siempreviva 742" },
  { "donacionId": 2, "entidadBeneficiariaId": 8, "direccionDestino": "Calle Falsa 123" }
]
```

| Campo | Tipo | Descripción |
|---|---|---|
| `donacionId` | número | ID de la donación |
| `entidadBeneficiariaId` | número | ID de la entidad que la recibe |
| `direccionDestino` | texto | Dirección de entrega |

> Si no hay donaciones, devolver una lista vacía `[]` (no un 404).

---

## Parte B — Endpoints que Donaciones debe EXPONER para recibir notificaciones

Cuando cambia el estado de una ruta o una entrega, Logística **avisa** a Donaciones con un
`POST`. El envío es **asincrónico** (fire-and-forget): Logística no espera respuesta y no
reintenta. Devolver `200`/`202` alcanza. **El contrato manda solo IDs**, no objetos completos.

### B.1 — Se inició una ruta

```
POST /notificaciones/inicio-ruta
```

```json
{
  "rutaId": 1,
  "patenteCamion": "AB123CD",
  "linkMapa": null,
  "entregasAfectadas": [
    { "entregaId": 10, "donacionId": 1, "entidadId": 5 },
    { "entregaId": 11, "donacionId": 2, "entidadId": 8 }
  ]
}
```

> Nota: hoy `linkMapa` puede llegar `null` (aún no lo provee el planificador).

### B.2 — Entrega confirmada

```
POST /notificaciones/entrega-confirmada
```

```json
{
  "entregaId": 10,
  "donacionId": 1,
  "patenteCamion": "AB123CD",
  "fechaHora": "2026-07-02T14:30:00"
}
```

> `fechaHora` es un `LocalDateTime` en formato ISO-8601 (`yyyy-MM-ddTHH:mm:ss`).

### B.3 — Entrega fallida (rechazada)

```
POST /notificaciones/entrega-fallida
```

```json
{
  "entregaId": 10,
  "donacionId": 1,
  "motivo": "Nadie en el domicilio"
}
```

---

## Parte C — Endpoints que Logística EXPONE (referencia para el resto del equipo)

Por si Donaciones (u otro front) necesita consultar el estado desde Logística
(`http://localhost:8080`):

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/rutas` · `/rutas/{id}` | Consultar rutas |
| GET | `/entregas` · `/entregas/{id}` | Consultar entregas |
| GET | `/camiones` · `/camiones/{id}` | Consultar camiones |
| GET | `/dashboard/camiones` | Últimas posiciones GPS de los camiones |
| POST | `/planificador/callback` | **Lo llama el Planificador** para devolver las rutas armadas |

> Aclaración: el `DonacionesAPI` (interfaz Java en `puertos/`) es **interno de Logística** —
> Donaciones NO lo implementa. Lo que se comparte es este contrato HTTP, no clases Java.

---

## Estado / pendientes de coordinación

- [ ] Confirmar que Donaciones expone `GET /donaciones?estado=ASIGNACION_REALIZADA` (A.1).
- [ ] Confirmar que Donaciones recibe los 3 `POST /notificaciones/...` (B.1, B.2, B.3).
- [ ] Verificar que los nombres de los campos JSON coinciden en ambos lados.
- [ ] Acordar si Donaciones necesita algún otro `GET` de Logística (Parte C).

> Importante: si estos endpoints aún no existen, Logística **igual compila y arranca**.
> Las notificaciones fallan en silencio (solo loguean) y el `GET` devuelve lista vacía.
> Conviene cerrar este contrato antes de la demo para no descubrirlo en vivo.
