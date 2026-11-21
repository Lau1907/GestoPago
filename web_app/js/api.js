/**
 * Cliente HTTP centralizado para consumo de API REST del backend Spring Boot
 */
const ApiClient = {
  // Callback global registrado cuando se detecta un 401 por sesión cerrada en otro dispositivo
  onSessionInvalidated: null,

  async request(url, options = {}) {
    const token = localStorage.getItem(AppConfig.STORAGE_KEYS.AUTH_TOKEN);
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...(options.headers || {})
    };

    if (token && !options.skipAuth) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), AppConfig.TIMEOUT_MS);

    try {
      const response = await fetch(url, {
        ...options,
        headers,
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      let data = null;
      const text = await response.text();
      if (text && text.trim().length > 0) {
        try {
          data = JSON.parse(text);
        } catch (_) {
          data = { mensaje: text };
        }
      }

      // 1. Manejo específico del HTTP 401: Control de Sesión Única
      if (response.status === 401) {
        let mensaje = 'Sesión cerrada o token inválido.';
        if (data && (data.mensaje || data.message)) {
          mensaje = data.mensaje || data.message;
        }

        // Limpiar tokens de sesión inmediatamente y resetear estado en memoria
        localStorage.removeItem(AppConfig.STORAGE_KEYS.AUTH_TOKEN);
        localStorage.removeItem(AppConfig.STORAGE_KEYS.TEMP_TOKEN);
        if (typeof AppState !== 'undefined' && AppState.clearSession) {
          AppState.clearSession();
        }

        // Notificar a la aplicación para mostrar alerta y redirigir
        if (this.onSessionInvalidated) {
          this.onSessionInvalidated(mensaje);
        }

        const error = new Error(mensaje);
        error.status = 401;
        error.isSessionInvalidated = true;
        throw error;
      }

      // 2. Manejo de errores de validación / negocio (HTTP 400, 404, 409, 422)
      if (response.status === 400 || response.status === 404 || response.status === 409 || response.status === 422) {
        const errorMsg = data?.mensaje || data?.message || 'Solicitud inválida o rechazada por el servidor.';
        const error = new Error(errorMsg);
        error.status = response.status;
        error.data = data;
        throw error;
      }

      // 3. Manejo de Timeout o Revisión con proveedor externo (HTTP 504)
      if (response.status === 504) {
        const errorMsg = data?.mensaje || data?.message || 'Tiempo de espera agotado con el proveedor externo. Transacción en revisión.';
        const error = new Error(errorMsg);
        error.status = 504;
        error.isPending = true;
        error.data = data;
        throw error;
      }

      // 4. Otros errores 5xx
      if (!response.ok) {
        const errorMsg = data?.mensaje || data?.message || `Error en el servidor (${response.status})`;
        const error = new Error(errorMsg);
        error.status = response.status;
        error.data = data;
        throw error;
      }

      return data;
    } catch (err) {
      clearTimeout(timeoutId);

      if (err.name === 'AbortError') {
        const timeoutError = new Error('Tiempo de espera agotado al conectar con el servidor.');
        timeoutError.isNetworkError = true;
        throw timeoutError;
      }

      if (err.message && (err.message.includes('Failed to fetch') || err.message.includes('NetworkError'))) {
        const netError = new Error(`No se pudo conectar con el servidor (${url}). Verifique que el backend de Spring Boot esté activo y permita conexiones.`);
        netError.isNetworkError = true;
        throw netError;
      }

      throw err;
    }
  },

  get(url, options = {}) {
    return this.request(url, { ...options, method: 'GET' });
  },

  post(url, body, options = {}) {
    return this.request(url, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : null
    });
  }
};
