# Integración con Servicio GestoPago

## Descripción
Este módulo contiene la integración con la plataforma de servicios externos **GestoPago** (API REST `GET /sistema/service/getProductList.do` autenticada mediante Bearer Token). Su objetivo es consultar el catálogo de productos y servicios disponibles, ofreciendo deserialización segura de XML, gestión automatizada de tokens y manejo estandarizado de excepciones.

## Arquitectura
La integración está organizada en una arquitectura por capas:

- **Controller (`com.proyecto.servicios.controller`)**:
  - `GestoPagoProductoController`: Expone el endpoint HTTP `GET /gestopago/productos` retornando la lista de productos en formato JSON (`MediaType.APPLICATION_JSON_VALUE`).
- **Service (`com.proyecto.servicios.service` / `Impl`)**:
  - `GestoPagoProductService` / `GestoPagoProductServiceImpl`: Orquesta el consumo del cliente Feign, mide el tiempo de respuesta, parsea la respuesta XML de forma segura y maneja errores de comunicación y timeout.
  - `GestoPagoTokenService` / `GestoPagoTokenServiceImpl`: Administra los Bearer Tokens, incluyendo su obtención desde base de datos y la renovación periódica programada.
- **Client Feign (`com.proyecto.servicios.client`)**:
  - `GestoPagoProductClient`: Cliente HTTP declarativo OpenFeign para obtener el catálogo de productos XML.
  - `GestoPagoAuthClient`: Cliente HTTP declarativo para autenticación y obtención de tokens.
- **Configuración (`com.proyecto.servicios.config`)**:
  - `GestoPagoFeignConfig`: Configuración de timeouts de conexión y lectura para el cliente Feign.
- **Modelo DTO (`com.proyecto.servicios.model.gestopago`)**:
  - `GestoPagoProductListResponse`, `GestoPagoProductoItem`, `GestoPagoMensajeResponse`, `GestoPagoAuthResponse`: Modelos anotados con JAXB (`jakarta.xml.bind`) para mapear las respuestas de GestoPago.
- **Persistencia (`com.proyecto.servicios.entity.gestopago` / `repositorys.gestopago`)**:
  - `GestoPagoToken`, `GestoPagoTokenRepository`: Entidad y repositorio JPA para consultar y mantener actualizado el token de acceso en base de datos.
- **Manejo de Excepciones (`com.proyecto.servicios.exception`)**:
  - `ExternalIntegrationException`: Excepción personalizada para fallos en integraciones externas.
  - `GlobalExceptionHandler`: Controlador global que traduce excepciones en respuestas JSON con estructura `GenericResponse` y código de estado HTTP adecuado.

## Configuración
La configuración se gestiona en `src/main/resources/application.properties`:

- `gestopago.service.url`: URL base del servicio de productos GestoPago (`https://gestopago.portalventas.net`).
- `gestopago.auth.url`: URL base del servicio de autenticación.
- `gestopago.auth.id-distribuidor`: Identificador del distribuidor asignado (ej. `83`).
- `gestopago.auth.codigo-dispositivo`: Identificador del dispositivo TPV (ej. `GPS83-TPV-17`).
- `gestopago.auth.password`: Contraseña de autenticación expuesta mediante la variable de entorno `GESTOPAGO_PASSWORD` (`${GESTOPAGO_PASSWORD:}`).
- `gestopago.auth.bearer-token`: Token de respaldo opcional.
- `gestopago.auth.refresh-rate-ms`: Intervalo de refresco programado para la renovación del token (por defecto `3600000` ms / 1 hora).
- `gestopago.service.connect-timeout-ms` y `gestopago.service.read-timeout-ms`: Timeouts de conexión (5000 ms) y lectura (10000 ms).

## Decisiones técnicas
- **Spring Cloud OpenFeign**: Facilita la invocación declarativa de servicios HTTP externos sin duplicar código de cliente HTTP.
- **Renovación programada del token (`@Scheduled`)**: `GestoPagoTokenServiceImpl` ejecuta una tarea periódica anotada con `@Scheduled` para mantener un token válido en base de datos sin requerir autenticación en cada petición individual.
- **JAXB para XML y JSON en el controller**: Permite consumir la API XML de GestoPago mediante JAXB (`jakarta.xml.bind`), mientras que el controller serializa el resultado hacia los clientes consumidores en formato JSON estándar.
- **Excepción propia y `GlobalExceptionHandler`**: Se utiliza `ExternalIntegrationException` para capturar errores de integración y mapear los estados de HTTP (ej. 401 Unauthorized, 403 Forbidden, 504 Gateway Timeout, 502 Bad Gateway, 500 Internal Server Error) en un formato `GenericResponse` unificado.
- **Timeouts estandarizados**: Se configuran timeouts explícitamente en `GestoPagoFeignConfig`. En el servicio se captura `feign.RetryableException` antes de `FeignException` para traducir los timeouts en HTTP 504 (`GATEWAY_TIMEOUT`) con el mensaje `"Tiempo de espera agotado con el servicio externo"`.
- **Prevención de vulnerabilidades XXE (XML External Entity)**: La deserialización XML utiliza `XMLInputFactory` configurado con `SUPPORT_DTD=false` e `IS_SUPPORTING_EXTERNAL_ENTITIES=false`, pasando un `XMLStreamReader` seguro al `Unmarshaller` de JAXB.
- **Logs de trazabilidad sin datos sensibles**: `GestoPagoProductServiceImpl` registra el inicio y fin de la invocación con la medición del tiempo total en milisegundos (`ms`), omitiendo deliberadamente tokens, headers y credenciales.

## Pruebas
Las pruebas unitarias implementadas en `GestoPagoProductServiceImplTest` cubren los siguientes escenarios:
- Consulta exitosa de productos con token activo y parseo XML correcto.
- Manejo de token expirado o no autorizado (`FeignException.Forbidden` -> HTTP 403 FORBIDDEN).
- Ausencia de token disponible en BD o propiedades (HTTP 401 UNAUTHORIZED).
- Error 401 devuelto por el cliente Feign (`FeignException.Unauthorized` -> HTTP 401 UNAUTHORIZED).
- Timeout en comunicación externa (`feign.RetryableException` -> HTTP 504 GATEWAY_TIMEOUT).
- Respuesta XML vacía (`HTTP 502 BAD_GATEWAY`).
- Respuesta XML malformada o corrupta (`JAXBException` / `XMLStreamException` -> HTTP 500 INTERNAL_SERVER_ERROR).
- Errores HTTP genéricos en Feign (`FeignException` -> HTTP status original / 502 BAD_GATEWAY).

Para ejecutar el conjunto completo de pruebas del proyecto:
```bash
./gradlew test
```

## Flujo de ramas
El desarrollo sigue la estrategia GitFlow:
`feature/gestopago-integracion` -> `develop` -> `main` mediante la creación de un Pull Request (PR) validado y aprobado.