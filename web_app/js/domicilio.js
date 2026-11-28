/**
 * Módulo de Autocompletado de Domicilio en México (Paso 3 KYC)
 * - Búsqueda por Código Postal (5 dígitos) vía API pública SEPOMEX / Zippopotam con timeout corto
 * - Cascada Estado -> Municipio/Alcaldía -> Colonia con catálogo local de respaldo
 * - Opción "Otra / Ingresar manualmente" siempre disponible sin bloquear el registro
 */
const Domicilio = {
  MANUAL_VALUE: '__MANUAL__',
  _ultimoCpConsultado: '',

  /**
   * Catálogo base de respaldo (32 estados con municipios/alcaldías y colonias representativas)
   */
  CATALOGO: {
    'Aguascalientes': {
      'Aguascalientes': { cp: '20000', colonias: ['Centro', 'Jardines de la Asunción', 'Las Américas', 'Pulgas Pandas'] },
      'Jesús María': { cp: '20900', colonias: ['Centro', 'Maravillas', 'Paseos de Aguascalientes'] },
      'Calvillo': { cp: '20800', colonias: ['Centro', 'Liberal', 'López Mateos'] }
    },
    'Baja California': {
      'Tijuana': { cp: '22000', colonias: ['Zona Centro', 'Zona Río', 'Playas de Tijuana', 'Otay'] },
      'Mexicali': { cp: '21000', colonias: ['Centro Cívico', 'Nueva', 'Justo Sierra'] },
      'Ensenada': { cp: '22800', colonias: ['Zona Centro', 'Valle Dorado', 'Chapultepec'] }
    },
    'Baja California Sur': {
      'La Paz': { cp: '23000', colonias: ['Centro', 'Esterito', 'Fidepaz'] },
      'Los Cabos': { cp: '23400', colonias: ['San José del Cabo Centro', 'El Médano', 'Cabo San Lucas Centro'] }
    },
    'Campeche': {
      'Campeche': { cp: '24000', colonias: ['Centro Histórico', 'San Román', 'Guadalupe'] },
      'Carmen': { cp: '24100', colonias: ['Centro', 'Playa Norte', 'Justo Sierra'] }
    },
    'Chiapas': {
      'Tuxtla Gutiérrez': { cp: '29000', colonias: ['Centro', 'Terán', 'Moctezuma', 'Las Palmas'] },
      'San Cristóbal de las Casas': { cp: '29200', colonias: ['Centro', 'Guadalupe', 'El Cerrillo'] },
      'Tapachula': { cp: '30700', colonias: ['Centro', '5 de Febrero', 'Los Naranjos'] }
    },
    'Chihuahua': {
      'Chihuahua': { cp: '31000', colonias: ['Centro', 'San Felipe', 'Quintas del Sol'] },
      'Juárez': { cp: '32000', colonias: ['Zona Centro', 'Pronaf', 'Campos Elíseos'] }
    },
    'Ciudad de México': {
      'Benito Juárez': { cp: '03100', colonias: ['Del Valle Centro', 'Del Valle Norte', 'Narvarte Poniente', 'Nápoles', 'Portales'] },
      'Cuauhtémoc': { cp: '06600', colonias: ['Juárez', 'Roma Norte', 'Condesa', 'Centro', 'Doctores'] },
      'Miguel Hidalgo': { cp: '11560', colonias: ['Polanco', 'Anzures', 'Lomas de Chapultepec', 'Escandón'] },
      'Coyoacán': { cp: '04000', colonias: ['Villa Coyoacán', 'Del Carmen', 'Copilco', 'Pedregal de Santo Domingo'] },
      'Álvaro Obregón': { cp: '01000', colonias: ['San Ángel', 'Florida', 'Guadalupe Inn', 'Santa Fe'] },
      'Iztapalapa': { cp: '09000', colonias: ['San Pablo', 'Leyes de Reforma', 'Santa Cruz Meyehualco'] },
      'Gustavo A. Madero': { cp: '07000', colonias: ['Lindavista', 'Villa de Guadalupe', 'Tepeyac Insurgentes'] },
      'Tlalpan': { cp: '14000', colonias: ['Tlalpan Centro', 'Coapa', 'Pedregal de Carrasco'] }
    },
    'Coahuila': {
      'Saltillo': { cp: '25000', colonias: ['Zona Centro', 'República', 'Doctores'] },
      'Torreón': { cp: '27000', colonias: ['Centro', 'Campestre La Rosita', 'Los Ángeles'] }
    },
    'Colima': {
      'Colima': { cp: '28000', colonias: ['Centro', 'Lomas de Circunvalación', 'Jardines Vista Hermosa'] },
      'Manzanillo': { cp: '28200', colonias: ['Centro', 'Salagua', 'Santiago'] }
    },
    'Durango': {
      'Durango': { cp: '34000', colonias: ['Zona Centro', 'Guadalupe', 'Los Remedios'] },
      'Gómez Palacio': { cp: '35000', colonias: ['Centro', 'Las Rosas', 'Campestre'] }
    },
    'Estado de México': {
      'Naucalpan de Juárez': { cp: '53100', colonias: ['Ciudad Satélite', 'Lomas Verdes', 'Naucalpan Centro'] },
      'Tlalnepantla de Baz': { cp: '54000', colonias: ['Tlalnepantla Centro', 'Valle Dorado', 'Viveros de la Loma'] },
      'Ecatepec de Morelos': { cp: '55000', colonias: ['San Cristóbal Centro', 'Jardines de Morelos', 'Las Américas'] },
      'Toluca': { cp: '50000', colonias: ['Centro', 'Universidad', 'Morelos'] },
      'Huixquilucan': { cp: '52760', colonias: ['Interlomas', 'Tecamachalco', 'La Herradura'] }
    },
    'Guanajuato': {
      'León': { cp: '37000', colonias: ['Centro', 'Jardines del Moral', 'Panorama'] },
      'Guanajuato': { cp: '36000', colonias: ['Centro', 'Marfil', 'Pastita'] },
      'Irapuato': { cp: '36500', colonias: ['Centro', 'Las Reynas', 'Moderna'] }
    },
    'Guerrero': {
      'Acapulco de Juárez': { cp: '39300', colonias: ['Centro', 'Costa Azul', 'Magallanes'] },
      'Chilpancingo de los Bravo': { cp: '39000', colonias: ['Centro', 'Universal', 'Guerrero'] }
    },
    'Hidalgo': {
      'Pachuca de Soto': { cp: '42000', colonias: ['Centro', 'Periodistas', 'Zona Plateada'] },
      'Tulancingo de Bravo': { cp: '43600', colonias: ['Centro', 'Jardines del Sur', 'Vicente Guerrero'] }
    },
    'Jalisco': {
      'Guadalajara': { cp: '44100', colonias: ['Centro', 'Americana', 'Providencia', 'Chapalita'] },
      'Zapopan': { cp: '45050', colonias: ['Zapopan Centro', 'Puerta de Hierro', 'Ciudad del Sol', 'Valle Real'] },
      'Puerto Vallarta': { cp: '48300', colonias: ['Centro', '5 de Diciembre', 'Marina Vallarta'] }
    },
    'Michoacán': {
      'Morelia': { cp: '58000', colonias: ['Centro Histórico', 'Chapultepec Norte', 'Las Américas'] },
      'Uruapan': { cp: '60000', colonias: ['Centro', 'Morelos', 'La Quinta'] }
    },
    'Morelos': {
      'Cuernavaca': { cp: '62000', colonias: ['Centro', 'Vista Hermosa', 'Lomas de la Selva'] },
      'Jiutepec': { cp: '62550', colonias: ['Centro', 'Civac', 'Tejalpa'] }
    },
    'Nayarit': {
      'Tepic': { cp: '63000', colonias: ['Centro', 'Ciudad del Valle', 'Los Llanitos'] },
      'Bahía de Banderas': { cp: '63732', colonias: ['Nuevo Vallarta', 'Bucerías', 'Mezcales'] }
    },
    'Nuevo León': {
      'Monterrey': { cp: '64000', colonias: ['Centro', 'Obispado', 'Cumbres', 'Contry'] },
      'San Pedro Garza García': { cp: '66220', colonias: ['Del Valle', 'Valle Oriente', 'San Agustín'] },
      'San Nicolás de los Garza': { cp: '66400', colonias: ['Centro', 'Anáhuac', 'Las Puentes'] }
    },
    'Oaxaca': {
      'Oaxaca de Juárez': { cp: '68000', colonias: ['Centro', 'Reforma', 'Jalatlaco'] },
      'Salina Cruz': { cp: '70600', colonias: ['Centro', 'Hidalgo Poniente', 'Barrio Nuevo'] }
    },
    'Puebla': {
      'Puebla': { cp: '72000', colonias: ['Centro Histórico', 'La Paz', 'Angelópolis', 'Huexotitla'] },
      'San Andrés Cholula': { cp: '72810', colonias: ['Centro', 'Lomas de Angelópolis', 'Tlaxcalancingo'] }
    },
    'Querétaro': {
      'Querétaro': { cp: '76000', colonias: ['Centro Histórico', 'Juriquilla', 'Álamos', 'El Campanario'] },
      'San Juan del Río': { cp: '76800', colonias: ['Centro', 'San Cayetano', 'Indeco'] }
    },
    'Quintana Roo': {
      'Benito Juárez': { cp: '77500', colonias: ['Centro', 'Supermanzana 15', 'Zona Hotelera', 'Alfredo V. Bonfil'] },
      'Solidaridad': { cp: '77710', colonias: ['Playa del Carmen Centro', 'Playacar', 'Ejidal'] },
      'Othón P. Blanco': { cp: '77000', colonias: ['Chetumal Centro', 'Adolfo López Mateos', 'Forjadores'] }
    },
    'San Luis Potosí': {
      'San Luis Potosí': { cp: '78000', colonias: ['Centro Histórico', 'Lomas', 'Del Valle'] },
      'Soledad de Graciano Sánchez': { cp: '78430', colonias: ['Cabecera Municipal', 'Hogares Ferrocarrileros'] }
    },
    'Sinaloa': {
      'Culiacán': { cp: '80000', colonias: ['Centro', 'Tres Ríos', 'Chapultepec'] },
      'Mazatlán': { cp: '82000', colonias: ['Centro', 'Zona Dorada', 'Sábalo Country'] }
    },
    'Sonora': {
      'Hermosillo': { cp: '83000', colonias: ['Centro', 'Pitic', 'Villa Satélite'] },
      'Cajeme': { cp: '85000', colonias: ['Ciudad Obregón Centro', 'Norte', 'Cumuripa'] }
    },
    'Tabasco': {
      'Centro': { cp: '86000', colonias: ['Villahermosa Centro', 'Tabasco 2000', 'Atasta'] },
      'Cárdenas': { cp: '86500', colonias: ['Centro', 'Pueblo Nuevo'] }
    },
    'Tamaulipas': {
      'Reynosa': { cp: '88500', colonias: ['Zona Centro', 'Del Valle', 'Las Fuentes'] },
      'Tampico': { cp: '89000', colonias: ['Zona Centro', 'Lomas de Rosales', 'Petrolera'] }
    },
    'Tlaxcala': {
      'Tlaxcala': { cp: '90000', colonias: ['Centro', 'La Loma Xicohténcatl', 'Ocotlán'] },
      'Apizaco': { cp: '90300', colonias: ['Centro', 'Fátima', 'Ferrocarrilera'] }
    },
    'Veracruz': {
      'Veracruz': { cp: '91700', colonias: ['Centro', 'Reforma', 'Zaragoza'] },
      'Boca del Río': { cp: '94290', colonias: ['Cabecera Municipal', 'Costa de Oro', 'Mocambo'] },
      'Xalapa': { cp: '91000', colonias: ['Zona Centro', 'Ánimas', 'Macuiltépetl'] }
    },
    'Yucatán': {
      'Mérida': { cp: '97000', colonias: ['Centro', 'García Ginerés', 'Montejo', 'Altabrisa'] },
      'Valladolid': { cp: '97780', colonias: ['Centro', 'Candelaria', 'Santa Ana'] }
    },
    'Zacatecas': {
      'Zacatecas': { cp: '98000', colonias: ['Centro Histórico', 'Lomas de la Soledad', 'Tres Cruces'] },
      'Guadalupe': { cp: '98600', colonias: ['Centro', 'Tierra y Libertad', 'Villas de Guadalupe'] }
    }
  },

  init() {
    const cpInput = document.getElementById('register-codigoPostal');
    const estadoSelect = document.getElementById('register-estado');
    const munSelect = document.getElementById('register-municipio-select');
    const colSelect = document.getElementById('register-colonia-select');

    if (cpInput) {
      cpInput.addEventListener('input', (e) => {
        const clean = e.target.value.replace(/[^0-9]/g, '').slice(0, 5);
        if (e.target.value !== clean) e.target.value = clean;
        this.onCodigoPostalInput(clean);
      });
    }
    if (estadoSelect) {
      estadoSelect.addEventListener('change', () => this.onEstadoChange());
    }
    if (munSelect) {
      munSelect.addEventListener('change', () => this.onMunicipioChange());
    }
    if (colSelect) {
      colSelect.addEventListener('change', () => this.onColoniaChange());
    }
  },

  reset() {
    this._ultimoCpConsultado = '';
    this.setCpStatus('', '');
    const munSelect = document.getElementById('register-municipio-select');
    const colSelect = document.getElementById('register-colonia-select');
    const munInput = document.getElementById('register-municipio');
    const colInput = document.getElementById('register-colonia');

    if (munSelect) {
      munSelect.innerHTML = `<option value="">Selecciona un estado primero</option><option value="${this.MANUAL_VALUE}">Otra / Ingresar manualmente</option>`;
      munSelect.value = '';
    }
    if (colSelect) {
      colSelect.innerHTML = `<option value="">Selecciona un municipio primero</option><option value="${this.MANUAL_VALUE}">Otra / Ingresar manualmente</option>`;
      colSelect.value = '';
    }
    if (munInput) {
      munInput.value = '';
      munInput.style.display = 'none';
    }
    if (colInput) {
      colInput.value = '';
      colInput.style.display = 'none';
    }
  },

  setCpStatus(text, type) {
    const el = document.getElementById('register-cp-status');
    if (!el) return;
    if (!text) {
      el.style.display = 'none';
      el.textContent = '';
      return;
    }
    el.style.display = 'block';
    el.textContent = text;
    el.style.color = type === 'ok' ? '#34d399' : (type === 'warn' ? '#fbbf24' : '#93c5fd');
  },

  normalizarEstado(nombre) {
    if (!nombre) return '';
    const clean = nombre.trim().toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '');

    if (clean.includes('distrito federal') || clean === 'cdmx' || clean.includes('ciudad de mexico')) {
      return 'Ciudad de México';
    }
    if (clean === 'mexico' || clean.includes('estado de mexico') || clean === 'edomex') {
      return 'Estado de México';
    }
    if (clean.includes('coahuila')) return 'Coahuila';
    if (clean.includes('michoacan')) return 'Michoacán';
    if (clean.includes('veracruz')) return 'Veracruz';

    const estados = Object.keys(this.CATALOGO);
    for (const est of estados) {
      const estNorm = est.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      if (estNorm === clean || clean.includes(estNorm) || estNorm.includes(clean)) {
        return est;
      }
    }
    return '';
  },

  async onCodigoPostalInput(cp) {
    if (!cp || cp.length < 5) {
      this._ultimoCpConsultado = '';
      this.setCpStatus('', '');
      return;
    }
    if (cp === this._ultimoCpConsultado) return;
    this._ultimoCpConsultado = cp;

    this.setCpStatus('Consultando C.P. en México...', 'info');
    const resultado = await this.buscarPorCodigoPostal(cp);

    if (resultado && resultado.estado) {
      this.aplicarResultadoPostal(resultado);
      this.setCpStatus(`✓ C.P. ${cp} localizado: ${resultado.municipio || ''}, ${resultado.estado}`, 'ok');
    } else {
      this.setCpStatus('C.P. no encontrado en línea. Selecciona Estado/Municipio o usa "Otra / Ingresar manualmente".', 'warn');
    }
  },

  async buscarPorCodigoPostal(cp) {
    // 1. Buscar primero en catálogo local (instantáneo y sin red)
    for (const [estado, municipios] of Object.entries(this.CATALOGO)) {
      for (const [municipio, info] of Object.entries(municipios)) {
        if (info.cp === cp) {
          return { estado, municipio, colonias: info.colonias };
        }
      }
    }

    // 2. Consultar API pública SEPOMEX (timeout corto 3.5s)
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 3500);
      const res = await fetch(`https://sepomex.icalialabs.com/api/v1/zip_codes?zip_code=${encodeURIComponent(cp)}`, {
        signal: controller.signal
      });
      clearTimeout(t);
      if (res.ok) {
        const data = await res.json();
        const items = Array.isArray(data?.zip_codes) ? data.zip_codes : [];
        if (items.length > 0) {
          const estado = this.normalizarEstado(items[0].d_estado);
          const municipio = items[0].d_mnpio || '';
          const colonias = [...new Set(items.map(i => i.d_asenta).filter(Boolean))];
          if (estado) {
            return { estado, municipio, colonias };
          }
        }
      }
    } catch (_) {
      // Fallback silencioso a siguiente proveedor
    }

    // 3. Fallback secundario: Zippopotam.us (timeout 3.5s)
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 3500);
      const res = await fetch(`https://api.zippopotam.us/mx/${encodeURIComponent(cp)}`, {
        signal: controller.signal
      });
      clearTimeout(t);
      if (res.ok) {
        const data = await res.json();
        const places = Array.isArray(data?.places) ? data.places : [];
        if (places.length > 0) {
          const estado = this.normalizarEstado(places[0].state);
          const colonias = [...new Set(places.map(p => p['place name']).filter(Boolean))];
          if (estado) {
            return { estado, municipio: '', colonias };
          }
        }
      }
    } catch (_) {
      // Sin conexión o timeout
    }

    return null;
  },

  aplicarResultadoPostal({ estado, municipio, colonias }) {
    const estadoSelect = document.getElementById('register-estado');
    if (estadoSelect && estado) {
      estadoSelect.value = estado;
      this.poblarMunicipios(estado, municipio);
      this.poblarColonias(colonias && colonias.length > 0 ? colonias : [], true);
    }
  },

  onEstadoChange() {
    const estado = document.getElementById('register-estado')?.value || '';
    this.poblarMunicipios(estado, '');
    this.poblarColonias([], false);
  },

  poblarMunicipios(estado, municipioPreseleccionado) {
    const munSelect = document.getElementById('register-municipio-select');
    const munInput = document.getElementById('register-municipio');
    if (!munSelect) return;

    const municipiosObj = this.CATALOGO[estado] || {};
    const lista = Object.keys(municipiosObj);

    if (municipioPreseleccionado && !lista.includes(municipioPreseleccionado)) {
      lista.unshift(municipioPreseleccionado);
    }

    munSelect.innerHTML = `<option value="">Selecciona alcaldía / municipio</option>`;
    lista.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m;
      opt.textContent = m;
      munSelect.appendChild(opt);
    });

    const manualOpt = document.createElement('option');
    manualOpt.value = this.MANUAL_VALUE;
    manualOpt.textContent = 'Otra / Ingresar manualmente';
    munSelect.appendChild(manualOpt);

    if (municipioPreseleccionado) {
      munSelect.value = municipioPreseleccionado;
      if (munInput) {
        munInput.value = municipioPreseleccionado;
        munInput.style.display = 'none';
      }
    } else {
      munSelect.value = '';
      if (munInput) {
        munInput.value = '';
        munInput.style.display = 'none';
      }
    }
  },

  onMunicipioChange() {
    const estado = document.getElementById('register-estado')?.value || '';
    const munSelect = document.getElementById('register-municipio-select');
    const munInput = document.getElementById('register-municipio');
    const cpInput = document.getElementById('register-codigoPostal');
    if (!munSelect) return;

    const val = munSelect.value;
    if (val === this.MANUAL_VALUE) {
      if (munInput) {
        munInput.value = '';
        munInput.style.display = 'block';
        munInput.focus();
      }
      this.poblarColonias([], false);
      return;
    }

    if (munInput) {
      munInput.value = val;
      munInput.style.display = 'none';
    }

    const info = this.CATALOGO[estado]?.[val];
    if (info) {
      this.poblarColonias(info.colonias, true);
      if (cpInput && !cpInput.value.trim() && info.cp) {
        cpInput.value = info.cp;
        this._ultimoCpConsultado = info.cp;
        this.setCpStatus(`C.P. sugerido (${info.cp}) para ${val}`, 'info');
      }
    } else {
      this.poblarColonias([], false);
    }
  },

  poblarColonias(colonias, autoSelectFirst) {
    const colSelect = document.getElementById('register-colonia-select');
    const colInput = document.getElementById('register-colonia');
    if (!colSelect) return;

    colSelect.innerHTML = `<option value="">Selecciona colonia</option>`;
    (colonias || []).forEach(c => {
      const opt = document.createElement('option');
      opt.value = c;
      opt.textContent = c;
      colSelect.appendChild(opt);
    });

    const manualOpt = document.createElement('option');
    manualOpt.value = this.MANUAL_VALUE;
    manualOpt.textContent = 'Otra / Ingresar manualmente';
    colSelect.appendChild(manualOpt);

    if (autoSelectFirst && colonias && colonias.length > 0) {
      colSelect.value = colonias[0];
      if (colInput) {
        colInput.value = colonias[0];
        colInput.style.display = 'none';
      }
    } else {
      colSelect.value = '';
      if (colInput) {
        colInput.value = '';
        colInput.style.display = 'none';
      }
    }
  },

  onColoniaChange() {
    const colSelect = document.getElementById('register-colonia-select');
    const colInput = document.getElementById('register-colonia');
    if (!colSelect) return;

    if (colSelect.value === this.MANUAL_VALUE) {
      if (colInput) {
        colInput.value = '';
        colInput.style.display = 'block';
        colInput.focus();
      }
    } else if (colInput) {
      colInput.value = colSelect.value;
      colInput.style.display = 'none';
    }
  },

  getMunicipio() {
    const sel = document.getElementById('register-municipio-select')?.value || '';
    if (sel === this.MANUAL_VALUE || !sel) {
      return (document.getElementById('register-municipio')?.value || '').trim();
    }
    return sel.trim();
  },

  getColonia() {
    const sel = document.getElementById('register-colonia-select')?.value || '';
    if (sel === this.MANUAL_VALUE || !sel) {
      return (document.getElementById('register-colonia')?.value || '').trim();
    }
    return sel.trim();
  }
};
