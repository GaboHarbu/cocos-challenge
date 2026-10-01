# Cocos Challenge - Backend API

API REST para operaciones de mercado y gestión de portafolios desarrollada en Java 21 y Spring Boot 3.

---

## Endpoints

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/v1/portfolio?userId={id}` | Valuación total de la cuenta, efectivo disponible y posiciones abiertas con rendimiento diario y total. |
| `GET` | `/api/v1/portfolio/{id}` | Mismo resultado que el anterior, accediendo por path variable. |
| `GET` | `/api/v1/instruments?query={q}` | Búsqueda paginada de instrumentos por ticker o nombre, con cotización de cierre y retorno diario. |
| `POST` | `/api/v1/orders` | Envío de órdenes (`MARKET` o `LIMIT`). Permite enviar `size` (acciones) o `amount` (pesos). Soporta `Idempotency-Key`. |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancelación de órdenes pendientes en estado `NEW`. |

---

## Reglas de Negocio Implementadas

### 1. Órdenes y Mercado
- **Tipos de orden:**
  - `MARKET`: Se ejecuta inmediatamente (`FILLED`) utilizando el último precio de cierre (`close`) de la tabla `marketdata`.
  - `LIMIT`: Se registra con estado inicial `NEW`. Requiere indicar `price` límite.
- **Cancelaciones:** Solo pueden cancelarse órdenes con estado `NEW`. Intentar cancelar una orden en otro estado (`FILLED`, `CANCELLED`, `REJECTED`) retorna error `422 Unprocessable Entity`.
- **Cálculo de acciones por monto (`amount`):** Cuando el usuario especifica un importe en pesos en lugar de cantidad de acciones, el sistema calcula el entero máximo: `floor(amount / price)` (sin fracciones). Si el monto no alcanza para comprar al menos 1 acción, la orden se guarda rechazada (`REJECTED`).
- **Validación de fondos y tenencias:**
  - En compras (`BUY`), se valida que el efectivo disponible (descontando órdenes de compra `NEW` pendientes) cubra el total.
  - En ventas (`SELL`), se valida que las acciones disponibles (descontando ventas `NEW` pendientes) cubran la cantidad a vender.
  - Si no alcanza el saldo o las acciones, la orden se persiste en base de datos con estado `REJECTED` y motivo descriptivo.

### 2. Moneda y Movimientos de Efectivo
- El efectivo (ARS) está modelado como instrumento de tipo `MONEDA`.
- Los ingresos y egresos de dinero se procesan como órdenes con lados `CASH_IN` y `CASH_OUT`, actualizando el disponible.
- Todos los cálculos monetarios se redondean a 2 decimales utilizando redondeo bancario (`RoundingMode.HALF_EVEN`).

### 3. Valuación de Cartera
- Las posiciones se computan a partir de las órdenes en estado `FILLED`.
- **Precio promedio de compra:** Se calcula como el promedio ponderado de las compras históricas. Las ventas reducen la cantidad de acciones en tenencia sin modificar dicho promedio.
- **Rendimientos:**
  - Rendimiento diario: `((close - previousClose) / previousClose) * 100`
  - Rendimiento total de la posición: `((marketPrice - averageCost) / averageCost) * 100`

### 4. Idempotencia y Concurrencia
- **Idempotencia:** El header opcional `Idempotency-Key` garantiza que reintentos de red con la misma clave devuelvan la orden original ya procesada sin duplicar movimientos.
- **Concurrencia:** Bloqueo pesimista (`SELECT ... FOR UPDATE`) sobre el usuario para serializar transacciones concurrentes y evitar sobregiros de saldo.

---

## Ejecución

### Opción A: Levantar todo con Docker (Recomendado)
El proyecto incluye un script bat para Windows que arranca la base de datos local, la API, el visor web de base de datos y la observabilidad:

```cmd
run-infra.bat
```

Servicios disponibles al iniciar:
- **API REST:** http://localhost:8080
- **Swagger / OpenAPI:** http://localhost:8080/swagger-ui.html
- **pgweb (Visor de Base de Datos):** http://localhost:8081
- **Grafana (Métricas y Logs):** http://localhost:3000 (usuario: `admin` / clave: `admin`)
- **Prometheus:** http://localhost:9090

Comandos adicionales del script:
```cmd
run-infra.bat infra     # Levanta solo infraestructura (DB, pgweb, Grafana, Loki) para correr la API desde el IDE
run-infra.bat db        # Levanta únicamente PostgreSQL y pgweb
run-infra.bat build     # Reconstruye las imágenes Docker
run-infra.bat stop      # Detiene los contenedores
```

### Opción B: Ejecutar API localmente
Para correr la aplicación desde la terminal o IDE:
```bash
# Con base de datos local (requiere PostgreSQL en localhost:5432 o via run-infra.bat infra):
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Con base remota NeonDB:
mvn spring-boot:run -Dspring-boot.run.profiles=neon
```

---

## Pruebas Automatizadas

El proyecto cuenta con 65 pruebas automatizadas (unitarias, funcionales con WebMvcTest e integración con Testcontainers PostgreSQL):

```bash
mvn test
```

---

## Observabilidad

El stack incluye monitoreo y agregación de logs centralizado, preconfigurado en Grafana (`http://localhost:3000`, credenciales `admin` / `admin`):

