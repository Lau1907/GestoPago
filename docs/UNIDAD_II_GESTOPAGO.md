# Documentación Técnica — Ecosistema GestoPago (Unidad II)

**Proyecto:** GestoPago (`proyecto-servicio-empresa` + Web App + Móvil Flutter)  
**Asignatura / Evaluación:** Desarrollo y Gestión de Software — Unidad II  
**Fecha:** 21 de noviembre de 2026  

---

## 1. Resumen Ejecutivo

**GestoPago** es una plataforma integral de consulta de catálogo, pago de servicios y consulta de historial transaccional que conecta dos canales clientes (**Aplicación Web** en JavaScript y **Aplicación Móvil** en Flutter) con un backend centralizado en **Spring Boot** (`http://localhost:8080`), persistencia en **H2** e integración con el servicio externo **GestoPago** (con soporte de contingencia mediante modo *mock* configurable).

El sistema garantiza autenticación en dos pasos (**2FA OTP**), emisión de tokens **JWT**, **control de sesión única** por usuario/dispositivo e **idempotencia** en operaciones de pago.

---

## 2. Arquitectura General

La solución sigue una arquitectura cliente-servidor desacoplada donde el backend en Spring Boot orquesta la seguridad, las reglas de negocio, la persistencia y la comunicación con el proveedor externo:

* **Cliente Web (`web_app/`):** Aplicación web desarrollada con HTML/CSS/JavaScript (`config.js`, `api.js`, `state.js`, `auth.js`, `app.js`, `catalogo.js`, `pago.js`, `historial.js`), servida en desarrollo (por ejemplo en `http://localhost:3000`) y configurada para consumir el backend en `http://localhost:8080`.
* **Cliente Móvil (`mobile_app/`):** Aplicación en Flutter conectada al backend REST para autenticación con 2FA, consulta de cuentas/catálogo, ejecución de pagos, sincronización de historial y reacción ante invalidación de sesión (`401`).
* **Backend (`proyecto-servicio-empresa` - Spring Boot):** Expone endpoints REST, aplica filtros de seguridad JWT y validación de sesión activa, gestiona transacciones con bloqueo pesimista e idempotencia, y consume el servicio externo GestoPago mediante clientes **OpenFeign** con deserialización **JAXB (XML)**.
* **Base de Datos (H2):** Almacena usuarios, sesiones (`MOBILE` / `WEB`), cuentas, clientes, transacciones de pago y tokens/estado operativo en archivo local (`./data/gestopago_db`), con consola disponible en `/h2-console` y documentación interactiva en `/swagger-ui.html`.
* **Servicio Externo GestoPago:** Proveedor externo de catálogo de productos y procesamiento de transacciones (`sendTx`), respaldado por un modo *mock* (`gestopago.mock.enabled`) ante fallos de red, *timeouts* o respuestas `401`/`403`.

---

## 3. Seguridad, Sesiones e Idempotencia

### 3.1 Autenticación JWT y 2FA OTP
1. El cliente envía credenciales y canal (`"WEB"` o `"MOBILE"`) a `POST /auth/login`.
2. Si se requiere segundo factor, el backend emite un token temporal y el código OTP de 6 dígitos (`codigoOtp` en entorno de pruebas) para validarlo en `POST /auth/2fa/verificar`.
3. Tras validar el OTP, el backend entrega el token **JWT** definitivo que viaja en el encabezado `Authorization: Bearer <token>` en las peticiones protegidas.

### 3.2 Control de Sesión Única (`MOBILE` vs `WEB`)
* El backend registra y valida el estado de la sesión asociada al usuario y canal/dispositivo.
* Cuando el usuario inicia sesión en un canal (por ejemplo en `MOBILE`), las sesiones previas en el otro canal (por ejemplo `WEB`) pasan a estado inactivo (motivos como `SESION_CERRADA_POR_NUEVO_LOGIN_EN_WEB` o cierre del canal anterior).
* Si se intenta operar o recargar con un token invalidado, el filtro JWT responde **HTTP 401 (`Unauthorized`)**.
* En la Web (`api.js`, `auth.js`, `app.js`), el interceptor de `401` notifica a `ApiClient.onSessionInvalidated` / `Auth.mostrarModalSesionInvalida(...)`, limpia el estado de sesión, muestra el aviso correspondiente y devuelve la vista a `#auth-views` (Login) evitando pantallas en blanco o *race conditions* tras actualizar el token en `Auth.actualizarToken(token)` y cargar datos en `Auth.cargarDatosPostLogin()`.

