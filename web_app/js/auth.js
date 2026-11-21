/**
 * Lógica de Autenticación, 2FA y Control de Sesión Única ("WEB")
 */
const Auth = {
  /**
   * Inicializa listeners y callbacks.
   */
  init() {
    // Callback global cuando el ApiClient recibe 401 (sesión invalidada)
    ApiClient.onSessionInvalidated = (mensaje) => {
      this.mostrarModalSesionInvalida(mensaje);
    };

    // Formulario Login
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
      loginForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.login();
      });
    }

    // Formulario 2FA
    const otpForm = document.getElementById('otp-form');
    if (otpForm) {
      otpForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.verificar2fa();
      });
    }
  },

  /**
   * Login tradicional enviando dispositivo = "WEB".
   */
  async login() {
    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const errorBox = document.getElementById('login-error');
    const submitBtn = document.getElementById('btn-login-submit');

    const correo = emailInput.value.trim();
    const password = passwordInput.value;

    // UI preliminar
    errorBox.style.display = 'none';
    errorBox.textContent = '';
    submitBtn.disabled = true;
    submitBtn.innerHTML = 'Validando credenciales...';

    try {
      const endpoints = AppConfig.getEndpoints();
      const payload = { correo, password, dispositivo: AppConfig.DEVICE_CHANNEL };
      const response = await ApiClient.post(endpoints.login, payload, { skipAuth: true });

      // Guardar email para posteriores llamadas
      AppState.userEmail = correo;
      localStorage.setItem(AppConfig.STORAGE_KEYS.USER_EMAIL, correo);

      if (response.requiere2FA) {
        // 2FA requerida
        AppState.preAuthToken = response.tokenTemporal || response.token;
        AppState.tempOtpDev = response.codigoOtp || null;
        if (AppState.preAuthToken) {
          localStorage.setItem(AppConfig.STORAGE_KEYS.TEMP_TOKEN, AppState.preAuthToken);
        }
        this.mostrarPantalla2FA(correo, response.codigoOtp);
      } else if (response.token) {
        // Login sin 2FA
        AppState.setAuthToken(response.token);
        await this.onAutenticacionExitosa();
      } else {
        throw new Error(response.mensaje || 'Respuesta de autenticación incompleta.');
      }
    } catch (err) {
      errorBox.textContent = err.message || 'Error al iniciar sesión.';
      errorBox.style.display = 'block';
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<span>Ingresar a la Plataforma</span> ➔';
    }
  },

  /**
   * Muestra la pantalla de ingreso del código OTP.
   */
  mostrarPantalla2FA(correo, codigoDev) {
    document.getElementById('auth-login-view').style.display = 'none';
    document.getElementById('auth-2fa-view').style.display = 'block';
    document.getElementById('otp-user-email').textContent = correo;

    const devBanner = document.getElementById('otp-dev-banner');
    const devCodeSpan = document.getElementById('otp-dev-code');
    const otpInput = document.getElementById('otp-code');

    if (codigoDev) {
      devCodeSpan.textContent = codigoDev;
      devBanner.style.display = 'block';
      otpInput.value = codigoDev; // facilitar pruebas en desarrollo
    } else {
      devBanner.style.display = 'none';
      otpInput.value = '';
    }
    otpInput.focus();
  },

  /**
   * Verifica el token OTP enviado por el usuario.
   */
  async verificar2fa() {
    const otpInput = document.getElementById('otp-code');
    const errorBox = document.getElementById('otp-error');
    const submitBtn = document.getElementById('btn-otp-submit');
    const codigo = otpInput.value.trim();

    if (codigo.length !== 6 || !/^\d{6}$/.test(codigo)) {
      errorBox.textContent = 'Ingrese un código numérico de exactamente 6 dígitos.';
      errorBox.style.display = 'block';
      return;
    }

    errorBox.style.display = 'none';
    submitBtn.disabled = true;
    submitBtn.innerHTML = 'Verificando código...';

    try {
      const endpoints = AppConfig.getEndpoints();
      const payload = {
        correo: AppState.userEmail,
        codigo: codigo,
        dispositivo: AppConfig.DEVICE_CHANNEL,
        tokenTemporal: AppState.preAuthToken
      };
      const response = await ApiClient.post(endpoints.verify2fa, payload, { skipAuth: true });

      if (response.token) {
        // Guardar token antes de continuar
        AppState.setAuthToken(response.token);
        await this.onAutenticacionExitosa();
      } else {
        throw new Error(response.mensaje || 'No se recibió el token de acceso.');
      }
    } catch (err) {
      errorBox.textContent = err.message || 'Código OTP inválido o expirado.';
      errorBox.style.display = 'block';
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<span>Validar y Acceder</span> ✓';
    }
  },

  /**
   * Acción después de un login exitoso (con o sin 2FA).
   */
  async onAutenticacionExitosa() {
    // Ocultar todo lo relacionado a la autenticación
    document.getElementById('auth-login-view').style.display = 'none';
    document.getElementById('auth-2fa-view').style.display = 'none';
    const authViews = document.getElementById('auth-views');
    if (authViews) authViews.style.display = 'none';
    // Mostrar la aplicación principal
    document.getElementById('app-views').style.display = 'block';
    window.scrollTo(0, 0);

    // Cargar datos post‑login y activar UI
    await this.cargarDatosPostLogin();
  },

  /**
   * Carga cuentas y cliente después del login y prepara la UI.
   */
  async cargarDatosPostLogin() {
    const endpoints = AppConfig.getEndpoints();
    try {
      // 1️⃣ Cargar cuentas
      const cuentas = await ApiClient.post(endpoints.cuentas, {});
      if (Array.isArray(cuentas) && cuentas.length > 0) {
        AppState.cuentas = cuentas.filter(c => c.estatus === 'ACTIVA' || c.estatus === 'A');
        AppState.cuentaSeleccionada = AppState.cuentas[0] || cuentas[0];
        localStorage.setItem(AppConfig.STORAGE_KEYS.NUMERO_CUENTA, AppState.cuentaSeleccionada.numeroCuenta);
      }

      // 2️⃣ Cargar información del cliente
      const cliente = await ApiClient.post(endpoints.cliente, { id: AppState.clienteId });
      if (cliente) {
        AppState.cliente = cliente;
      }
    } catch (err) {
      console.error('Error al cargar cuentas o cliente:', err);
      // Dejar que el manejador global del API procese 401 si ocurre
      throw err;
    }

    // 3️⃣ Refrescar header y UI principal
    App.actualizarHeaderCuenta();
    App.switchTab('catalogo');
    Catalogo.cargar();
    Historial.render();
  },

  /**
   * Cierra la sesión actual y vuelve a la pantalla de login.
   */
  logout() {
    // Limpiar estado de sesión
    AppState.clearSession();

    // Mostrar vistas de autenticación
    const authViews = document.getElementById('auth-views');
    if (authViews) authViews.style.display = 'block';
    document.getElementById('auth-login-view').style.display = 'block';
    document.getElementById('auth-2fa-view').style.display = 'none';
    // Ocultar la aplicación
    document.getElementById('app-views').style.display = 'none';

    // Resetear formulario de login
    const passInput = document.getElementById('login-password');
    if (passInput) passInput.value = '';
    const emailInput = document.getElementById('login-email');
    if (emailInput) emailInput.value = '';
    const errorBox = document.getElementById('login-error');
    if (errorBox) {
      errorBox.style.display = 'none';
      errorBox.textContent = '';
    }

    // Actualizar header (quedará sin cuenta)
    App.actualizarHeaderCuenta();
  },

  /**
   * Alterna la visibilidad de la contraseña del login.
   */
  togglePasswordVisibility() {
    const passwordInput = document.getElementById('login-password');
    const toggleIcon = document.getElementById('password-toggle-icon');
    if (!passwordInput) return;
    if (passwordInput.type === 'password') {
      passwordInput.type = 'text';
      if (toggleIcon) toggleIcon.textContent = '🙈';
    } else {
      passwordInput.type = 'password';
      if (toggleIcon) toggleIcon.textContent = '👁️';
    }
  },

  /**
   * Muestra modal indicando que la sesión fue invalidada (401).
   */
  mostrarModalSesionInvalida(mensaje) {
    // Cerrar cualquier modal secundario abierto (p.ej. de pagos)
    if (typeof Pago !== 'undefined' && Pago.cerrarModal) {
      Pago.cerrarModal();
    }
    // Limpiar sesión y UI antes de notificar al usuario
    this.logout();

    const modal = document.getElementById('modal-session-invalidated');
    const msgEl = document.getElementById('session-invalidated-msg');
    if (msgEl) {
      msgEl.textContent = mensaje || 'Se ha iniciado sesión desde otro dispositivo (ej. Móvil) o su sesión ha expirado.';
    }
    if (modal) {
      modal.classList.add('active');
    }
  },

  /**
   * Cierra el modal de sesión invalidada.
   */
  cerrarModalSesionInvalida() {
    const modal = document.getElementById('modal-session-invalidated');
    if (modal) {
      modal.classList.remove('active');
    }
  }
};
