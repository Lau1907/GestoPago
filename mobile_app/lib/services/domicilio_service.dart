import 'dart:convert';
import 'package:http/http.dart' as http;

/// Resultado de búsqueda de código postal
class ResultadoPostal {
  final String estado;
  final String municipio;
  final List<String> colonias;

  const ResultadoPostal({
    required this.estado,
    required this.municipio,
    required this.colonias,
  });
}

/// Servicio de autocompletado de domicilio para México
/// Busca primero en catálogo local, luego en SEPOMEX API y fallback Zippopotam.us
class DomicilioService {
  static const String _manualValue = '__MANUAL__';
  static String get manualValue => _manualValue;

  /// Catálogo base de respaldo (estados/municipios/colonias)
  static const Map<String, Map<String, Map<String, dynamic>>> catalogo = {
    'Aguascalientes': {
      'Aguascalientes': {'cp': '20000', 'colonias': ['Centro', 'Jardines de la Asunción', 'Las Américas', 'Pulgas Pandas']},
      'Jesús María': {'cp': '20900', 'colonias': ['Centro', 'Maravillas', 'Paseos de Aguascalientes']},
      'Calvillo': {'cp': '20800', 'colonias': ['Centro', 'Liberal', 'López Mateos']},
    },
    'Baja California': {
      'Tijuana': {'cp': '22000', 'colonias': ['Zona Centro', 'Zona Río', 'Playas de Tijuana', 'Otay']},
      'Mexicali': {'cp': '21000', 'colonias': ['Centro Cívico', 'Nueva', 'Justo Sierra']},
      'Ensenada': {'cp': '22800', 'colonias': ['Zona Centro', 'Valle Dorado', 'Chapultepec']},
    },
    'Baja California Sur': {
      'La Paz': {'cp': '23000', 'colonias': ['Centro', 'Esterito', 'Fidepaz']},
      'Los Cabos': {'cp': '23400', 'colonias': ['San José del Cabo Centro', 'El Médano', 'Cabo San Lucas Centro']},
    },
    'Campeche': {
      'Campeche': {'cp': '24000', 'colonias': ['Centro Histórico', 'San Román', 'Guadalupe']},
      'Carmen': {'cp': '24100', 'colonias': ['Centro', 'Playa Norte', 'Justo Sierra']},
    },
    'Chiapas': {
      'Tuxtla Gutiérrez': {'cp': '29000', 'colonias': ['Centro', 'Terán', 'Moctezuma', 'Las Palmas']},
      'San Cristóbal de las Casas': {'cp': '29200', 'colonias': ['Centro', 'Guadalupe', 'El Cerrillo']},
      'Tapachula': {'cp': '30700', 'colonias': ['Centro', '5 de Febrero', 'Los Naranjos']},
    },
    'Chihuahua': {
      'Chihuahua': {'cp': '31000', 'colonias': ['Centro', 'San Felipe', 'Quintas del Sol']},
      'Juárez': {'cp': '32000', 'colonias': ['Zona Centro', 'Pronaf', 'Campos Elíseos']},
    },
    'Ciudad de México': {
      'Benito Juárez': {'cp': '03100', 'colonias': ['Del Valle Centro', 'Del Valle Norte', 'Narvarte Poniente', 'Nápoles', 'Portales']},
      'Cuauhtémoc': {'cp': '06600', 'colonias': ['Juárez', 'Roma Norte', 'Condesa', 'Centro', 'Doctores']},
      'Miguel Hidalgo': {'cp': '11560', 'colonias': ['Polanco', 'Anzures', 'Lomas de Chapultepec', 'Escandón']},
      'Coyoacán': {'cp': '04000', 'colonias': ['Villa Coyoacán', 'Del Carmen', 'Copilco', 'Pedregal de Santo Domingo']},
      'Álvaro Obregón': {'cp': '01000', 'colonias': ['San Ángel', 'Florida', 'Guadalupe Inn', 'Santa Fe']},
      'Iztapalapa': {'cp': '09000', 'colonias': ['San Pablo', 'Leyes de Reforma', 'Santa Cruz Meyehualco']},
      'Gustavo A. Madero': {'cp': '07000', 'colonias': ['Lindavista', 'Villa de Guadalupe', 'Tepeyac Insurgentes']},
      'Tlalpan': {'cp': '14000', 'colonias': ['Tlalpan Centro', 'Coapa', 'Pedregal de Carrasco']},
    },
    'Coahuila': {
      'Saltillo': {'cp': '25000', 'colonias': ['Zona Centro', 'República', 'Doctores']},
      'Torreón': {'cp': '27000', 'colonias': ['Centro', 'Campestre La Rosita', 'Los Ángeles']},
    },
    'Colima': {
      'Colima': {'cp': '28000', 'colonias': ['Centro', 'Lomas de Circunvalación', 'Jardines Vista Hermosa']},
      'Manzanillo': {'cp': '28200', 'colonias': ['Centro', 'Salagua', 'Santiago']},
    },
    'Durango': {
      'Durango': {'cp': '34000', 'colonias': ['Zona Centro', 'Guadalupe', 'Los Remedios']},
      'Gómez Palacio': {'cp': '35000', 'colonias': ['Centro', 'Las Rosas', 'Campestre']},
    },
    'Estado de México': {
      'Naucalpan de Juárez': {'cp': '53100', 'colonias': ['Ciudad Satélite', 'Lomas Verdes', 'Naucalpan Centro']},
      'Tlalnepantla de Baz': {'cp': '54000', 'colonias': ['Tlalnepantla Centro', 'Valle Dorado', 'Viveros de la Loma']},
      'Ecatepec de Morelos': {'cp': '55000', 'colonias': ['San Cristóbal Centro', 'Jardines de Morelos', 'Las Américas']},
      'Toluca': {'cp': '50000', 'colonias': ['Centro', 'Universidad', 'Morelos']},
      'Huixquilucan': {'cp': '52760', 'colonias': ['Interlomas', 'Tecamachalco', 'La Herradura']},
    },
    'Guanajuato': {
      'León': {'cp': '37000', 'colonias': ['Centro', 'Jardines del Moral', 'Panorama']},
      'Guanajuato': {'cp': '36000', 'colonias': ['Centro', 'Marfil', 'Pastita']},
      'Irapuato': {'cp': '36500', 'colonias': ['Centro', 'Las Reynas', 'Moderna']},
    },
    'Guerrero': {
      'Acapulco de Juárez': {'cp': '39300', 'colonias': ['Centro', 'Costa Azul', 'Magallanes']},
      'Chilpancingo de los Bravo': {'cp': '39000', 'colonias': ['Centro', 'Universal', 'Guerrero']},
    },
    'Hidalgo': {
      'Pachuca de Soto': {'cp': '42000', 'colonias': ['Centro', 'Periodistas', 'Zona Plateada']},
      'Tulancingo de Bravo': {'cp': '43600', 'colonias': ['Centro', 'Jardines del Sur', 'Vicente Guerrero']},
    },
    'Jalisco': {
      'Guadalajara': {'cp': '44100', 'colonias': ['Centro', 'Americana', 'Providencia', 'Chapalita']},
      'Zapopan': {'cp': '45050', 'colonias': ['Zapopan Centro', 'Puerta de Hierro', 'Ciudad del Sol', 'Valle Real']},
      'Puerto Vallarta': {'cp': '48300', 'colonias': ['Centro', '5 de Diciembre', 'Marina Vallarta']},
    },
    'Michoacán': {
      'Morelia': {'cp': '58000', 'colonias': ['Centro Histórico', 'Chapultepec Norte', 'Las Américas']},
      'Uruapan': {'cp': '60000', 'colonias': ['Centro', 'Morelos', 'La Quinta']},
    },
    'Morelos': {
      'Cuernavaca': {'cp': '62000', 'colonias': ['Centro', 'Vista Hermosa', 'Lomas de la Selva']},
      'Jiutepec': {'cp': '62550', 'colonias': ['Centro', 'Civac', 'Tejalpa']},
    },
    'Nayarit': {
      'Tepic': {'cp': '63000', 'colonias': ['Centro', 'Ciudad del Valle', 'Los Llanitos']},
      'Bahía de Banderas': {'cp': '63732', 'colonias': ['Nuevo Vallarta', 'Bucerías', 'Mezcales']},
    },
    'Nuevo León': {
      'Monterrey': {'cp': '64000', 'colonias': ['Centro', 'Obispado', 'Cumbres', 'Contry']},
      'San Pedro Garza García': {'cp': '66220', 'colonias': ['Del Valle', 'Valle Oriente', 'San Agustín']},
      'San Nicolás de los Garza': {'cp': '66400', 'colonias': ['Centro', 'Anáhuac', 'Las Puentes']},
    },
    'Oaxaca': {
      'Oaxaca de Juárez': {'cp': '68000', 'colonias': ['Centro', 'Reforma', 'Jalatlaco']},
      'Salina Cruz': {'cp': '70600', 'colonias': ['Centro', 'Hidalgo Poniente', 'Barrio Nuevo']},
    },
    'Puebla': {
      'Puebla': {'cp': '72000', 'colonias': ['Centro Histórico', 'La Paz', 'Angelópolis', 'Huexotitla']},
      'San Andrés Cholula': {'cp': '72810', 'colonias': ['Centro', 'Lomas de Angelópolis', 'Tlaxcalancingo']},
    },
    'Querétaro': {
      'Querétaro': {'cp': '76000', 'colonias': ['Centro Histórico', 'Juriquilla', 'Álamos', 'El Campanario']},
      'San Juan del Río': {'cp': '76800', 'colonias': ['Centro', 'San Cayetano', 'Indeco']},
    },
    'Quintana Roo': {
      'Benito Juárez': {'cp': '77500', 'colonias': ['Centro', 'Supermanzana 15', 'Zona Hotelera', 'Alfredo V. Bonfil']},
      'Solidaridad': {'cp': '77710', 'colonias': ['Playa del Carmen Centro', 'Playacar', 'Ejidal']},
      'Othón P. Blanco': {'cp': '77000', 'colonias': ['Chetumal Centro', 'Adolfo López Mateos', 'Forjadores']},
    },
    'San Luis Potosí': {
      'San Luis Potosí': {'cp': '78000', 'colonias': ['Centro Histórico', 'Lomas', 'Del Valle']},
      'Soledad de Graciano Sánchez': {'cp': '78430', 'colonias': ['Cabecera Municipal', 'Hogares Ferrocarrileros']},
    },
    'Sinaloa': {
      'Culiacán': {'cp': '80000', 'colonias': ['Centro', 'Tres Ríos', 'Chapultepec']},
      'Mazatlán': {'cp': '82000', 'colonias': ['Centro', 'Zona Dorada', 'Sábalo Country']},
    },
    'Sonora': {
      'Hermosillo': {'cp': '83000', 'colonias': ['Centro', 'Pitic', 'Villa Satélite']},
      'Cajeme': {'cp': '85000', 'colonias': ['Ciudad Obregón Centro', 'Norte', 'Cumuripa']},
    },
    'Tabasco': {
      'Centro': {'cp': '86000', 'colonias': ['Villahermosa Centro', 'Tabasco 2000', 'Atasta']},
      'Cárdenas': {'cp': '86500', 'colonias': ['Centro', 'Pueblo Nuevo']},
    },
    'Tamaulipas': {
      'Reynosa': {'cp': '88500', 'colonias': ['Zona Centro', 'Del Valle', 'Las Fuentes']},
      'Tampico': {'cp': '89000', 'colonias': ['Zona Centro', 'Lomas de Rosales', 'Petrolera']},
    },
    'Tlaxcala': {
      'Tlaxcala': {'cp': '90000', 'colonias': ['Centro', 'La Loma Xicohténcatl', 'Ocotlán']},
      'Apizaco': {'cp': '90300', 'colonias': ['Centro', 'Fátima', 'Ferrocarrilera']},
    },
    'Veracruz': {
      'Veracruz': {'cp': '91700', 'colonias': ['Centro', 'Reforma', 'Zaragoza']},
      'Boca del Río': {'cp': '94290', 'colonias': ['Cabecera Municipal', 'Costa de Oro', 'Mocambo']},
      'Xalapa': {'cp': '91000', 'colonias': ['Zona Centro', 'Ánimas', 'Macuiltépetl']},
    },
    'Yucatán': {
      'Mérida': {'cp': '97000', 'colonias': ['Centro', 'García Ginerés', 'Montejo', 'Altabrisa']},
      'Valladolid': {'cp': '97780', 'colonias': ['Centro', 'Candelaria', 'Santa Ana']},
    },
    'Zacatecas': {
      'Zacatecas': {'cp': '98000', 'colonias': ['Centro Histórico', 'Lomas de la Soledad', 'Tres Cruces']},
      'Guadalupe': {'cp': '98600', 'colonias': ['Centro', 'Tierra y Libertad', 'Villas de Guadalupe']},
    },
  };

