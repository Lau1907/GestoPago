
const App = {

  init() {
    Auth.init();
    AppState.cargarHistorialLocal();

    // ---- sesión existente -------------------------------------------------
    if (AppState.isAuthenticated()) {
      // Ocultar vistas de autenticación
      document.getElementById('auth-login-view').style.display = 'none';
      document.getElementById('auth-2fa-view').style.display = 'none';
      const authViews = document.getElementById('auth-views');
      if (authViews) authViews.style.display = 'none';

      // Mostrar la aplicación principal
      document.getElementById('app-views').style.display = 'block';
      window.scrollTo(0, 0);

      // Cargar datos (cuentas, cliente, UI) con manejo de errores
      Auth.cargarDatosPostLogin()
        .then(() => {
          Catalogo.cargar();
          Historial.render();
        })
        .catch(err => {
          // Si la sesión ha sido invalidada, el modal ya se mostró; silenciamos.
          if (!err.isSessionInvalidated) {
            console.error('Error al cargar datos post‑login:', err);
          }
        });
    } else {
      // ---- sin sesión ------------------------------------------------------
      document.getElementById('auth-login-view').style.display = 'block';
      document.getElementById('auth-2fa-view').style.display = 'none';
      document.getElementById('app-views').style.display = 'none';
    }

    // ---- buscador en el catálogo ------------------------------------------
    const searchInput = document.getElementById('search-catalog');
    if (searchInput) {
      searchInput.addEventListener('input', e => {
        Catalogo.filtrarPorTexto(e.target.value);
      });
    }

    // ---- UI inicial -------------------------------------------------------
    this.actualizarHeaderCuenta();
    this.renderInfoPerfil();
  },

  /** Cambia la pestaña visible */
  switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabId);
    });

    document.querySelectorAll('.view-section').forEach(sec => {
      sec.classList.remove('active');
    });

    const targetSection = document.getElementById(`section-${tabId}`);
    if (targetSection) {
      targetSection.classList.add('active');
    }

    if (tabId === 'catalogo') {
      if (AppState.productos.length === 0) {
        Catalogo.cargar();
      }
    } else if (tabId === 'historial') {
      Historial.cargar();
    } else if (tabId === 'perfil') {
      this.renderInfoPerfil();
    }
  },

  /** Actualiza el header con la cuenta actual */
  actualizarHeaderCuenta() {
    const cuenta = AppState.cuentaSeleccionada;
    const accountBadge = document.getElementById('header-account-badge');
    const balanceSpan = document.getElementById('header-balance');
    const accountNumSpan = document.getElementById('header-account-num');
    const userEmailSpan = document.getElementById('header-user-email');

    if (AppState.isAuthenticated() && cuenta) {
      accountBadge.style.display = 'block';
      balanceSpan.textContent = `$${cuenta.saldo.toFixed(2)} MXN`;
      accountNumSpan.textContent = `Cuenta: •••• ${cuenta.numeroCuenta.slice(-4)}`;
      userEmailSpan.textContent = AppState.userEmail || 'Usuario';
    } else {
      accountBadge.style.display = 'none';
      userEmailSpan.textContent = 'Canal WEB Seguro';
    }

    const currentUrlLabel = document.getElementById('current-backend-url-label');
    if (currentUrlLabel) {
      currentUrlLabel.textContent = AppConfig.getBaseUrl();
    }
  },

  /** Rellena la sección de perfil */
  renderInfoPerfil() {
    const emailEl = document.getElementById('profile-email');
    const nameEl = document.getElementById('profile-name');
    const cuentaEl = document.getElementById('profile-account');
    const saldoEl = document.getElementById('profile-balance');
    const urlInput = document.getElementById('settings-base-url');

    if (emailEl) emailEl.textContent = AppState.userEmail || '-';
    if (nameEl) {
      const cli = AppState.cliente;
      const nombre = cli ? `${cli.primerNombre} ${cli.apellidoPaterno || ''}` : 'Cliente Titular';
      nameEl.textContent = nombre;
    }

    const cuenta = AppState.cuentaSeleccionada;
    if (cuentaEl) cuentaEl.textContent = cuenta ? cuenta.numeroCuenta : '-';
    if (saldoEl) saldoEl.textContent = cuenta ? `$${cuenta.saldo.toFixed(2)} MXN` : '$0.00 MXN';

    if (urlInput) {
      urlInput.value = AppConfig.getBaseUrl();
    }
  },

  /** Cambia la URL del backend y recarga datos si está autenticado */
  guardarConfiguracionUrl() {
    const input = document.getElementById('settings-base-url');
    if (input && input.value.trim()) {
      AppConfig.setBaseUrl(input.value.trim());
      this.actualizarHeaderCuenta();
      alert(`URL de backend actualizada a: ${AppConfig.getBaseUrl()}`);
      if (AppState.isAuthenticated()) {
        Catalogo.cargar();
        Auth.cargarDatosPostLogin();
      }
    }
  },

  /** Selecciona una URL predefinida */
  setUrlPredefinida(url) {
    const input = document.getElementById('settings-base-url');
    if (input) {
      input.value = url;
      this.guardarConfiguracionUrl();
    }
  }
};

// Arrancar al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
