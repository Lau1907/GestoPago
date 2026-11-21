# Aplicación Web Frontend - Ecosistema GestoPago (Unidad II)

Esta aplicación web es el frontend oficial del ecosistema **GestoPago**, diseñada con una interfaz moderna y responsiva basada en **Material Design 3** y conectada directamente a los servicios REST del backend **Spring Boot** en `http://localhost:8080` (sin simulaciones ni mocks donde aplica la API real).

---

## 📂 Estructura del Proyecto Web

```
web_app/
├── index.html                   # Shell SPA responsiva con vistas, modales y navegación
├── css/
│   └── styles.css               # Estilos Material Design 3, paleta bancaria, animaciones y responsividad
├── js/
│   ├── config.js                # Base URL dinámica, constantes ("WEB") y endpoints del backend
│   ├── api.js                   # Cliente HTTP centralizado (fetch), captura de 401 y manejo de errores
│   ├── state.js                 # Estado global reactivo, generador de IdempotencyKey y persistencia
│   ├── auth.js                  # Login con "WEB", 2FA OTP de 6 dígitos, Logout seguro y alerta de 401
│   ├── catalogo.js              # Consumo de GET /gestopago/productos, búsqueda y filtrado por chips
│   ├── pago.js                  # Modal de compra, débito a cuenta, 4 estados de transacción y reconsulta
│   ├── historial.js             # Listado exclusivo de compras exitosas y visor de ticket oficial
│   └── app.js                   # Orquestador principal, navegación entre pestañas y listeners
├── package.json                 # Scripts para servidor estático local
└── README.md                    # Guía de ejecución y pruebas de sesión cruzada
```

---

## 🚀 Cómo Arrancar el Frontend Web en Local

Asegúrate de que tu backend de Spring Boot esté ejecutándose en `http://localhost:8080`.

Para servir el frontend web localmente, puedes usar cualquiera de estas opciones:

### Opción 1: Con Node.js / npx (Recomendada)
Desde la carpeta raíz del proyecto o desde `web_app/`:
```bash
cd web_app
npx serve . -l 3000
```
Luego abre tu navegador en **`http://localhost:3000`**.

### Opción 2: Con Python
```bash
cd web_app
python -m http.server 3000
```
Luego abre tu navegador en **`http://localhost:3000`**.

### Opción 3: Abrir directamente en el navegador
Puedes abrir el archivo [`index.html`](file:///c:/Users/laura/OneDrive/Desktop/ING.%20DESARROLLO%20Y%20GESTION%20DE%20SOFTWARE/CUATRIMESTRE%20AGOSTO-DICIEMBRE/prueba/prueba/web_app/index.html) directamente con Google Chrome, Edge o Firefox.

---

## 🔄 Cómo Probar el Flujo Cruzado Móvil <-> Web (Control de Sesión Única)

El backend implementa la regla estricta de **Sesión Única**: un usuario sólo puede tener una sesión activa a la vez entre los dispositivos/plataformas.

### Escenario A: Sesión en Web invalidada por inicio en Móvil
1. **En la Web (`http://localhost:3000`):**
   - Inicia sesión con `juan.perez@example.com` y contraseña `Password123!`.
   - La Web envía `"dispositivo": "WEB"` al endpoint `POST /auth/login`.
   - Ingresa el código OTP de 6 dígitos para completar el 2FA.
   - Observa que accedes al Catálogo y se muestra tu saldo y cuenta bancaria.
2. **En la App Móvil (Emulador o Teléfono Físico):**
   - Inicia sesión con el mismo usuario (`juan.perez@example.com`).
   - La app móvil envía `"dispositivo": "MOBILE"` y completa el 2FA.
   - En este momento, el backend de Spring Boot invalida la sesión previa del canal `"WEB"`.
3. **Regresa a la Web:**
   - Haz clic en cualquier botón de la Web (por ejemplo, refrescar el catálogo, cambiar de pestaña o pulsar *"Pagar"*).
   - El backend responderá inmediatamente **HTTP 401**:
     ```json
     {
       "codigo": 401,
       "mensaje": "Sesión inválida o cerrada. Se ha iniciado sesión desde otro dispositivo o plataforma."
     }
     ```
   - El cliente `api.js` captura el 401, borra de inmediato el JWT de `localStorage`, despliega el modal informativo **"Sesión Cerrada en este Dispositivo"** y redirige al Login.

### Escenario B: Sesión en Móvil invalidada por inicio en Web
1. Mantén la sesión activa en la App Móvil.
2. Inicia sesión en la Web con las mismas credenciales.
3. Intenta realizar una compra o actualizar el catálogo en la App Móvil:
   - La app móvil recibirá el HTTP 401, desplegará la alerta modal de sesión cerrada, limpiará `FlutterSecureStorage` y regresará al Login.

---

## 🛒 Verificación de los 4 Estados de Compra en la Web

Al presionar **"Pagar"** sobre cualquier servicio del Catálogo:
1. **Compra Exitosa**:
   - Con saldo suficiente en la cuenta bancaria, pulsa *"Autorizar Débito Seguro"*.
   - El backend debita la cuenta y responde HTTP 200 con folio y ticket de GestoPago.
   - Se abre el modal con el **Comprobante Oficial de Pago / Ticket Bancario** y se actualiza el saldo en la cabecera.
   - La compra se refleja en la pestaña **Historial de Compras**.
2. **Compra Rechazada**:
   - Intenta ingresar un monto mayor al saldo de tu cuenta (o un valor inválido).
   - El backend responde HTTP 400 con `SaldoInsuficienteException`.
   - Se muestra la tarjeta roja con el mensaje exacto del servidor:
     *"Saldo insuficiente en cuenta: Saldo actual $X, Monto requerido $Y"*.
3. **Compra Pendiente**:
   - Si ocurre un timeout con el proveedor externo GestoPago (HTTP 504) o la transacción queda en revisión.
   - Se muestra la tarjeta ámbar con el mensaje explicativo y el botón activo para **"Reconsultar Estado en Servidor"** llamando a `POST /pagos/consultar`.
4. **Error de Comunicación**:
   - Si detienes el backend de Spring Boot o colocas una URL inaccesible.
   - Se despliega la alerta de red indicando que no se pudo conectar con el servidor.

---

## 🎨 Coherencia de Diseño con la App Móvil

- **Paleta de Colores Bancaria**:
  - Azul Marino Primario: `#0A3D62`
  - Verde Esmeralda de Éxito: `#00A86B`
  - Rojo de Alerta/Rechazo: `#D63031`
  - Ámbar de Revisión: `#F39C12`
  - Fondo: `#F8F9FA`
- **Layout Responsivo**:
  - En pantallas de escritorio: Grid de servicios de 3 o 4 columnas, encabezado completo con saldo y detalles.
  - En tablets y teléfonos: Grid de 1 o 2 columnas, modales adaptables y barra de pestañas scrollable sin cortes ni desbordamientos.
