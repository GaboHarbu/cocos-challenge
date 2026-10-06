# Cocos Capital — Backend Challenge API

Solución del challenge técnico de backend para Cocos Capital. Desarrollada en **Java 21 (LTS)** y **Spring Boot 3.4**, con persistencia en **PostgreSQL**, migraciones versionadas con **Flyway** y observabilidad completa en contenedor (**Prometheus**, **Grafana**, **Loki**, **Promtail**).

---

## Ejecución Rápida con Docker

**Requisito único:** Docker Desktop / Docker Compose.

```bash
docker compose up --build -d
```

Una vez que los contenedores estén levantados:

| Servicio | URL / Acceso | Detalle |
| :--- | :--- | :--- |
| **API REST** | `http://localhost:8080` | Entrypoint principal de la aplicación |
| **Swagger UI** | `http://localhost:8080/swagger-ui/index.html` | Documentación OpenAPI interactiva |
| **Health Check** | `http://localhost:8080/actuator/health` | Estado del servicio y conexión a BD |
| **PostgreSQL** | `localhost:5432` | Usuario/Password: `postgres` / `postgres` (DB: `cocos_local`) |
| **pgweb** | `http://localhost:8081` | Interfaz web ligera para consultar PostgreSQL |
| **Grafana** | `http://localhost:3000` | Dashboards y logs (User: `admin` / Pass: `admin`) |
| **Prometheus** | `http://localhost:9090` | Métricas del sistema y del Actuator |

> **Scripts de conveniencia:** Se incluyen scripts para gestionar todo el entorno con un solo comando:
> - **macOS / Linux:** `./run-infra.sh` (ej. `./run-infra.sh` para levantar todo, `./run-infra.sh infra` para solo dependencias)
> - **Windows:** `run-infra.bat` (ej. `run-infra.bat` para levantar todo, `run-infra.bat infra` para solo dependencias)

---

## Decisiones de Diseño y Criterio Financiero

### 1. La tabla `orders` como Ledger Contable (Proyección de Saldos)
En este modelo no existe una tabla de "saldos" o "cuentas corrientes". El estado patrimonial del usuario es una **proyección derivada de la tabla `orders`**:
* **`totalCash` (Efectivo contable/liquidado):** Suma neta de transferencias (`CASH_IN` / `CASH_OUT`) y operaciones efectivamente ejecutadas (`FILLED`). Este dinero sigue perteneciendo al usuario.
* **`availableCash` (Poder de compra real):** Descuenta las **reservas comprometidas en órdenes vivas**:
  $$\text{availableCash} = \text{totalCash} - \sum (\text{BUY en estado NEW}: \text{size} \times \text{price})$$
  Si un usuario tiene $1.000.000 y coloca una orden `LIMIT BUY` por $200.000 que queda en `NEW`, su patrimonio sigue siendo de $1.000.000, pero su disponible para emitir nuevas órdenes baja a $800.000.
* **`totalAccountValue` (Patrimonio neto total):**
  $$\text{totalAccountValue} = \text{totalCash} + \sum (\text{marketValue de posiciones})$$
  El dinero reservado en órdenes límite no se descuenta del patrimonio hasta que la orden se liquide.

### 2. Concurrencia y Consistencia Transaccional (Sin Redis)
Para evitar *overdrafts* (sobregiros de saldo) y carreras concurrentes (*check-then-act* write skew):
* Cada orden abre una transacción de base de datos que ejecuta de inmediato un **bloqueo pesimista a nivel de fila sobre el usuario**:
  ```sql
  SELECT * FROM users WHERE id = ? FOR UPDATE;
  ```
* **Por qué en PostgreSQL y no en Redis:** Introducir locks distribuidos en Redis agrega riesgo de *split-brain*, complejidad de red y desincronización con el motor relacional. PostgreSQL garantiza serialización atómica (ACID) por usuario en milisegundos sin generar contención entre usuarios distintos.

### 3. Idempotencia Estricta a Nivel Financiero
El endpoint de creación de órdenes acepta el header opcional `Idempotency-Key`:
* **Validación de formato:** Rechaza claves vacías, en blanco o malformadas mediante regex (`^[a-zA-Z0-9_-]{1,64}$`), devolviendo `400 Bad Request`.
* **Detección de colisión / Mismatch de intención:** Si un cliente reintenta la misma clave pero con un payload diferente (por ejemplo, cambiando el `side` de BUY a SELL, o alterando el instrumento), la API lo detecta y rechaza con `400 Bad Request` en vez de procesar una orden equivocada.
* **Replay idéntico:** Si la orden ya fue procesada, responde con la orden existente de inmediato (`200 OK` o `201 Created`), garantizando que problemas de red no dupliquen transacciones.

### 4. Rechazo de Órdenes como Evento de Negocio
* Si una orden no cumple con fondos suficientes o acciones disponibles, **se persiste en base de datos con estado `REJECTED`** (con su motivo detallado) y responde `200 OK` con metadata de rechazo.
* Esto preserva la trazabilidad y auditoría requerida por normativas financieras. Los errores HTTP `4xx` quedan reservados exclusivamente para fallos de transporte, sintaxis o reglas estructurales de la petición.

### 5. Cálculo de Acciones por Monto (`amount`)
Cuando el usuario envía un monto a invertir en lugar de cantidad de acciones, se calcula el número entero máximo permitido:
$$\text{size} = \left\lfloor \frac{\text{amount}}{\text{price}} \right\rfloor$$
No se permiten fracciones de acciones. Si el monto enviado no alcanza para adquirir al menos 1 acción completa al precio de mercado, la orden se rechaza y se persiste en estado `REJECTED`.

