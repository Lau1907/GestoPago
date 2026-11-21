/**
 * Gestión del Estado global de la aplicación Web
 */
const AppState = {
  userEmail: localStorage.getItem(AppConfig.STORAGE_KEYS.USER_EMAIL) || null,
  clienteId: parseInt(localStorage.getItem(AppConfig.STORAGE_KEYS.CLIENTE_ID) || '1', 10),
  cliente: null,
  cuentas: [],
  cuentaSeleccionada: null,
  productos: [],
  categoriaActiva: null,
  filtroTexto: '',
  historialExitoso: [],
  preAuthToken: localStorage.getItem(AppConfig.STORAGE_KEYS.TEMP_TOKEN) || null,
  tempOtpDev: null,

  isAuthenticated() {
    return !!localStorage.getItem(AppConfig.STORAGE_KEYS.AUTH_TOKEN);
  },

  setAuthToken(token) {
    localStorage.setItem(AppConfig.STORAGE_KEYS.AUTH_TOKEN, token);
  },

  clearSession() {
    localStorage.removeItem(AppConfig.STORAGE_KEYS.AUTH_TOKEN);
    localStorage.removeItem(AppConfig.STORAGE_KEYS.TEMP_TOKEN);
    localStorage.removeItem(AppConfig.STORAGE_KEYS.USER_EMAIL);
    localStorage.removeItem(AppConfig.STORAGE_KEYS.CLIENTE_ID);
    localStorage.removeItem(AppConfig.STORAGE_KEYS.NUMERO_CUENTA);
    this.userEmail = null;
    this.cliente = null;
    this.cuentas = [];
    this.cuentaSeleccionada = null;
    this.preAuthToken = null;
    this.tempOtpDev = null;
  },

  // Generador UUID seguro para claves de idempotencia en compras
  generateIdempotencyKey() {
    const now = new Date();
    const datePrefix = now.toISOString().slice(0, 10).replace(/-/g, '');
    const randomHex = Math.random().toString(36).substring(2, 10).toUpperCase() + 
                      Math.random().toString(36).substring(2, 6).toUpperCase();
    return `TX-${datePrefix}-${randomHex}`;
  },

  // Carga de compras exitosas guardadas localmente
  cargarHistorialLocal() {
    try {
      const raw = localStorage.getItem(AppConfig.STORAGE_KEYS.HISTORIAL);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          // Filtrado estricto: únicamente exitosas
          this.historialExitoso = parsed.filter(item => 
            item.estado === 'EXITOSA' || item.estado === 'APROBADA'
          );
        }
      }
    } catch (_) {
      this.historialExitoso = [];
    }
  },

  guardarCompraExitosa(compra) {
    if (!compra || compra.estado !== 'EXITOSA') return;
    
    // Evitar duplicados por idempotencyKey o idTransaccion
    const yaExiste = this.historialExitoso.some(item => 
      (item.idempotencyKey && item.idempotencyKey === compra.idempotencyKey) ||
      (item.idTransaccion && item.idTransaccion === compra.idTransaccion)
    );

    if (!yaExiste) {
      this.historialExitoso.unshift(compra);
      localStorage.setItem(
        AppConfig.STORAGE_KEYS.HISTORIAL, 
        JSON.stringify(this.historialExitoso)
      );
    }
  }
};
