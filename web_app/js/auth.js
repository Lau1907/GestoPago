/**
 * Lógica de Autenticación, 2FA y Control de Sesión Única ("WEB")
 */
const Auth = {
  /**
   * Inicializa listeners y callbacks.
   */
  init() {
    // Register form handling
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
      registerForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.registrarCliente();
      });
    }
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
        if (AppState.preAuthToken) {
          localStorage.setItem(AppConfig.STORAGE_KEYS.TEMP_TOKEN, AppState.preAuthToken);
        }
        this.mostrarPantalla2FA(correo);
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
   * Muestra la pantalla de registro KYC (Paso 1) y limpia errores previos.
   */
  mostrarPantallaRegistro() {
    document.getElementById('auth-login-view').style.display = 'none';
    document.getElementById('auth-2fa-view').style.display = 'none';
    document.getElementById('auth-register-view').style.display = 'flex';
    this.irAPasoRegistro(1);
    this.actualizarRequisitosPassword(document.getElementById('register-password')?.value || '');
    const errorBox = document.getElementById('register-error');
    if (errorBox) {
      errorBox.style.display = 'none';
      errorBox.textContent = '';
    }
    window.scrollTo(0, 0);
  },

  /**
   * Muestra la pantalla de login y oculta registro y 2FA.
   */
  mostrarPantallaLogin() {
    document.getElementById('auth-register-view').style.display = 'none';
    document.getElementById('auth-2fa-view').style.display = 'none';
    document.getElementById('auth-login-view').style.display = 'flex';
  },

  /**
   * Navega al paso indicado del wizard KYC (1, 2 o 3) actualizando el stepper.
   */
  irAPasoRegistro(paso) {
    const errorBox = document.getElementById('register-error');
    if (errorBox) {
      errorBox.style.display = 'none';
      errorBox.textContent = '';
    }

    for (let i = 1; i <= 3; i++) {
      const panel = document.getElementById(`kyc-step-${i}`);
      if (panel) panel.style.display = i === paso ? 'block' : 'none';

      const indicator = document.getElementById(`kyc-step-indicator-${i}`);
      const circle = document.getElementById(`kyc-step-circle-${i}`);
      if (indicator && circle) {
        indicator.classList.remove('active', 'completed');
        if (i < paso) {
          indicator.classList.add('completed');
          circle.textContent = '✓';
        } else if (i === paso) {
          indicator.classList.add('active');
          circle.textContent = String(i);
        } else {
          circle.textContent = String(i);
        }
      }
    }

    const line1 = document.getElementById('kyc-step-line-1');
    const line2 = document.getElementById('kyc-step-line-2');
    if (line1) line1.classList.toggle('completed', paso > 1);
    if (line2) line2.classList.toggle('completed', paso > 2);
  },

  /**
   * Valida los campos del paso actual antes de avanzar al siguiente paso.
   */
  siguientePasoRegistro(pasoActual) {
    const msg = this.validarPasoRegistro(pasoActual);
    if (msg) {
      this.mostrarErrorRegistro(msg);
      return;
    }
    this.irAPasoRegistro(pasoActual + 1);
  },

  mostrarErrorRegistro(mensaje) {
    const errorBox = document.getElementById('register-error');
    if (errorBox) {
      errorBox.textContent = mensaje;
      errorBox.style.display = 'block';
    }
  },

  validarPasoRegistro(paso) {
    const getVal = (id) => (document.getElementById(id)?.value || '').trim();
    const nombreRegex = /^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$/;

    if (paso === 1) {
      const primerNombre = getVal('register-primerNombre');
      const segundoNombre = getVal('register-segundoNombre');
      const apellidoPaterno = getVal('register-apellidoPaterno');
      const apellidoMaterno = getVal('register-apellidoMaterno');
      const fechaNacimiento = getVal('register-fechaNacimiento');
      const sexo = getVal('register-sexo');
      const estadoCivil = getVal('register-estadoCivil');

      if (!nombreRegex.test(primerNombre)) return 'El primer nombre debe tener entre 2 y 50 letras.';
      if (segundoNombre && !nombreRegex.test(segundoNombre)) return 'El segundo nombre debe tener entre 2 y 50 letras.';
      if (!nombreRegex.test(apellidoPaterno)) return 'El apellido paterno debe tener entre 2 y 50 letras.';
      if (!nombreRegex.test(apellidoMaterno)) return 'El apellido materno debe tener entre 2 y 50 letras.';
      if (!fechaNacimiento) return 'La fecha de nacimiento es obligatoria.';

      const dob = new Date(`${fechaNacimiento}T00:00:00`);
      const hoy = new Date();
      let edad = hoy.getFullYear() - dob.getFullYear();
      const m = hoy.getMonth() - dob.getMonth();
      if (m < 0 || (m === 0 && hoy.getDate() < dob.getDate())) edad--;
      if (isNaN(dob.getTime()) || edad < 18) {
        return 'Debes ser mayor de 18 años para solicitar la apertura bancaria.';
      }

      if (!['H', 'M'].includes(sexo)) return 'Selecciona tu sexo.';
      if (!['S', 'C', 'D', 'V', 'U'].includes(estadoCivil)) return 'Selecciona tu estado civil.';
      return null;
    }

    if (paso === 2) {
      const curp = getVal('register-curp').toUpperCase();
      const rfc = getVal('register-rfc').toUpperCase();
      const telefono = getVal('register-telefonoMovil');
      const correo = getVal('register-correo');
      const ocupacion = getVal('register-ocupacion');
      const ingreso = getVal('register-ingresoMensual');

      const curpRegex = /^[A-Z][AEIOUX][A-Z]{2}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9]\d$/;
      const rfcRegex = /^[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}$/;
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      if (!curpRegex.test(curp)) return 'La CURP no cumple con el formato oficial de 18 caracteres.';
      if (!rfcRegex.test(rfc)) return 'El RFC debe tener 12 o 13 caracteres válidos.';
      if (!/^\d{10}$/.test(telefono)) return 'El teléfono móvil debe tener exactamente 10 dígitos.';
      if (!emailRegex.test(correo)) return 'Ingresa un correo electrónico válido.';
      if (!ocupacion) return 'Selecciona tu ocupación.';
      if (!ingreso || parseFloat(ingreso) <= 0) return 'Selecciona tu rango de ingreso mensual.';
      return null;
    }

    if (paso === 3) {
      const cp = getVal('register-codigoPostal');
      const estado = getVal('register-estado');
      const municipio = getVal('register-municipio');
      const colonia = getVal('register-colonia');
      const calle = getVal('register-calle');
      const numExt = getVal('register-numeroExterior');
      const password = document.getElementById('register-password')?.value || '';
      const confirmPassword = document.getElementById('register-confirmPassword')?.value || '';

      if (!/^\d{5}$/.test(cp)) return 'El código postal debe tener exactamente 5 dígitos.';
      if (!estado) return 'Selecciona tu estado.';
      if (!municipio) return 'Ingresa tu alcaldía o municipio.';
      if (!colonia) return 'Ingresa tu colonia.';
      if (!calle) return 'Ingresa tu calle.';
      if (!numExt) return 'Ingresa el número exterior.';

      const checks = this.evaluarPassword(password);
      if (!checks.len || !checks.upper || !checks.lower || !checks.digit || !checks.symbol) {
        return 'La contraseña no cumple con todos los requisitos de seguridad.';
      }
      if (password !== confirmPassword) {
        return 'La confirmación de contraseña no coincide.';
      }
      return null;
    }

    return null;
  },

  evaluarPassword(pwd) {
    return {
      len: pwd.length >= 8 && pwd.length <= 12,
      upper: /[A-Z]/.test(pwd),
      lower: /[a-z]/.test(pwd),
      digit: /[0-9]/.test(pwd),
      symbol: /[^A-Za-z0-9]/.test(pwd)
    };
  },

  actualizarRequisitosPassword(pwd) {
    const c = this.evaluarPassword(pwd || '');
    const map = {
      'req-len': c.len,
      'req-upper': c.upper,
      'req-lower': c.lower,
      'req-digit': c.digit,
      'req-symbol': c.symbol
    };
    Object.entries(map).forEach(([id, ok]) => {
      const el = document.getElementById(id);
      if (el) el.classList.toggle('met', ok);
    });
  },

  toggleRegisterPasswordVisibility(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    if (!input) return;
    if (input.type === 'password') {
      input.type = 'text';
      if (icon) icon.textContent = '🙈';
    } else {
      input.type = 'password';
      if (icon) icon.textContent = '👁️';
    }
  },

  /**
   * Registra un nuevo cliente enviando POST /clientes sin autenticación.
   */
  async registrarCliente() {
    const errorMsg = this.validarPasoRegistro(3);
    if (errorMsg) {
      this.mostrarErrorRegistro(errorMsg);
      return;
    }

    const errorBox = document.getElementById('register-error');
    const submitBtn = document.getElementById('btn-register-submit');
    const form = document.getElementById('register-form');

    const getVal = (id) => (document.getElementById(id)?.value || '').trim();
    const getOptionalVal = (id) => {
      const v = getVal(id);
      return v.length > 0 ? v : null;
    };

    const payload = {
      primerNombre: getVal('register-primerNombre'),
      segundoNombre: getOptionalVal('register-segundoNombre'),
      apellidoPaterno: getVal('register-apellidoPaterno'),
      apellidoMaterno: getVal('register-apellidoMaterno'),
      curp: getVal('register-curp').toUpperCase(),
      rfc: getVal('register-rfc').toUpperCase(),
      correo: getVal('register-correo'),
      fechaNacimiento: getVal('register-fechaNacimiento'),
      telefonoMovil: getVal('register-telefonoMovil'),
      telefonoAlternativo: null,
      sexo: getVal('register-sexo'),
      estadoCivil: getVal('register-estadoCivil'),
      nacionalidad: 'Mexicana',
      ocupacion: getVal('register-ocupacion'),
      empresa: 'No especificada',
      ingresoMensual: parseFloat(getVal('register-ingresoMensual')),
      domicilio: {
        calle: getVal('register-calle'),
        numeroExterior: getVal('register-numeroExterior'),
        numeroInterior: getOptionalVal('register-numeroInterior'),
        colonia: getVal('register-colonia'),
        municipio: getVal('register-municipio'),
        estado: getVal('register-estado'),
        pais: 'México',
        codigoPostal: getVal('register-codigoPostal')
      },
      password: document.getElementById('register-password')?.value || ''
    };

    if (errorBox) {
      errorBox.style.display = 'none';
      errorBox.textContent = '';
    }
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Procesando solicitud...';
    }

    try {
      const endpoints = AppConfig.getEndpoints();
      await ApiClient.post(endpoints.registroCliente, payload, { skipAuth: true });
      if (form) form.reset();
      this.actualizarRequisitosPassword('');
      this.irAPasoRegistro(1);
      alert('¡Solicitud de apertura bancaria exitosa! Ya puedes iniciar sesión con tu correo y contraseña.');
      this.mostrarPantallaLogin();
    } catch (err) {
      const detalles = err.data?.errores
        ? Object.values(err.data.errores).join(' • ')
        : '';
      this.mostrarErrorRegistro(
        detalles
          ? `${err.message || 'Error de validación'}: ${detalles}`
          : (err.message || 'No se pudo completar el registro del cliente.')
      );
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Solicitar Apertura Bancaria';
      }
    }
  },

  /**
   * Muestra la pantalla de ingreso del código OTP.
   */
  mostrarPantalla2FA(correo) {
    document.getElementById('auth-login-view').style.display = 'none';
    const regView = document.getElementById('auth-register-view');
    if (regView) regView.style.display = 'none';
    document.getElementById('auth-2fa-view').style.display = 'block';
    document.getElementById('otp-user-email').textContent = correo;

    const otpError = document.getElementById('otp-error');
    if (otpError) {
      otpError.style.display = 'none';
      otpError.textContent = '';
    }

    const otpInput = document.getElementById('otp-code');
    otpInput.value = '';
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
    const regView = document.getElementById('auth-register-view');
    if (regView) regView.style.display = 'none';
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
    const regView = document.getElementById('auth-register-view');
    if (regView) regView.style.display = 'none';
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