### 6. Valuación de Posiciones y Rendimientos
* **Filtrado:** Solo las órdenes `FILLED` en activos de tipo `ACCIONES` componen las tenencias (el `ARS` es un instrumento `MONEDA` que representa el efectivo).
* **Precio Promedio Ponderado (PPP):** Las compras calculan el costo medio de adquisición. Las ventas reducen la tenencia sin alterar el precio promedio de compra.
* **Rendimiento Diario (`dailyReturnPercent`):** Variación intrínseca del activo frente al día anterior:
  $$\text{dailyReturnPercent} = \frac{\text{close} - \text{previousClose}}{\text{previousClose}} \times 100$$
* **Rendimiento Total (`totalReturnPercent`):** Rendimiento acumulado de la posición del usuario frente a su costo promedio:
  $$\text{totalReturnPercent} = \frac{\text{marketPrice} - \text{averageCost}}{\text{averageCost}} \times 100$$
* Todos los cálculos monetarios usan `BigDecimal` con escala y redondeo bancario (`RoundingMode.HALF_EVEN`).

---

## Referencia de la API

### 1. Portfolio de Usuario
```http
GET /api/v1/portfolio/{userId}
GET /api/v1/portfolio?userId={userId}
```
**Respuesta (`200 OK`):**
```json
{
  "userId": 1,
  "accountNumber": "10001",
  "totalAccountValue": 904784.00,
  "availableCash": 654161.00,
  "totalCash": 678592.80,
  "positions": [
    {
      "instrumentId": 34,
      "ticker": "GGAL",
      "name": "Grupo Financiero Galicia",
      "size": 84,
      "marketPrice": 885.80,
      "marketValue": 74407.20,
      "dailyReturnPercent": -3.48,
      "totalReturnPercent": 0.00
    }
  ]
}
```

### 2. Búsqueda de Instrumentos
Soporta búsqueda por subcadena (*contains*) tanto por ticker como por nombre, case-insensitive y paginada.
```http
GET /api/v1/instruments?query=gal&page=0&size=10
```

### 3. Envío de Órdenes
```http
POST /api/v1/orders
Content-Type: application/json
Idempotency-Key: 7b8c2d1e-9f3a-4b5c-8d1e-2f3a4b5c6d7e
```
* **MARKET por cantidad (`size`):**
  ```json
  { "userId": 1, "ticker": "GGAL", "side": "BUY", "type": "MARKET", "size": 10 }
  ```
* **MARKET por monto en pesos (`amount`):**
  ```json
  { "userId": 1, "instrumentId": 34, "side": "BUY", "type": "MARKET", "amount": 50000.00 }
  ```
* **LIMIT (requiere `price` límite):**
  ```json
  { "userId": 1, "ticker": "PAMP", "side": "BUY", "type": "LIMIT", "size": 5, "price": 920.00 }
  ```

### 4. Cancelación de Órdenes
Solo aplicable a órdenes con estado `NEW`.
```http
POST /api/v1/orders/{orderId}/cancel
```
* Si la orden ya está `FILLED` o `CANCELLED`: Retorna `422 Unprocessable Entity`.
* Si no existe: Retorna `404 Not Found`.

---

## Observabilidad y Trazabilidad

El proyecto implementa observabilidad como ciudadano de primera clase:

1. **Inyección de `X-Trace-Id`:** El filtro `MdcTraceIdFilter` genera un identificador hexadecimal único de 16 caracteres por solicitud, lo inyecta en el `MDC` de SLF4J y lo retorna en los headers HTTP de respuesta.
2. **Logs Estructurados por Contexto:**
   ```text
   2026-10-06 03:41:53.193 [http-nio-8080-exec-7] [traceId=01bd038fbcc14734] INFO  ... - [HTTP] [REQUEST] method=POST path=/api/v1/orders
   2026-10-06 03:41:53.201 [http-nio-8080-exec-7] [traceId=01bd038fbcc14734] INFO  ... - [TRADING] [ORDER_SUBMITTED] userId=1 ticker=CAPX side=BUY type=MARKET size=1 price=1865.00 ARS
   2026-10-06 03:41:53.209 [http-nio-8080-exec-7] [traceId=01bd038fbcc14734] INFO  ... - [TRADING] [ORDER_FILLED] orderId=53 userId=1 ticker=CAPX side=BUY size=1 price=1865.00 ARS
   2026-10-06 03:41:53.214 [http-nio-8080-exec-7] [traceId=01bd038fbcc14734] INFO  ... - [HTTP] [RESPONSE] method=POST path=/api/v1/orders status=201 durationMs=21ms
   ```
   Las etiquetas `[HTTP]`, `[TRADING]`, `[PORTFOLIO]`, `[AUDIT]` y `[EXCEPTION]` permiten filtrado rápido en Grafana/Loki.
3. **Métricas en Grafana / Prometheus:** Scrapeo de Actuator exponiendo métricas de rendimiento JVM y contadores de negocio.

---

## Testing y Calidad

La suite cubre pruebas unitarias de dominio, calculadora contable, validaciones web y pruebas funcionales de integración con base de datos real:

```bash
mvn test
```

* **Total:** 67 tests automatizados ejecutados al 100% de éxito.
* **Colección Postman y HTTP Client:** En la carpeta [`postman/`](postman/) se incluye la colección `Cocos-Challenge-API.postman_collection.json` y el archivo ejecutable [`requests.http`](postman/requests.http) para pruebas directas.
