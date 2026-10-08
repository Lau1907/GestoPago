# Evidencias de Ejecución y Pruebas del Sistema

## 1. Resumen de Ejecución de Pruebas Automatizadas (`./gradlew test`)

- **Resultado de la Suite:** `BUILD SUCCESSFUL`
- **Total de Pruebas Ejecutadas:** 63 pruebas
- **Pruebas Exitosas:** 63
- **Pruebas Fallidas:** 0
- **Pruebas Omitidas:** 0
- **Cobertura de Componentes:**
  - Pruebas Unitarias de Servicios (Cliente, Cuenta, Usuario, Autenticación).
  - Pruebas Unitarias de Validadores Personalizados (`@MayorDeEdad`, `@PasswordSegura`, CURP, RFC, Teléfono, CP, Nombres).
  - Pruebas Unitarias de Controladores (`MockMvc` standalone con `GlobalExceptionHandler`).
  - Pruebas de Integración End-to-End (`@SpringBootTest` con base de datos H2 en memoria y Flyway V2).
  - Pruebas de Integración de Rechazo de Campos Desconocidos y No Modificables (`ClienteCamposDesconocidosIntegrationTest`).

---

## 2. Ejemplos de Petición y Respuesta de Endpoints (API REST)

| Método | Endpoint | Descripción | Body de Petición (Ejemplo) | Código HTTP | Respuesta (Ejemplo) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/clientes` | Registro de Cliente, Domicilio, Cuenta y Usuario | `{"primerNombre":"Juan","apellidoPaterno":"Perez","apellidoMaterno":"Lopez","curp":"HEGG560427MVZRRL04","rfc":"HEGG560427AB1","correo":"juan.perez@example.com","fechaNacimiento":"1990-05-15","telefonoMovil":"9981234567","sexo":"H","estadoCivil":"S","nacionalidad":"Mexicana","ocupacion":"Ingeniero","empresa":"Tech Corp","ingresoMensual":25000.00,"domicilio":{"calle":"Av. Hidalgo","numeroExterior":"123","colonia":"Centro","municipio":"Cancun","estado":"Quintana Roo","pais":"Mexico","codigoPostal":"77500"},"password":"[OCULTA_POR_SEGURIDAD]"}` | `201 Created` | `{"id":1,"primerNombre":"Juan","segundoNombre":null,"apellidoPaterno":"Perez","apellidoMaterno":"Lopez","curp":"HEGG560427MVZRRL04","rfc":"HEGG560427AB1","correo":"juan.perez@example.com","fechaNacimiento":"1990-05-15","telefonoMovil":"9981234567","sexo":"H","estadoCivil":"S","nacionalidad":"Mexicana","ocupacion":"Ingeniero","empresa":"Tech Corp","ingresoMensual":25000.00,"activo":true,"domicilio":{"id":1,"calle":"Av. Hidalgo","numeroExterior":"123","colonia":"Centro","municipio":"Cancun","estado":"Quintana Roo","pais":"Mexico","codigoPostal":"77500"},"cuentas":[{"id":1,"clienteId":1,"numeroCuenta":"8493021948","saldo":0.00,"estatus":"ACTIVA"}],"usuarioId":1}` |
| **POST** | `/clientes` | Intento de registro enviando `saldoInicial` no permitido | `{"primerNombre":"Juan",...,"saldoInicial":500.00}` | `400 Bad Request` | `{"codigo":400,"mensaje":"Propiedad no reconocida o no permitida: 'saldoInicial'"}` |
| **POST** | `/auth/login` | Autenticación y generación de JWT | `{"correo":"juan.perez@example.com","password":"[OCULTA_POR_SEGURIDAD]"}` | `200 OK` | `{"token":"eyJhbGciOiJIUzI1...","tipo":"Bearer","expiraEn":3600000}` |
| **GET** | `/clientes/1` | Consulta de cliente por ID (con token Bearer) | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"primerNombre":"Juan","apellidoPaterno":"Perez","curp":"HEGG560427MVZRRL04","correo":"juan.perez@example.com","activo":true}` |
| **GET** | `/clientes/curp/HEGG560427MVZRRL04` | Consulta de cliente por CURP | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"primerNombre":"Juan","curp":"HEGG560427MVZRRL04"}` |
| **GET** | `/clientes/rfc/HEGG560427AB1` | Consulta de cliente por RFC | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"primerNombre":"Juan","rfc":"HEGG560427AB1"}` |
| **GET** | `/clientes/cuenta/8493021948` | Consulta de cliente por número de cuenta | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"primerNombre":"Juan","cuentas":[{"numeroCuenta":"8493021948"}]}` |
| **GET** | `/clientes/correo?correo=juan.perez@example.com` | Consulta de cliente por correo | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"primerNombre":"Juan","correo":"juan.perez@example.com"}` |
| **GET** | `/clientes?activo=true` | Consulta de todos los clientes activos | *N/A (Header Authorization)* | `200 OK` | `[{"id":1,"primerNombre":"Juan","activo":true}]` |
| **PUT** | `/clientes/1` | Actualización de datos personales y contacto | `{"primerNombre":"Juan Carlos","apellidoPaterno":"Perez","apellidoMaterno":"Lopez","correo":"juan.perez@example.com","fechaNacimiento":"1990-05-15","telefonoMovil":"9987654321","sexo":"H","estadoCivil":"C","nacionalidad":"Mexicana","ocupacion":"Director","empresa":"Tech Corp","ingresoMensual":35000.00,"domicilio":{"calle":"Av. Tulum","numeroExterior":"45","colonia":"Centro","municipio":"Cancun","estado":"Quintana Roo","pais":"Mexico","codigoPostal":"77500"}}` | `200 OK` | `{"id":1,"primerNombre":"Juan Carlos","telefonoMovil":"9987654321","ingresoMensual":35000.00}` |
| **PUT** | `/clientes/1` | Intento de modificar campo no modificable `curp` | `{"primerNombre":"Juan",...,"curp":"HEGG560427MVZRRL09"}` | `400 Bad Request` | `{"codigo":400,"mensaje":"El campo 'curp' no es modificable"}` |
| **PUT** | `/clientes/1` | Intento de modificar campo no modificable `rfc` | `{"primerNombre":"Juan",...,"rfc":"HEGG560427AB9"}` | `400 Bad Request` | `{"codigo":400,"mensaje":"El campo 'rfc' no es modificable"}` |
| **GET** | `/cuentas/8493021948` | Consulta de cuenta por número | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"clienteId":1,"numeroCuenta":"8493021948","saldo":1000.00,"estatus":"ACTIVA"}` |
| **GET** | `/cuentas/8493021948/saldo` | Consulta de saldo de cuenta | *N/A (Header Authorization)* | `200 OK` | `{"numeroCuenta":"8493021948","saldo":1000.00}` |
| **GET** | `/cuentas?estatus=ACTIVA` | Lista de cuentas activas | *N/A (Header Authorization)* | `200 OK` | `[{"numeroCuenta":"8493021948","saldo":1000.00,"estatus":"ACTIVA"}]` |
| **GET** | `/usuarios/1` | Consulta de usuario por ID (propio usuario) | *N/A (Header Authorization)* | `200 OK` | `{"id":1,"clienteId":1,"correo":"juan.perez@example.com","activo":true}` |
| **PUT** | `/usuarios/1/password` | Cambio de contraseña del propio usuario | `{"passwordActual":"[OCULTA]","passwordNueva":"[OCULTA]"}` | `204 No Content` | *Sin contenido* |
| **DELETE** | `/clientes/1` | Baja lógica de cliente | *N/A (Header Authorization)* | `204 No Content` | *Sin contenido* |
| **GET** | `/clientes/1` | Intento de consulta sin token JWT | *N/A (Sin Header)* | `401 Unauthorized` | `{"codigo":401,"mensaje":"No autorizado: se requiere un token JWT válido para acceder a este recurso"}` |
| **POST** | `/auth/login` | Intento de login de usuario dado de baja | `{"correo":"juan.perez@example.com","password":"[OCULTA]"}` | `403 Forbidden` | `{"codigo":403,"mensaje":"El usuario se encuentra inactivo"}` |