  /// Busca por código postal (5 dígitos).
  /// 1. Catálogo local (instantáneo)
  /// 2. SEPOMEX API con timeout 3.5s
  /// 3. Zippopotam.us con timeout 3.5s
  static Future<ResultadoPostal?> buscarPorCodigoPostal(String cp) async {
    if (cp.length != 5) return null;

    // 1. Catálogo local
    for (final estado in catalogo.entries) {
      for (final municipio in estado.value.entries) {
        final info = municipio.value;
        if (info['cp'] == cp) {
          return ResultadoPostal(
            estado: estado.key,
            municipio: municipio.key,
            colonias: List<String>.from(info['colonias'] as List),
          );
        }
      }
    }

    // 2. SEPOMEX API
    try {
      final res = await http.get(
        Uri.parse('https://sepomex.icalialabs.com/api/v1/zip_codes?zip_code=$cp'),
      ).timeout(const Duration(milliseconds: 3500));

      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        final items = (data['zip_codes'] as List?) ?? [];
        if (items.isNotEmpty) {
          final first = items.first as Map<String, dynamic>;
          final estado = _normalizarEstado(first['d_estado']?.toString() ?? '');
          final municipio = first['d_mnpio']?.toString() ?? '';
          final colonias = items
              .map((i) => (i as Map<String, dynamic>)['d_asenta']?.toString())
              .whereType<String>()
              .toSet()
              .toList();
          if (estado.isNotEmpty) {
            return ResultadoPostal(estado: estado, municipio: municipio, colonias: colonias);
          }
        }
      }
    } catch (_) {
      // timeout o error de red → siguiente fallback
    }

