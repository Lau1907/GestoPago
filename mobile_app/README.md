# Aplicación Móvil Bancaria Flutter - GestoPago & Sesión Única (Unidad II)

Esta aplicación móvil está desarrollada con **Flutter 3.x / Dart**, diseñada con **Material Design 3**, arquitectura limpia por capas y preparada para conectarse con el backend de **Spring Boot**.

---

## 🚀 Requerimientos y Mejoras Implementadas

### 1. Corrección de Overflows y Adaptabilidad de Pantalla
- **Corrección de Overflow Horizontal (`RIGHT OVERFLOWED BY 36 PIXELS`)**: Se optimizó la modal/diálogo de confirmación de compra (`ConfirmacionPagoDialog`) envolviendo los valores dinámicos largos (nombre de producto, servicio, referencia bancaria y clave de idempotencia) con `Expanded`, `maxLines: 2`, `overflow: TextOverflow.ellipsis` y alineación `textAlign: TextAlign.end`, delimitando la etiqueta con ancho predecible y `crossAxisAlignment: CrossAxisAlignment.start`.
- **Botones Adaptables**: En `CustomButton` se añadió la propiedad `isFullWidth: false` con wrap y `actionsOverflowButtonSpacing: 8` en diálogos para garantizar que en teléfonos angostos o con letras ampliadas los botones se acomoden fluidamente sin salirse de la pantalla.
- **Cabeceras y Tarjetas Responsivas**: En `MainNavigationScreen` y `HistorialScreen`, las filas de datos ahora tienen protección de desbordamiento horizontal con `Expanded` en las columnas de texto.

### 2. Manejo Explícito de los 4 Estados de Compra
La aplicación procesa y visualiza de forma transparente las respuestas reales del backend de Spring Boot sin simular datos:
1. **Compra Exitosa**:
   - HTTP 200 con `estado == "EXITOSA"`.
   - Se registra en el historial local persistente y se navega a la pantalla de comprobante (`ComprobantePagoScreen`), visualizando Folio GestoPago, No. de Autorización, Ticket, saldo anterior y saldo nuevo debitado.
2. **Compra Rechazada**:
   - HTTP 400/404/409/422 (saldo insuficiente, cuenta inactiva, regla de negocio inválida) o HTTP 200 con `estado == "FALLIDA"`.
   - Se despliega tarjeta roja estilizada con icono `Icons.cancel_outlined`, título **Compra Rechazada** y el mensaje de negocio exacto emitido por el servidor (ej. *"Saldo insuficiente en cuenta: Saldo actual $X, Monto requerido $Y"*), con botón para reintentar la compra.
3. **Compra Pendiente**:
   - HTTP 504 Gateway Timeout o respuesta con `estado == "TIMEOUT_REVISION"` o `"INICIADA"`.
   - Se despliega tarjeta ámbar con icono `Icons.pending_actions_outlined`, título **Compra en Revisión / Pendiente**, mensaje de protección contra cobros duplicados y botón activo para **Reconsultar Estado en Servidor** consumiendo `POST /pagos/consultar`.
4. **Error de Comunicación**:
   - `SocketException`, `TimeoutException` de red o servidor backend apagado.
   - Se despliega tarjeta informativa con icono `Icons.wifi_off_outlined`, título **Error de Comunicación**, indicando revisar la conectividad o la URL configurada, con opción de reintentar la conexión.

### 3. Sesión Única ("MOBILE") y Seguridad Estricta
- **Detección Automática de Sesión Inválida (HTTP 401)**:
  - Cuando el usuario inicia sesión desde otro dispositivo (ej. desde la plataforma `"WEB"`), el backend de Spring Boot invalida la sesión anterior y responde HTTP 401.
  - El cliente HTTP centralizado (`ApiClient`) detecta el código 401, elimina de inmediato todas las credenciales de `FlutterSecureStorage` (`await _storage.clearAll()`), cierra cualquier modal o pantalla secundaria apilada (`navigatorKey.currentState?.popUntil((route) => route.isFirst)`) y redirige limpiamente a la pantalla de Login mostrando un diálogo modal explicativo de sesión cerrada.
