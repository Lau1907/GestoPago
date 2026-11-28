
const Catalogo = {
  async cargar() {
    const grid = document.getElementById('products-grid');
    const loading = document.getElementById('catalogo-loading');
    const errorBox = document.getElementById('catalogo-error');
    const chipsContainer = document.getElementById('category-chips');

    grid.innerHTML = '';
    errorBox.style.display = 'none';
    loading.style.display = 'flex';

    try {
      const endpoints = AppConfig.getEndpoints();
      const response = await ApiClient.get(endpoints.catalogo);

      if (response && Array.isArray(response.productos)) {
        AppState.productos = response.productos;
      } else {
        AppState.productos = [];
      }

      loading.style.display = 'none';
      this.renderCategorias(chipsContainer);
      this.renderProductos();
    } catch (err) {
      loading.style.display = 'none';
      errorBox.textContent = `Error al cargar el catálogo de servicios: ${err.message}`;
      errorBox.style.display = 'block';
    }
  },

  renderCategorias(container) {
    const categorias = [...new Set(AppState.productos.map(p => p.servicio))].sort();

    container.innerHTML = `
      <button class="chip ${AppState.categoriaActiva === null ? 'active' : ''}" onclick="Catalogo.filtrarCategoria(null)">
        Todos
      </button>
    `;

    categorias.forEach(cat => {
      const isSelected = AppState.categoriaActiva === cat;
      const btn = document.createElement('button');
      btn.className = `chip ${isSelected ? 'active' : ''}`;
      btn.textContent = cat;
      btn.onclick = () => Catalogo.filtrarCategoria(cat);
      container.appendChild(btn);
    });
  },

  filtrarCategoria(cat) {
    AppState.categoriaActiva = cat;
    const chipsContainer = document.getElementById('category-chips');
    this.renderCategorias(chipsContainer);
    this.renderProductos();
  },

  filtrarPorTexto(query) {
    AppState.filtroTexto = query.toLowerCase().trim();
    this.renderProductos();
  },

  renderProductos() {
    const grid = document.getElementById('products-grid');
    const emptyState = document.getElementById('catalogo-empty');
    grid.innerHTML = '';

    const filtrados = AppState.productos.filter(p => {
      const matchCat = AppState.categoriaActiva === null || p.servicio === AppState.categoriaActiva;
      const matchText = !AppState.filtroTexto ||
        (p.producto && p.producto.toLowerCase().includes(AppState.filtroTexto)) ||
        (p.servicio && p.servicio.toLowerCase().includes(AppState.filtroTexto));
      return matchCat && matchText;
    });

    if (filtrados.length === 0) {
      emptyState.style.display = 'block';
      return;
    }
    emptyState.style.display = 'none';

    filtrados.forEach(prod => {
      const card = document.createElement('div');
      card.className = 'product-card';

      const icon = this.obtenerIcono(prod.servicio);
      const precioFijo = parseFloat(prod.precio);
      const tienePrecioFijo = !isNaN(precioFijo) && precioFijo > 0;
      const precioHtml = tienePrecioFijo
        ? `$${precioFijo.toFixed(2)} MXN`
        : `<span class="free">Monto Libre</span>`;

      // Logo: imagen de red con fallback al emoji de servicio
      const logoHtml = prod.urlLogo
        ? `<img src="${prod.urlLogo}" alt="${this.escapeHtml(prod.servicio)}"
              onerror="this.style.display='none';this.nextElementSibling.style.display='flex'"
              style="width:40px;height:40px;object-fit:contain;border-radius:8px;">
           <span class="product-icon-fallback" style="display:none">${icon}</span>`
        : icon;

      card.innerHTML = `
        <div class="product-card-top">
          <div class="product-icon">${logoHtml}</div>
          <div class="product-info">
            <div class="product-title">${this.escapeHtml(prod.producto)}</div>
            <div class="product-category">${this.escapeHtml(prod.servicio)}</div>
            ${prod.legend ? `<div class="product-legend">${this.escapeHtml(prod.legend)}</div>` : ''}
          </div>
        </div>
        <div class="product-card-bottom">
          <div class="product-price">${precioHtml}</div>
          <button class="btn-pay" onclick="Pago.abrirModalCompra(${prod.idProducto})">Pagar</button>
        </div>
      `;

      grid.appendChild(card);
    });
  },

  obtenerIcono(servicio) {
    const s = (servicio || '').toLowerCase();
    if (s.includes('agua')) return '💧';
    if (s.includes('luz') || s.includes('cfe') || s.includes('energia')) return '⚡';
    if (s.includes('gas')) return '🔥';
    if (s.includes('tel') || s.includes('movil') || s.includes('recarga')) return '📱';
    if (s.includes('tv') || s.includes('cable') || s.includes('sky')) return '📺';
    if (s.includes('internet')) return '🌐';
    return '💳';
  },

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
};
