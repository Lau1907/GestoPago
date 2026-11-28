/**
 * Configuración global del Frontend Web de GestoPago
 */
const AppConfig = {
  // URL base del backend Spring Boot (configurable en tiempo de ejecución)
  DEFAULT_BASE_URL: 'http://localhost:8080',
  
  // Canal de dispositivo exigido por el backend para el control de sesión única
  DEVICE_CHANNEL: 'WEB',

  // Timeout para peticiones de red (milisegundos)
  TIMEOUT_MS: 15000,

  // Clave en almacenamiento local
  STORAGE_KEYS: {
    BASE_URL: 'gestopago_web_base_url',
    AUTH_TOKEN: 'gestopago_web_jwt_token',
    TEMP_TOKEN: 'gestopago_web_temp_token',
    USER_EMAIL: 'gestopago_web_user_email',
    CLIENTE_ID: 'gestopago_web_cliente_id',
    NUMERO_CUENTA: 'gestopago_web_numero_cuenta',
    HISTORIAL: 'gestopago_web_historial_exitoso'
  },

  getBaseUrl() {
    return localStorage.getItem(this.STORAGE_KEYS.BASE_URL) || this.DEFAULT_BASE_URL;
  },

  setBaseUrl(newUrl) {
    const cleanUrl = newUrl.trim().replace(/\/+$/, '');
    localStorage.setItem(this.STORAGE_KEYS.BASE_URL, cleanUrl);
  },

  getEndpoints() {
    const base = this.getBaseUrl();
    return {
      login: `${base}/auth/login`,
      verify2fa: `${base}/auth/2fa/verificar`,
      catalogo: `${base}/gestopago/productos`,
      pagar: `${base}/pagos/servicio`,
      consultarPago: `${base}/pagos/consultar`,
      historial: `${base}/pagos/historial`,
      cuentas: `${base}/cuentas/buscar`,
      cliente: `${base}/clientes/consultar`,
      registroCliente: `${base}/clientes`
    };
  }
};