- **Cierre de Sesión Seguro y Manual**:
  - Desde el menú de Perfil / Configuración, al pulsar *"Cerrar Sesión Segura"* se borran todos los tokens del almacenamiento cifrado (`FlutterSecureStorage`), se resetea el estado en memoria y se limpia el historial.

---

## 📱 Guía para Probar en Dispositivo Físico o Emulador

Para conectar la aplicación en un dispositivo físico con tu backend local en ejecución en `http://127.0.0.1:8080`:

### Opción A: Dispositivo Físico conectado por Cable USB (Recomendada)
1. Conecta tu teléfono mediante cable USB y habilita la **Depuración por USB**.
2. Ejecuta en tu terminal el comando de redirección de puertos de Android ADB:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
3. En la app móvil, abre el selector de URL (icono de tuerca en el Login o en Perfil) y selecciona:
   **`http://127.0.0.1:8080`** (o `http://localhost:8080`).
4. El teléfono redirigirá las peticiones a través del cable USB directamente a tu backend de Spring Boot.

### Opción B: Dispositivo Físico en la misma Red Wi-Fi
1. Averigua la dirección IP local de tu computadora ejecutando en PowerShell:
   ```powershell
   ipconfig
   ```
   *(Busca tu Adaptador Wi-Fi, por ejemplo: `192.168.1.85`)*.
2. Asegúrate de que el firewall de Windows permita conexiones entrantes al puerto 8080.
3. En la app móvil, ingresa en el selector de URL:
   **`http://192.168.X.X:8080`** (reemplazando por tu IP real).

### Opción C: Emulador de Android Studio
- Utiliza la URL por defecto: **`http://10.0.2.2:8080`** (alias automático del emulador hacia el localhost de tu PC).

---

## 🧪 Casos de Prueba Paso a Paso

### Prueba 1: Comprobación del Diálogo de Compra sin Overflow
1. Inicia sesión con:
   - **Correo:** `juan.perez@example.com`
   - **Contraseña:** `Password123!`
2. En la pantalla 2FA ingresa el código OTP de 6 dígitos mostrado.
3. En el catálogo, presiona **Pagar** en un servicio con nombre largo (ej. *AGUAKAN (Cancun) - Agua Cancun*).
4. Ingresa una referencia de prueba (ej. `9981234567`) y presiona **Continuar a Confirmación**.
5. **Verificación:** Observa la modal *Confirmar Compra*. Comprueba que no hay ninguna franja amarilla/negra ni error de overflow en consola; las etiquetas y valores se alinean y envuelven limpiamente.

### Prueba 2: Comprobación de Compra Rechazada (Saldo Insuficiente o Regla de Negocio)
1. En el modal de compra, intenta ingresar un monto mayor al saldo de tu cuenta (o provocar un rechazo de negocio).
2. Pulsa **Autorizar Débito**.
3. **Verificación:** Aparecerá la tarjeta roja con el mensaje real devuelto por el servidor Spring Boot (ej. *"Saldo insuficiente en cuenta..."*) y el botón cambiará a *Reintentar Compra*.

### Prueba 3: Comprobación de Sesión Única (HTTP 401)
1. Con la app móvil abierta y con sesión iniciada, abre Postman o un navegador web y haz login con el mismo usuario pero especificando `"dispositivo": "WEB"`:
   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"correo":"juan.perez@example.com","password":"Password123!","dispositivo":"WEB"}'
   ```
2. Regresa a la app móvil e intenta consultar o realizar una compra.
3. **Verificación:** El backend responde 401. La app cerrará de inmediato cualquier modal abierto, borrará el token de `FlutterSecureStorage` y mostrará la alerta modal de *Sesión Cerrada en este Dispositivo*, regresando a la pantalla de Login.

### Prueba 4: Cierre de Sesión Seguro
1. Ve a la pestaña **Perfil** (Settings).
2. Pulsa **Cerrar Sesión Segura** y confirma el diálogo.
3. **Verificación:** Se borran las credenciales y la app regresa al Login sin dejar rastros de la sesión anterior.