### 3.3 Idempotencia y Bloqueo Transaccional
* Cada pago enviado a `POST /pagos/servicio` se valida mediante `idempotencyKey` (`TransaccionPagoRepository.findByIdempotencyKey`) para evitar cargos duplicados ante reintentos.
* La cuenta afectada se bloquea de forma pesimista (`findByNumeroCuentaForUpdate`) antes de validar propiedad, estatus, saldo, reglas de precio y dígito verificador.

---

## 4. Integración con el Servicio Externo GestoPago

### 4.1 Configuración (`application.properties` y `GestoPagoFeignConfig.java`)

| Propiedad / Configuración | Ubicación | Propósito |
| :--- | :--- | :--- |
| `gestopago.service.url` | `src/main/resources/application.properties` | URL base utilizada por los clientes Feign (`GestoPagoProductClient`, `GestoPagoPaymentClient`, etc.). |
| `gestopago.auth.url` | `src/main/resources/application.properties` | Endpoint de autenticación externa para obtener token JWT. |
| `gestopago.auth.id-distribuidor` | `src/main/resources/application.properties` | Identificador de distribuidor enviado en autenticación. |
| `gestopago.auth.codigo-dispositivo` | `src/main/resources/application.properties` | Código de dispositivo asociado a la autenticación externa. |
| `gestopago.auth.password` | `src/main/resources/application.properties` | Credencial/parámetro de autenticación del servicio externo. |
| `gestopago.auth.bearer-token` | `src/main/resources/application.properties` | Token de respaldo (*sandbox*) cuando no existe token vigente en base de datos. |
| `gestopago.auth.refresh-rate-ms` | `src/main/resources/application.properties` | Intervalo de renovación automática de token (`82800000` ms = 23 horas). |
| `gestopago.mock.enabled` | `src/main/resources/application.properties` | Activa (`true`) o desactiva (`false`) el *fallback* simulado ante errores externos. |
| `gestopago.service.connect-timeout-ms` / `read-timeout-ms` | `GestoPagoFeignConfig.java` | Configura `Request.Options` (*timeouts* de conexión y lectura) para Feign. |

### 4.2 Flujo de Consulta de Catálogo (`GestoPagoProductServiceImpl`)
1. Resuelve el token Bearer mediante `resolverBearerToken()` (BD, *fallback* o *mock*).
2. Invoca `gestopagoProductClient.getProductList(...)` con `Authorization: Bearer <token>`.
3. Deserializa el XML de respuesta con **JAXB** hacia `GestoPagoProductListResponse`.
4. Si ocurre `RetryableException` (*timeout*), `401`, `403`, `FeignException` u otra excepción:
   * Con `gestopago.mock.enabled=true`, devuelve catálogo simulado (`generarXmlCatalogoSimulado()`).
   * Con `gestopago.mock.enabled=false`, lanza `ExternalIntegrationException`.

### 4.3 Flujo de Envío de Transacción (`PagoServicioServiceImpl` — `sendTx`)
1. Verifica `idempotencyKey` y aplica bloqueo pesimista sobre la cuenta (`findByNumeroCuentaForUpdate`).
2. Ejecuta validaciones de negocio, consulta el catálogo y registra la transacción en estado `INICIADA`.
3. Resuelve el token Bearer y ejecuta `invocarSendTx` vía `GestoPagoPaymentClient`.
4. **Manejo de fallos y *fallback*:**
   * Si `gestopago.mock.enabled=true` ante *timeout*, `401`, `403` o excepción Feign/genérica, genera XML simulado exitoso con `<FOLIO>MOCK-...</FOLIO>`, `<AUTORIZACION>AUTH-...</AUTORIZACION>` y `<TICKET>RECIBO-MOCK-...</TICKET>`.
   * Si `gestopago.mock.enabled=false` y ocurre *timeout* (`RetryableException`), marca la transacción como `TIMEOUT_REVISION` y responde `504 GATEWAY_TIMEOUT`.