- **Métricas de Sistema (Prometheus + Actuator):** Monitoreo continuo de memoria JVM, tasas de peticiones HTTP y tiempos de respuesta.
- **Métricas de Negocio (PostgreSQL):** Paneles en tiempo real con total de órdenes, distribución por estado (`NEW`, `FILLED`, `REJECTED`, `CANCELLED`), usuarios e instrumentos.
- **Logs y Trazabilidad (Loki + Promtail):**
  - Cada solicitud HTTP genera un `traceId` único propagado en el MDC de SLF4J, incluido en los logs y en las respuestas de error (RFC 7807 `ProblemDetail`).
  - Los logs registran eventos con etiquetas estructuradas de negocio: `[HTTP]`, `[TRADING]`, `[PORTFOLIO]`, `[MARKET]`, `[AUDIT]`, `[EXCEPTION]`.
  - El dashboard de Grafana permite filtrar logs dinámicamente mediante el selector `Log Type` (órdenes ejecutadas, órdenes rechazadas, cancelaciones, excepciones, etc.).

---

## Ejemplos de Uso (cURL)

### 1. Consultar Cartera
```bash
curl -s -X GET "http://localhost:8080/api/v1/portfolio/1"
```

### 2. Buscar Activo
```bash
curl -s -X GET "http://localhost:8080/api/v1/instruments?query=GGAL"
```

### 3. Enviar Orden MARKET por cantidad
```bash
curl -s -X POST "http://localhost:8080/api/v1/orders" \
     -H "Content-Type: application/json" \
     -H "Idempotency-Key: 11111111-2222-3333-4444-555555555555" \
     -d '{"userId": 1, "ticker": "GGAL", "side": "BUY", "type": "MARKET", "size": 10}'
```

### 4. Enviar Orden MARKET por monto en pesos
```bash
curl -s -X POST "http://localhost:8080/api/v1/orders" \
     -H "Content-Type: application/json" \
     -d '{"userId": 1, "ticker": "GGAL", "side": "BUY", "type": "MARKET", "amount": 50000}'
```

### 5. Enviar Orden LIMIT
```bash
curl -s -X POST "http://localhost:8080/api/v1/orders" \
     -H "Content-Type: application/json" \
     -d '{"userId": 1, "ticker": "PAMP", "side": "BUY", "type": "LIMIT", "size": 5, "price": 900.00}'
```

### 6. Cancelar Orden NEW
```bash
curl -s -X POST "http://localhost:8080/api/v1/orders/{orderId}/cancel"
```

---

## Colección Postman y Requests HTTP

En el directorio `postman/` se encuentran disponibles para pruebas:
- `Cocos-Challenge-API.postman_collection.json`: Colección completa lista para importar en Postman.
- `requests.http`: Archivo para ejecutar peticiones directamente desde el editor (IntelliJ HTTP Client o extensión REST Client en VS Code).

