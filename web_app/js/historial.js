/**
 * Módulo de Historial de Compras (Exclusivamente Exitosas)
 * Sincronizado en tiempo real con el backend de Spring Boot
 */
const Historial = {
  isLoading: false,

  async cargar() {
    const list = document.getElementById('history-list');
    const emptyState = document.getElementById('history-empty');
    if (!list) return;

    list.innerHTML = `
      <div style="text-align: center; padding: 30px; color: var(--primary); font-weight: 600;">
        ⏳ Sincronizando transacciones exitosas con el servidor...
      </div>
    `;
    emptyState.style.display = 'none';
    this.isLoading = true;

    try {
      const endpoints = AppConfig.getEndpoints();
      const payload = {
        clienteId: AppState.clienteId || 1,
        numeroCuenta: AppState.cuentaSeleccionada ? AppState.cuentaSeleccionada.numeroCuenta : null,
        soloExitosas: true
      };

      const response = await ApiClient.post(endpoints.historial, payload);
      if (Array.isArray(response)) {
        AppState.historialExitoso = response.filter(item => 
          item.estado === 'EXITOSA' || item.estado === 'APROBADA'
        );
        // Persistir en local como backup
        localStorage.setItem(AppConfig.STORAGE_KEYS.HISTORIAL, JSON.stringify(AppState.historialExitoso));
      } else {
        AppState.cargarHistorialLocal();
      }
    } catch (err) {
      console.warn('No se pudo sincronizar historial con el backend, usando almacenamiento local:', err.message);
      AppState.cargarHistorialLocal();
    } finally {
      this.isLoading = false;
      this.render();
    }
  },

  render() {
    const list = document.getElementById('history-list');
    const emptyState = document.getElementById('history-empty');
    if (!list) return;
    list.innerHTML = '';

    const comprasExitosas = AppState.historialExitoso;

    if (!comprasExitosas || comprasExitosas.length === 0) {
      emptyState.style.display = 'block';
      return;
    }
    emptyState.style.display = 'none';

    comprasExitosas.forEach((pago, index) => {
      const card = document.createElement('div');
      card.className = 'history-card';
      card.style.cursor = 'pointer';
      card.onclick = () => Pago.mostrarTicketComprobante(pago);

      const fechaStr = pago.fecha 
        ? new Date(pago.fecha).toLocaleString('es-MX', { 
            day: '2-digit', month: 'short', year: 'numeric', 
            hour: '2-digit', minute: '2-digit' 
          })
        : 'Reciente';

      const montoNum = parseFloat(pago.monto || 0);

      card.innerHTML = `
        <div class="history-left">
          <div class="history-product">${Catalogo.escapeHtml(pago.producto || 'Servicio')}</div>
          <div class="history-details">
            <strong>${Catalogo.escapeHtml(pago.servicio || '')}</strong> • Folio: <strong>${Catalogo.escapeHtml(pago.folioGestoPago || 'GP-' + (pago.idTransaccion || index + 1))}</strong>
          </div>
          <div class="history-details">
            Ref: ${Catalogo.escapeHtml(pago.referencia || '-')} • ${fechaStr}
          </div>
        </div>
        <div class="history-right">
          <div class="history-amount">$${montoNum.toFixed(2)} MXN</div>
          <span class="badge-success">✓ Exitosa</span>
          <span style="font-size: 11px; color: var(--primary); font-weight: 600; margin-top: 2px;">Ver Ticket ➔</span>
        </div>
      `;

      list.appendChild(card);
    });
  }
};
