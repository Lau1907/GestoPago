/**
 * Módulo de Pagos, Compras e Idempotencia
 */
const Pago = {
  productoActual: null,
  idempotencyKeyActual: null,

  abrirModalCompra(idProducto) {
    const prod = AppState.productos.find(p => p.idProducto === idProducto);
    if (!prod) return;

    this.productoActual = prod;
    this.idempotencyKeyActual = AppState.generateIdempotencyKey();

    // Rellenar datos en el modal
    document.getElementById('modal-prod-nombre').textContent = prod.producto;
    document.getElementById('modal-prod-servicio').textContent = prod.servicio;

    const cuenta = AppState.cuentaSeleccionada;
    document.getElementById('modal-cuenta-numero').textContent = cuenta 
      ? `•••• •••• ${cuenta.numeroCuenta.slice(-4)}`
      : 'Sin cuenta asignada';
    document.getElementById('modal-cuenta-saldo').textContent = cuenta 
      ? `$${cuenta.saldo.toFixed(2)} MXN`
      : '$0.00 MXN';

    const precioFijo = parseFloat(prod.precio);
    const tienePrecioFijo = !isNaN(precioFijo) && precioFijo > 0;

    const montoInput = document.getElementById('modal-input-monto');
    montoInput.value = tienePrecioFijo ? precioFijo.toFixed(2) : '';
    montoInput.disabled = tienePrecioFijo;

    // Dígito verificador
    const dvContainer = document.getElementById('modal-dv-container');
    const dvInput = document.getElementById('modal-input-dv');
    if (prod.hasDigitoVerificador) {
      dvContainer.style.display = 'block';
      dvInput.value = '';
    } else {
      dvContainer.style.display = 'none';
      dvInput.value = '';
    }

    // Limpiar referencia y estados anteriores
    document.getElementById('modal-input-referencia').value = '';
    document.getElementById('modal-idempotency-key').textContent = `Clave Idempotencia: ${this.idempotencyKeyActual}`;

    this.ocultarBannersEstado();

    const submitBtn = document.getElementById('btn-autorizar-debito');
    submitBtn.disabled = false;
    submitBtn.innerHTML = 'Autorizar Débito Seguro';

    document.getElementById('modal-compra').classList.add('active');
  },

  cerrarModal() {
    const modal = document.getElementById('modal-compra');
    if (modal) modal.classList.remove('active');
  },

  ocultarBannersEstado() {
    document.getElementById('banner-processing').style.display = 'none';
    document.getElementById('banner-rejected').style.display = 'none';
    document.getElementById('banner-pending').style.display = 'none';
    document.getElementById('banner-network-error').style.display = 'none';
  },

  /**
   * Ejecución real de la compra enviando al endpoint POST /pagos/servicio
   */
  async ejecutarPago() {
    const referencia = document.getElementById('modal-input-referencia').value.trim();
    const montoStr = document.getElementById('modal-input-monto').value.trim();
    const dv = document.getElementById('modal-input-dv').value.trim();
    const submitBtn = document.getElementById('btn-autorizar-debito');

    this.ocultarBannersEstado();

    if (!referencia || referencia.length < 3) {
      this.mostrarBannerRechazo('Por favor ingrese una referencia válida de al menos 3 caracteres.');
      return;
    }

    const monto = parseFloat(montoStr);
    if (isNaN(monto) || monto <= 0) {
      this.mostrarBannerRechazo('Ingrese un monto monetario mayor a cero.');
      return;
    }

    if (this.productoActual.hasDigitoVerificador && !dv) {
      this.mostrarBannerRechazo('Este producto requiere obligatoriamente un dígito verificador.');
      return;
    }

    const cuenta = AppState.cuentaSeleccionada;
    if (!cuenta) {
      this.mostrarBannerRechazo('No hay una cuenta bancaria activa seleccionada.');
      return;
    }

    if (cuenta.saldo < monto) {
      this.mostrarBannerRechazo(`Saldo insuficiente. Saldo disponible: $${cuenta.saldo.toFixed(2)} MXN, requerido: $${monto.toFixed(2)} MXN.`);
      return;
    }

    // Estado: Procesando
    document.getElementById('banner-processing').style.display = 'flex';
    submitBtn.disabled = true;
    submitBtn.innerHTML = 'Procesando con GestoPago...';

    const payload = {
      clienteId: AppState.clienteId || 1,
      numeroCuenta: cuenta.numeroCuenta,
      idProducto: this.productoActual.idProducto,
      monto: monto,
      referencia: referencia,
      digitoVerificador: dv || undefined,
      idempotencyKey: this.idempotencyKeyActual
    };

    try {
      const endpoints = AppConfig.getEndpoints();
      const response = await ApiClient.post(endpoints.pagar, payload);

      this.ocultarBannersEstado();
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Autorizar Débito Seguro';

      // 1. ESTADO COMPRA EXITOSA
      if (response && (response.estado === 'EXITOSA' || response.estado === 'APROBADA')) {
        // Actualizar saldo local
        if (response.saldoNuevo !== undefined && response.saldoNuevo !== null) {
          cuenta.saldo = parseFloat(response.saldoNuevo);
          App.actualizarHeaderCuenta();
        }

        // Guardar en historial exclusivo de exitosas
        AppState.guardarCompraExitosa(response);
        Historial.render();

        // Cerrar modal de compra y mostrar ticket
        this.cerrarModal();
        this.mostrarTicketComprobante(response);
      } 
      // 2. ESTADO COMPRA PENDIENTE (en revisión por timeout del proveedor externo)
      else if (response && (response.estado === 'TIMEOUT_REVISION' || response.estado === 'INICIADA')) {
        this.mostrarBannerPendiente(response.mensajeGestoPago || 'La transacción se encuentra en estado de verificación con el proveedor.');
      } 
      // 3. ESTADO COMPRA RECHAZADA (GestoPago devolvió código distinto de 01)
      else if (response && response.estado === 'FALLIDA') {
        this.cerrarModal();
        this.mostrarTicketComprobante(response);
      } else {
        this.mostrarBannerRechazo((response && response.mensajeGestoPago) || 'La transacción fue rechazada por el proveedor del servicio.');
      }
    } catch (err) {
      this.ocultarBannersEstado();
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Reintentar Compra';

      if (err.isPending || err.status === 504) {
        // 2. COMPRA PENDIENTE (Timeout 504)
        this.mostrarBannerPendiente(err.message);
      } else if (err.isNetworkError) {
        // 4. ERROR DE COMUNICACIÓN
        this.mostrarBannerErrorRed(err.message);
      } else {
        // 3. COMPRA RECHAZADA (HTTP 400 / Regla de negocio)
        this.mostrarBannerRechazo(err.message);
      }
    }
  },

  /**
   * Reconsulta de transacción en revisión mediante POST /pagos/consultar
   */
  async reconsultarTransaccion() {
    if (!this.idempotencyKeyActual) return;

    this.ocultarBannersEstado();
    document.getElementById('banner-processing').style.display = 'flex';

    try {
      const endpoints = AppConfig.getEndpoints();
      const response = await ApiClient.post(endpoints.consultarPago, {
        idempotencyKey: this.idempotencyKeyActual
      });

      this.ocultarBannersEstado();

      if (response && response.estado === 'EXITOSA') {
        if (response.saldoNuevo !== undefined && response.saldoNuevo !== null) {
          if (AppState.cuentaSeleccionada) {
            AppState.cuentaSeleccionada.saldo = parseFloat(response.saldoNuevo);
            App.actualizarHeaderCuenta();
          }
        }
        AppState.guardarCompraExitosa(response);
        Historial.render();
        this.cerrarModal();
        this.mostrarTicketComprobante(response);
      } else if (response && (response.estado === 'TIMEOUT_REVISION' || response.estado === 'INICIADA')) {
        this.mostrarBannerPendiente('La transacción continúa en verificación por el proveedor externo.');
      } else {
        this.mostrarBannerRechazo(response.mensajeGestoPago || 'La transacción resultó rechazada.');
      }
    } catch (err) {
      this.ocultarBannersEstado();
      this.mostrarBannerErrorRed(err.message);
    }
  },

  mostrarBannerRechazo(msg) {
    const b = document.getElementById('banner-rejected');
    document.getElementById('banner-rejected-msg').textContent = msg;
    b.style.display = 'flex';
  },

  mostrarBannerPendiente(msg) {
    const b = document.getElementById('banner-pending');
    document.getElementById('banner-pending-msg').textContent = msg;
    b.style.display = 'flex';
  },

  mostrarBannerErrorRed(msg) {
    const b = document.getElementById('banner-network-error');
    document.getElementById('banner-network-msg').textContent = msg;
    b.style.display = 'flex';
  },

  /**
   * Muestra el modal de comprobante/ticket bancario oficial (exitoso o rechazado)
   */
  mostrarTicketComprobante(compra) {
    if (!compra) return;
    const esExitosa = compra.estado === 'EXITOSA' || compra.estado === 'APROBADA';
    const logoEl = document.getElementById('ticket-service-logo');
    const titleEl = document.getElementById('ticket-status-title');
    const badgeEl = document.getElementById('ticket-status-badge');
    const msgEl = document.getElementById('ticket-status-msg');
    const montoEl = document.getElementById('ticket-monto');

    const serviceIcon = (typeof Catalogo !== 'undefined' && Catalogo.obtenerIcono)
      ? Catalogo.obtenerIcono(compra.servicio)
      : (esExitosa ? '✅' : '❌');

    if (logoEl) logoEl.textContent = serviceIcon;
    if (titleEl) {
      titleEl.textContent = esExitosa ? '¡Pago Aplicado con Éxito!' : 'Transacción Rechazada';
    }
    if (badgeEl) {
      badgeEl.textContent = compra.estado || (esExitosa ? 'EXITOSA' : 'FALLIDA');
      badgeEl.className = esExitosa ? 'badge-success' : 'status-badge rejected';
      badgeEl.style.background = esExitosa ? '' : 'rgba(214, 48, 49, 0.12)';
      badgeEl.style.color = esExitosa ? '' : 'var(--danger)';
      badgeEl.style.display = 'inline-block';
      badgeEl.style.padding = '4px 12px';
      badgeEl.style.borderRadius = '20px';
      badgeEl.style.fontWeight = '700';
      badgeEl.style.fontSize = '12px';
    }
    if (msgEl) {
      msgEl.textContent = compra.mensajeGestoPago
        ? `${compra.codigoGestoPago ? `[${compra.codigoGestoPago}] ` : ''}${compra.mensajeGestoPago}`
        : '';
    }

    document.getElementById('ticket-folio').textContent =
      compra.folioGestoPago || (compra.idTransaccion ? `TX-${compra.idTransaccion}` : '-');
    document.getElementById('ticket-provider-tx').textContent =
      compra.idTransaccionProveedor || '-';
    document.getElementById('ticket-upc').textContent = compra.codigoUpc || 'N/A';
    document.getElementById('ticket-auth').textContent =
      compra.autorizacionGestoPago || '-';
    document.getElementById('ticket-servicio').textContent = compra.servicio || 'Servicio';
    document.getElementById('ticket-producto').textContent = compra.producto || 'Producto';
    document.getElementById('ticket-referencia').textContent = compra.referencia || '-';
    document.getElementById('ticket-cuenta').textContent = compra.numeroCuenta
      ? `•••• •••• ${String(compra.numeroCuenta).slice(-4)}`
      : '-';

    if (montoEl) {
      montoEl.textContent = `$${parseFloat(compra.monto || 0).toFixed(2)} MXN`;
      montoEl.style.color = esExitosa ? 'var(--accent)' : 'var(--danger)';
    }
    document.getElementById('ticket-saldo-ant').textContent = compra.saldoAnterior != null
      ? `$${parseFloat(compra.saldoAnterior).toFixed(2)} MXN`
      : '-';
    document.getElementById('ticket-saldo-nuevo').textContent = compra.saldoNuevo != null
      ? `$${parseFloat(compra.saldoNuevo).toFixed(2)} MXN`
      : (compra.saldoAnterior != null && !esExitosa ? `$${parseFloat(compra.saldoAnterior).toFixed(2)} MXN` : '-');
    document.getElementById('ticket-idempotencia').textContent = compra.idempotencyKey || '-';
    document.getElementById('ticket-fecha').textContent = compra.fecha
      ? new Date(compra.fecha).toLocaleString('es-MX')
      : new Date().toLocaleString('es-MX');

    const leyendaBox = document.getElementById('ticket-leyenda-box');
    const leyendaText = document.getElementById('ticket-leyenda-text');
    if (compra.ticket) {
      leyendaText.textContent = compra.ticket;
      leyendaBox.style.display = 'block';
    } else {
      leyendaBox.style.display = 'none';
    }

    document.getElementById('modal-ticket').classList.add('active');
  },

  cerrarTicket() {
    document.getElementById('modal-ticket').classList.remove('active');
  }
};