    // 3. Zippopotam.us
    try {
      final res = await http.get(
        Uri.parse('https://api.zippopotam.us/mx/$cp'),
      ).timeout(const Duration(milliseconds: 3500));

      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        final places = (data['places'] as List?) ?? [];
        if (places.isNotEmpty) {
          final first = places.first as Map<String, dynamic>;
          final estado = _normalizarEstado(first['state']?.toString() ?? '');
          final colonias = places
              .map((p) => (p as Map<String, dynamic>)['place name']?.toString())
              .whereType<String>()
              .toSet()
              .toList();
          if (estado.isNotEmpty) {
            return ResultadoPostal(estado: estado, municipio: '', colonias: colonias);
          }
        }
      }
    } catch (_) {
      // sin conexión
    }

    return null;
  }

  /// Normaliza el nombre de un estado a su forma canónica del catálogo
  static String _normalizarEstado(String nombre) {
    if (nombre.isEmpty) return '';
    final clean = _removeDiacritics(nombre.trim().toLowerCase());

    if (clean.contains('distrito federal') || clean == 'cdmx' || clean.contains('ciudad de mexico')) {
      return 'Ciudad de México';
    }
    if (clean == 'mexico' || clean.contains('estado de mexico') || clean == 'edomex') {
      return 'Estado de México';
    }
    if (clean.contains('coahuila')) return 'Coahuila';
    if (clean.contains('michoacan')) return 'Michoacán';
    if (clean.contains('veracruz')) return 'Veracruz';

    for (final estado in catalogo.keys) {
      final estNorm = _removeDiacritics(estado.toLowerCase());
      if (estNorm == clean || clean.contains(estNorm) || estNorm.contains(clean)) {
        return estado;
      }
    }
    return '';
  }

  static String _removeDiacritics(String s) {
    const withDiacritics    = 'áéíóúüñÁÉÍÓÚÜÑ';
    const withoutDiacritics = 'aeiouunAEIOUUN';
    final result = StringBuffer();
    for (final ch in s.split('')) {
      final idx = withDiacritics.indexOf(ch);
      result.write(idx >= 0 ? withoutDiacritics[idx] : ch);
    }
    return result.toString();
  }

  /// Devuelve la lista de municipios para un estado dado
  static List<String> municipiosDe(String estado) {
    return catalogo[estado]?.keys.toList() ?? [];
  }

  /// Devuelve las colonias para un estado/municipio dados
  static List<String> coloniasDe(String estado, String municipio) {
    final info = catalogo[estado]?[municipio];
    if (info == null) return [];
    return List<String>.from(info['colonias'] as List);
  }

  /// Devuelve el CP del catálogo local para un estado/municipio dados (puede ser vacío)
  static String cpDe(String estado, String municipio) {
    return catalogo[estado]?[municipio]?['cp']?.toString() ?? '';
  }
}
