Catálogo Técnico de Validaciones y Reglas de Negocio - GestoPago

Este documento detalla la implementación técnica, restricciones a nivel de base de datos, anotaciones de Bean Validation y validadores personalizados para cada una de las reglas de negocio exigidas en el proyecto de Onboarding de Clientes Personas Físicas.

---

## 1. Validación de Datos Personales y Nombres
* **Regla de Negocio:** Los nombres y apellidos son obligatorios, deben contener exclusivamente letras y espacios, y una longitud de entre 2 y 50 caracteres.
* **Implementación en DTO (`ClienteCreateRequest.java`):**
  ```java
  @NotBlank(message = "El primer nombre es obligatorio")
  @Size(min = 2, max = 50, message = "El primer nombre debe tener entre 2 y 50 caracteres")
  @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El primer nombre solo debe contener letras y espacios")
  private String primerNombre;
  ```
  *(Aplicado de forma idéntica en `apellidoPaterno` y `apellidoMaterno`; opcional en `segundoNombre`).*

---

## 2. Validación y Restricción de CURP
* **Regla de Negocio:** Obligatoria, exactamente 18 caracteres, formato oficial mediante expresión regular y unicidad estricta en el sistema.
* **Implementación:**
  * **Bean Validation:** `@Size(min = 18, max = 18)` y `@Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]{2}$", message = "Formato de CURP inválido")`.
  * **Base de Datos & Lógica:** Restricción `CHECK (LENGTH(curp) = 18)`, índice único `UNIQUE` y excepción controlada `CurpDuplicadaException` que retorna `409 Conflict` si ya existe.

---

## 3. Validación y Restricción de RFC
* **Regla de Negocio:** Obligatorio, longitud de 12 o 13 caracteres, formato oficial y unicidad garantizada.
* **Implementación:**
  * **Bean Validation:** `@Pattern(regexp = "^[A-Z&Ñ]{3,4}\\d{6}[A-V1-9][A-Z0-9]{2}$", message = "Formato de RFC inválido")`.
  * **Base de Datos & Lógica:** Índice único `UNIQUE` y manejo de excepciones con `RfcDuplicadoException` (`409 Conflict`).

---

## 4. Validación de Correo Electrónico
* **Regla de Negocio:** Obligatorio, formato de correo electrónico válido, máximo 100 caracteres y único tanto en Clientes como en Usuarios de acceso.
* **Implementación:**
  * **Bean Validation:** `@NotBlank`, `@Email(message = "El correo debe tener un formato válido")`, `@Size(max = 100)`.
  * **Lógica:** Excepción `CorreoDuplicadoException` (`409 Conflict`) ante duplicados en base de datos.

---

## 5. Validación de Fecha de Nacimiento y Mayoría de Edad
* **Regla de Negocio:** La fecha de nacimiento no puede ser futura y el cliente debe ser mayor de edad (18 años cumplidos o más a la fecha actual).
* **Implementación (Validador Personalizado `@MayorDeEdad`):**
  ```java
  @Target({ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @Constraint(validatedBy = MayorDeEdadValidator.class)
  public @interface MayorDeEdad {
      String message() default "El cliente debe ser mayor de edad (18 años o más)";
      Class<?>[] groups() default {};
      Class<? extends Payload>[] payload() default {};
  }
  ```
  *La lógica comprueba que `fechaNacimiento.plusYears(18).isBefore(LocalDate.now())` o sea igual al día de hoy.*

---

## 6. Validación de Teléfono Móvil y Código Postal
* **Regla de Negocio:** El teléfono móvil debe contener exactamente 10 dígitos numéricos y el código postal exactamente 5 dígitos.
* **Implementación:**
  * Teléfono: `@Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos")`.
  * Código Postal: `@Pattern(regexp = "^\\d{5}$", message = "El código postal debe tener exactamente 5 dígitos")`.
  * Base de Datos: Tipos `CHAR(10)` y `CHAR(5)` con restricciones `CHECK`.

---

## 7. Validación de Ingreso Mensual y Saldo de Cuenta
* **Regla de Negocio:** El ingreso mensual reportado por el cliente debe ser mayor a cero. El saldo inicial de la cuenta no puede ser negativo.
* **Implementación:**
  * Ingreso Mensual: `@DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")`.
  * Base de Datos: `NUMERIC(12,2) CHECK (ingreso_mensual > 0)` y `NUMERIC(14,2) CHECK (saldo >= 0)`.

---

## 8. Blindaje contra Campos No Permitidos y No Modificables
* **Regla de Negocio:** 
  1. El saldo inicial de la cuenta no puede ser manipulado por el usuario en el registro; se asigna exclusivamente de forma centralizada por el sistema (`onboarding.cuenta.saldo-inicial`).
  2. En la actualización de clientes (`PUT /clientes/{id}`), los datos fiscales clave como CURP, RFC y número de cuenta son totalmente inmutables.
* **Implementación:**
  * Uso de `@JsonAnySetter` en los DTOs para interceptar y rechazar con código HTTP `400 Bad Request` cualquier intento de inyección de campos no reconocidos (como `saldoInicial` en el registro o `curp`/`rfc` en la actualización).

---

## 9. Complejidad de Contraseña y Cifrado BCrypt
* **Regla de Negocio:** La contraseña del usuario de acceso debe contener al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un carácter especial. No debe almacenarse en texto plano.
* **Implementación:**
  * Validador personalizado `@PasswordSegura` acoplado al registro de usuarios.
  * Cifrado de alto rendimiento mediante `BCryptPasswordEncoder` (generando hashes de longitud fija `CHAR(60)` en base de datos).

---

## 10. Baja Lógica en Cascada y Seguridad Dinámica por JWT
* **Regla de Negocio:** Al dar de baja a un cliente, la operación debe ser transaccional (baja lógica), desactivando simultáneamente sus cuentas y su usuario de acceso. Los tokens JWT emitidos previamente para un usuario inactivo deben invalidarse de inmediato.
* **Implementación:**
  * Método transaccional `@Transactional` en `ClienteServiceImpl` que actualiza estatus y marca `fecha_baja`.
  * El filtro `JwtAuthenticationFilter` consulta activamente la base de datos en cada petición protegida para verificar que el usuario asociado siga activo, bloqueando de inmediato el acceso con código `403 Forbidden` si fue dado de baja.