5. Al deserializar el XML (`GestoPagoTxResponse`):
   * Código `"01"`: debita el saldo de la cuenta, guarda folio/autorización/ticket y marca la transacción como `EXITOSA`.
   * Código distinto de `"01"`: marca `FALLIDA` y lanza `ExternalIntegrationException` (`400`).

---

## 5. Endpoints REST Principales y Códigos HTTP

| Método | Ruta | Controlador / Módulo | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | `AuthController` | Inicia sesión (`WEB` / `MOBILE`) y devuelve token o requerimiento 2FA con `tokenTemporal` y `codigoOtp`. |
| `POST` | `/auth/2fa/verificar` | `AuthController` | Valida el código OTP de 6 dígitos y emite el token JWT definitivo. |
| `GET` / `POST` | `/cuentas` o `/cuentas/buscar` | `CuentaController` | Consulta las cuentas asociadas al usuario autenticado. |
| `POST` | `/clientes/consultar` | `ClienteController` | Consulta los datos del cliente autenticado. |
| `GET` | `/catalogo` | Catálogo / `GestoPagoProductService` | Obtiene el listado de servicios/productos disponibles (o simulados vía *fallback*). |
| `POST` | `/pagos/servicio` | `PagoServicioController` | Procesa el pago idempotente de un servicio y devuelve folio, autorización, ticket y saldo actualizado. |
| `POST` | `/pagos/consultar` | `PagoServicioController` | Consulta el estado de una transacción por `idempotencyKey`. |
| `GET` | `/historial` | `PagoServicioController` | Obtiene el historial de transacciones sincronizado. |
| `GET` | `/swagger-ui.html` | OpenAPI | Documentación interactiva de endpoints REST. |
| `GET` | `/h2-console` | H2 Console | Consola web de inspección de la base de datos H2 en desarrollo. |

### Ejemplos de Payload y Respuesta

Login (`POST /auth/login`):
```json
{
  "correo": "usuario@correo.com",
  "password": "********",
  "dispositivo": "WEB"
}
```

Verificación 2FA (`POST /auth/2fa/verificar`):
```json
{
  "correo": "usuario@correo.com",
  "codigo": "123456",
  "dispositivo": "WEB",
  "tokenTemporal": "<token-temporal>"
}
```

**Códigos de estado HTTP relevantes:**
* `200 OK`: Operación completada correctamente.
* `400 Bad Request`: Datos inválidos, reglas de negocio no cumplidas o respuesta de rechazo (`!= "01"`) del proveedor.
* `401 Unauthorized`: Token ausente/inválido o sesión cerrada por control de sesión única.
* `403 Forbidden`: Acceso denegado por permisos/rol o rechazo del servicio externo.
* `500 Internal Server Error` / `504 Gateway Timeout`: Error interno o *timeout* (`TIMEOUT_REVISION`) al comunicarse con el servicio externo sin *mock*.

---

## 6. Verificación de Pruebas Realizadas

1. **Autenticación + 2FA en Web y Móvil:** Validación de credenciales y código OTP de 6 dígitos con emisión de token JWT y carga de cuenta, saldo y catálogo.
2. **Pago y sincronización de historial:** Ejecución de pagos de servicios (como *CFE SUMINISTRADOR DE SERVICIOS BASICOS* y *Agua Cancun*), descuento de saldo en cuenta y consulta de historial persistido en H2.
3. **Control de sesión única (`401`):** Al iniciar sesión en un segundo canal (por ejemplo `MOBILE` estando activa `WEB`), el backend invalida la sesión anterior y al recargar u operar en el canal invalidado se recibe `401`, se muestra el aviso/modal y se regresa al Login.
4. **Persistencia:** Verificación de que los registros en H2 (`./data/gestopago_db`) se mantienen tras reiniciar el backend (`bootRun`).