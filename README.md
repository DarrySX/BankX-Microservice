# BankX — transactions-service

Microservicio **reactivo** de movimientos bancarios con evaluación de riesgo contra un módulo legado bloqueante.

- **Spring WebFlux** (Netty) — endpoints no bloqueantes
- **MongoDB reactivo** — cuentas y transacciones (`ReactiveMongoRepository`)
- **JPA + H2** — módulo legado de reglas de riesgo, aislado con `Schedulers.boundedElastic()`
- **SSE** — stream en vivo de transacciones (`Sinks.many().multicast()`)
- **Clean Architecture + SOLID** — dominio puro, casos de uso por interfaz, infraestructura reemplazable

Puerto `8084` · Java 17 · Spring Boot 3.2.5 · Maven 3.9+

---

## Tabla de contenido

1. [Requisitos](#1-requisitos)
2. [Puesta en marcha](#2-puesta-en-marcha)
3. [Compilar y ejecutar](#3-compilar-y-ejecutar)
4. [Datos de ejemplo](#4-datos-de-ejemplo)
5. [Endpoints](#5-endpoints)
6. [Ejemplos completos](#6-ejemplos-completos)
7. [Demo del stream SSE](#7-demo-del-stream-sse)
8. [Colección Postman](#8-colección-postman)
9. [Tests](#9-tests)
10. [Arquitectura](#10-arquitectura)
11. [Códigos de error](#11-códigos-de-error)
12. [Política de riesgo](#12-política-de-riesgo)
13. [Configuración](#13-configuración)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Requisitos

| Herramienta | Versión usada | Comprobación |
|---|---|---|
| JDK | 17 (Temurin 17.0.20) | `java -version` |
| Maven | 3.9.16 | `mvn -v` |
| Podman | 6.0.2 | `podman --version` |

No necesitas Maven instalado si usas el wrapper incluido: `./mvnw` (Linux/macOS) o `.\mvnw.cmd` (Windows).

---

## 2. Puesta en marcha

### MongoDB con Podman

```bash
podman run -d --name mongo-bankx -p 127.0.0.1:27017:27017 docker.io/library/mongo:6
```

Arranques posteriores:

```bash
podman start mongo-bankx
```

> ⚠️ **El prefijo `127.0.0.1:` no es opcional en Windows/WSL.** Sin él, Podman publica el puerto solo en `[::1]`: el puerto *parece* abierto (`Test-NetConnection` devuelve `True`) pero el relay corta la conexión al leer, y la app muere tras 30 s con `MongoSocketReadException`. Ver [Troubleshooting](#14-troubleshooting).

Comprueba que quedó bien publicado:

```powershell
netstat -ano | Select-String ":27017"
# Debe verse  127.0.0.1:27017  ...  LISTENING
# Si ves      [::1]:27017      ...  recrea el contenedor con el prefijo
```

Y que mongod responde:

```bash
podman exec mongo-bankx mongosh --quiet --eval "db.runCommand({ping:1}).ok"
# 1
```

### H2

No hay nada que levantar: es una base **en memoria** que arranca y muere con la aplicación.

---

## 3. Compilar y ejecutar

```bash
# compilar + tests
mvn clean verify

# levantar
mvn spring-boot:run

# o desde el jar
java -jar target/transactions-service-0.0.1-SNAPSHOT.jar
```

Señales de que arrancó bien, en el log:

```
o.s.b.web.embedded.netty.NettyWebServer   : Netty started on port 8084
c.b.t.infrastructure.config.DataSeeder    : Seed completado
```

**Debe decir Netty, no Tomcat.** Si aparece Tomcat es que se coló `spring-boot-starter-web` en el `pom.xml`.

---

## 4. Datos de ejemplo

El `DataSeeder` recrea estos datos en cada arranque (las cuentas se borran y se vuelven a insertar; las reglas de riesgo viven en H2 con `ddl-auto: create-drop`).

**Cuentas (Mongo)**

| Número | Titular | Moneda | Saldo inicial |
|---|---|---|---|
| `001-0001` | Ana Peru | PEN | 2000 |
| `001-0002` | Luis Acuña | PEN | 800 |

**Reglas de riesgo (H2)**

| Moneda | Débito máximo por transacción |
|---|---|
| PEN | 1500 |
| USD | 500 |

---

## 5. Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/transactions` | Crea una transacción (201) |
| `GET` | `/api/transactions?accountNumber=…` | Lista por cuenta, orden descendente por fecha |
| `GET` | `/api/stream/transactions` | Stream SSE en vivo (`text/event-stream`) |

**Body de creación**

```json
{ "accountNumber": "001-0001", "type": "DEBIT", "amount": 100 }
```

| Campo | Reglas |
|---|---|
| `accountNumber` | obligatorio, no vacío |
| `type` | obligatorio, `DEBIT` o `CREDIT` (case-insensitive) |
| `amount` | obligatorio, ≥ 0.01 |

---

## 6. Ejemplos completos

Todas las salidas de abajo son reales, capturadas contra la app corriendo.

### 6.1 Crear transacción — OK

```bash
curl -i -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"001-0001","type":"DEBIT","amount":100}'
```

```
HTTP/1.1 201 Created
```
```json
{
  "id": "6a73c19c78239e7a936e9e4e",
  "accountId": "6a73c17d78239e7a936e9e4c",
  "type": "DEBIT",
  "amount": 100,
  "timestamp": "2026-08-05T23:05:00.442580700Z",
  "status": "OK",
  "reason": null
}
```

El saldo de `001-0001` pasa de **2000 a 1900**.

### 6.2 Rechazo por riesgo

```bash
curl -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"001-0001","type":"DEBIT","amount":2000}'
```

```json
{ "error": "risk_rejected" }
```
`400` — supera `maxDebitPerTx = 1500` para PEN.

### 6.3 Rechazo por fondos insuficientes

```bash
curl -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"001-0002","type":"DEBIT","amount":1200}'
```

```json
{ "error": "insufficient_funds" }
```
`400` — pasa riesgo (1200 ≤ 1500) pero el saldo de `001-0002` es 800.

> **Ojo con el orden.** Con `amount: 2000` en esta misma cuenta el error sería `risk_rejected`, no `insufficient_funds`: el riesgo se evalúa **antes** que el saldo. Usa 1200 para demostrar la rama de fondos.

### 6.4 Cuenta inexistente

```bash
curl -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"999-9999","type":"CREDIT","amount":50}'
```

```json
{ "error": "account_not_found" }
```

### 6.5 Payload inválido

```bash
curl -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"","type":"TRANSFER","amount":0}'
```

```json
{
  "error": "validation_error",
  "details": {
    "amount": "debe ser mayor que o igual a 0.01",
    "type": "invalid_transaction_type",
    "accountNumber": "no debe estar vacío"
  }
}
```

### 6.6 Abono (CREDIT)

Los créditos no pasan por el módulo de riesgo, así que cualquier importe se acepta:

```bash
curl -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"001-0002","type":"CREDIT","amount":5000}'
```

### 6.7 Listar por cuenta

```bash
curl "http://localhost:8084/api/transactions?accountNumber=001-0001"
```

```json
[
  { "id": "6a73c19c78239e7a936e9e4f", "type": "DEBIT", "amount": 50,  "timestamp": "2026-08-05T23:05:00.708Z", "status": "OK", "reason": null },
  { "id": "6a73c19c78239e7a936e9e4e", "type": "DEBIT", "amount": 100, "timestamp": "2026-08-05T23:05:00.442Z", "status": "OK", "reason": null }
]
```
`200` — ordenado por `timestamp` descendente. Solo aparecen las aprobadas: los rechazos no se persisten.

### 6.8 Comprobar saldos en Mongo

```bash
podman exec mongo-bankx mongosh bankx --quiet \
  --eval "db.accounts.find({}, {number:1, holderName:1, balance:1, _id:0})"
```

```json
[
  { "number": "001-0001", "holderName": "Ana Peru",   "balance": "1850" },
  { "number": "001-0002", "holderName": "Luis Acuña", "balance": "800" }
]
```

---

## 7. Demo del stream SSE

**Orden recomendado para la exposición:** abrir el stream → POST OK → POST riesgo → POST fondos → GET listar.

Terminal 1 — deja el stream abierto:

```bash
curl -N http://localhost:8084/api/stream/transactions
```

Terminal 2 — lanza una transacción:

```bash
curl -X POST http://localhost:8084/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"001-0001","type":"DEBIT","amount":100}'
```

En la terminal 1 aparece al instante:

```
event:transaction
data:{"id":"6a73c19c78239e7a936e9e4e","accountId":"6a73c17d78239e7a936e9e4c","type":"DEBIT","amount":100,"timestamp":"2026-08-05T23:05:00.442580700Z","status":"OK","reason":null}
```

Puntos a destacar:

- El sink es `multicast`, así que **varios clientes** reciben el mismo evento. Ábrelo en dos terminales a la vez.
- Solo se emiten transacciones **aprobadas**: el evento se publica con `doOnNext` *después* de persistir. Lanza un `risk_rejected` con el stream abierto y comprueba que no llega nada.
- El sink se creó con `autoCancel = false`: cerrar un cliente SSE no rompe el stream para los demás ni para las conexiones futuras.

---

## 8. Colección Postman

Importa [`postman/BankX.postman_collection.json`](postman/BankX.postman_collection.json). Trae los 7 requests con tests automáticos (status y cuerpo esperados) y la variable `{{baseUrl}}` apuntando a `http://localhost:8084`.

Para correrla entera desde terminal:

```bash
newman run postman/BankX.postman_collection.json
```

> El request 6 (SSE) es un stream: Postman lo deja abierto y no completa. Es el comportamiento correcto — para verlo mejor usa `curl -N`.

---

## 9. Tests

```bash
mvn clean verify
```

11 tests, 3 clases:

| Clase | Tipo | Qué cubre |
|---|---|---|
| `CreateTransactionServiceTest` | unitario puro | Las 4 ramas del caso de uso: cuenta inexistente, riesgo denegado, fondos insuficientes y camino feliz (incluye que el saldo debitado sea el persistido y que el evento se emita) |
| `JpaRiskPolicyAdapterTest` | integración (`@SpringBootTest` + H2) | Límite respetado, límite superado, créditos que saltan la evaluación, moneda sin regla |
| `JpaRiskPolicyAdapterDegradedTest` | unitario puro | Política degradada cuando el módulo legado está caído, y que los créditos ni tocan el repositorio |

`CreateTransactionServiceTest` corre **sin Mongo, sin H2 y sin contexto de Spring** — es la prueba de que la arquitectura está bien: la capa de aplicación se instancia con un `new` y mocks. Si necesitaras `@SpringBootTest` para probar una regla de negocio, algo se habría filtrado hacia adentro.

> Verás un stack trace en el log durante `JpaRiskPolicyAdapterDegradedTest`: es el `log.warn` esperado de la política degradada, no un fallo.

---

## 10. Arquitectura

```
                    ┌─────────────────────────────────────────┐
   HTTP / SSE       │        INFRASTRUCTURE (adapters in)     │
  ───────────────►  │  TransactionController · DTOs · Advice  │
                    └───────────────────┬─────────────────────┘
                                        │ depende de ▼
                    ┌─────────────────────────────────────────┐
                    │      APPLICATION (casos de uso)         │
                    │  CreateTransactionService               │
                    │  ListTransactionsService                │
                    │  StreamTransactionsService              │
                    └───────────────────┬─────────────────────┘
                                        │ depende de ▼
                    ┌─────────────────────────────────────────┐
                    │            DOMAIN (puro)                │
                    │  Account · Transaction · TransactionType│
                    │  Excepciones de negocio · PUERTOS       │
                    └───────────────────▲─────────────────────┘
                                        │ implementa
                    ┌───────────────────┴─────────────────────┐
                    │       INFRASTRUCTURE (adapters out)     │
                    │  Mongo reactivo │ JPA+H2 (legado) │Sinks│
                    └─────────────────────────────────────────┘
```

**Regla de dependencia:** las flechas apuntan siempre hacia adentro. El dominio no importa nada de Spring, Mongo ni JPA (solo `Mono`/`Flux` en las firmas de los puertos, la concesión práctica estándar en WebFlux).

### Flujo de `POST /api/transactions`

1. El controller valida el DTO y lo mapea a `CreateTransactionCommand`.
2. `CreateTransactionService` busca la cuenta en Mongo → si no existe, `account_not_found`.
3. Consulta `RiskPolicyPort` → el adaptador JPA/H2 corre en `boundedElastic()` → si excede el límite, `risk_rejected`.
4. El dominio aplica el movimiento (`Account.apply()`) → si no alcanza el saldo, `insufficient_funds`.
5. Persiste cuenta + transacción en Mongo.
6. Publica el evento en el sink → llega a los suscriptores SSE.
7. `GlobalErrorHandler` traduce cualquier `BusinessException` a `{"error": "<código>"}`.

### Estructura de paquetes

```
src/main/java/com/bankx/transactions/
├── TransactionsServiceApplication.java
│
├── domain/                                   ← SIN dependencias de framework
│   ├── model/          Account · Transaction · TransactionType · TransactionStatus
│   ├── exception/      BusinessException + 3 subclases
│   └── port/
│       ├── in/         CreateTransactionUseCase · CreateTransactionCommand
│       │               ListTransactionsUseCase · StreamTransactionsUseCase
│       └── out/        LoadAccountPort · SaveAccountPort · SaveTransactionPort
│                       LoadTransactionsPort · RiskPolicyPort
│                       PublishTransactionEventPort · SubscribeTransactionEventsPort
│
├── application/service/                      ← orquestación, sin anotaciones Spring
│   └── CreateTransactionService · ListTransactionsService · StreamTransactionsService
│
└── infrastructure/
    ├── adapter/in/web/         TransactionController · dto/ · GlobalErrorHandler
    ├── adapter/out/mongo/      document/ · repository/ · mapper/ · 2 adaptadores
    ├── adapter/out/legacyrisk/ RiskRule (@Entity) · RiskRuleJpaRepository
    │                           JpaRiskPolicyAdapter (boundedElastic)
    ├── adapter/out/event/      SinkTransactionEventAdapter
    └── config/                 PersistenceConfig · BeanConfig · DataSeeder
```

> **Nota clave:** los adaptadores Mongo y JPA viven en **paquetes distintos** a propósito, y `PersistenceConfig` acota el scanning de cada uno. Si mezclas `data-jpa` y `data-mongodb-reactive` en el mismo paquete, Spring intenta crear repositorios JPA a partir de las interfaces reactivas y la app no arranca.

### Por qué `boundedElastic()`

WebFlux corre sobre pocos hilos de Netty (uno por core). Una llamada JDBC bloquea ese hilo y con él todas las peticiones que estaba atendiendo. `boundedElastic` mueve la llamada a un pool elástico y acotado, diseñado justo para envolver I/O bloqueante legado.

### Cómo se aplica SOLID

| Principio | Aplicación |
|---|---|
| **S** | `CreateTransactionService` solo orquesta; `Account` solo conoce la regla de saldo; `JpaRiskPolicyAdapter` solo traduce JPA→reactivo; el controller solo traduce HTTP. |
| **O** | Nueva regla de riesgo = nuevo adaptador de `RiskPolicyPort`, sin tocar el caso de uso. Nuevo error de negocio = nueva subclase de `BusinessException`, sin tocar `GlobalErrorHandler`. |
| **L** | Toda implementación de `LoadAccountPort` respeta el contrato: `Mono.empty()` cuando no existe (no `null`, no excepción). Por eso `switchIfEmpty` funciona igual con Mongo, con un mock o con una caché. |
| **I** | Puertos finos: `LoadAccountPort`/`SaveAccountPort` separados, `PublishTransactionEventPort`/`SubscribeTransactionEventsPort` separados, aunque un mismo adaptador implemente ambos. |
| **D** | Las interfaces las **define el dominio** y las **implementa la infraestructura**. `application` no importa nada de `infrastructure`; el wiring vive en `BeanConfig`. Verificable: borrar el paquete Mongo no rompe la compilación de `domain` ni de `application`. |

---

## 11. Códigos de error

Todos responden `400` con el cuerpo `{"error": "<código>"}`.

| Código | Cuándo |
|---|---|
| `account_not_found` | El `accountNumber` no existe |
| `risk_rejected` | El débito supera el límite del módulo de riesgo |
| `insufficient_funds` | El saldo no alcanza para el débito |
| `invalid_transaction_type` | El `type` no es `DEBIT` ni `CREDIT` |
| `validation_error` | Falla la validación del DTO; incluye `details` por campo |

Agregar una regla de negocio nueva = crear una subclase de `BusinessException` con su código. **Cero cambios en el handler** (OCP).

Las rutas inexistentes devuelven `404`, no `500`: `GlobalErrorHandler` deja pasar las `ResponseStatusException` con su status original antes de llegar al handler genérico.

---

## 12. Política de riesgo

Comportamiento de `JpaRiskPolicyAdapter`:

| Situación | Resultado |
|---|---|
| `CREDIT` | Aprobado sin consultar el módulo legado |
| `DEBIT` con regla para la moneda | Aprobado si `amount ≤ maxDebitPerTx` |
| `DEBIT` sin regla para la moneda | **Rechazado** (el límite por defecto es 0) |
| Módulo legado caído | Reintentos con backoff y, si persiste, **política degradada** |

**Degradación controlada.** Si H2 no responde, la llamada tiene `timeout(2s)` y se reintenta hasta 3 veces con backoff exponencial y jitter. Agotados los reintentos, en vez de propagar un 500 se aplica un límite conservador de **100** y se registra un `WARN`. Es deliberadamente más estricto que cualquier regla de la tabla: ante la duda, preferimos rechazar de más a aprobar a ciegas.

---

## 13. Configuración

`src/main/resources/application.yml`:

| Propiedad | Valor por defecto |
|---|---|
| `server.port` | `8084` |
| `spring.data.mongodb.uri` | `mongodb://127.0.0.1:27017/bankx` |
| `spring.datasource.url` | `jdbc:h2:mem:riskdb;DB_CLOSE_DELAY=-1` |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` |

La URI de Mongo es sobreescribible por entorno sin tocar el yml:

```bash
# bash
MONGODB_URI=mongodb://localhost:27017/bankx mvn spring-boot:run
```
```powershell
# PowerShell
$env:MONGODB_URI = "mongodb://localhost:27017/bankx"; mvn spring-boot:run
```

---

## 14. Troubleshooting

### La app muere al arrancar con `MongoTimeoutException` tras 30 segundos

Causa habitual en Windows/WSL: **Podman publicó el puerto solo en IPv6**. Diagnóstico:

```powershell
netstat -ano | Select-String ":27017"
```

Si ves `[::1]:27017` en lugar de `127.0.0.1:27017`, recrea el contenedor con la IP explícita:

```bash
podman rm -f mongo-bankx
podman run -d --name mongo-bankx -p 127.0.0.1:27017:27017 docker.io/library/mongo:6
```

Lo engañoso del caso: `Test-NetConnection localhost -Port 27017` devuelve `True` y el contenedor está perfectamente sano — el relay acepta el handshake y corta al leer. No pierdas tiempo depurando la app.

### `Port 8084 was already in use`

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*transactions-service*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

### `podman: port is already allocated`

Otro contenedor ya tiene tomado el 27017. Localízalo con `podman ps -a` y páralo (`podman stop <nombre>`), o publica este en otro puerto host cambiando el lado izquierdo del mapeo y la URI de Mongo.

### Arranca Tomcat en vez de Netty

Se coló `spring-boot-starter-web` en el `pom.xml`. Quítalo: convive mal con WebFlux.

### La consola H2 en `/h2` devuelve 404

Es lo esperado y **no es un bug**: `H2ConsoleAutoConfiguration` está anotada `@ConditionalOnWebApplication(type = SERVLET)` y aquí no hay contenedor servlet, solo Netty. La configuración `spring.h2.console` del yml queda inerte. Para inspeccionar las reglas de riesgo usa el log de Hibernate, que ya viene en `debug`.

### El stream SSE no muestra nada

Comprueba que la transacción se aprobó: los rechazos no se publican, por diseño. Y usa `curl -N` — sin `-N`, curl bufferiza y parece que no llega nada.

### Los datos de ejemplo desaparecieron

El `DataSeeder` borra y recrea las cuentas en **cada arranque**. Reinicia la app y vuelven al estado inicial (2000 y 800).
