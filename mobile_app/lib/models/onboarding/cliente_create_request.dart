// Request DTO matching Spring Boot backend (POST /clientes) with @JsonIgnoreProperties(ignoreUnknown = false)
// All fields use the exact JSON keys required by the backend.

class ClienteCreateRequest {
  final String primerNombre;
  final String? segundoNombre;
  final String apellidoPaterno;
  final String apellidoMaterno;
  final String curp; // 18 uppercase chars
  final String rfc; // 12‑13 uppercase chars
  final String correo;
  final String fechaNacimiento; // YYYY‑MM‑DD
  final String telefonoMovil; // 10 digits
  final String? telefonoAlternativo;
  final String sexo; // "M" or "H"
  final String estadoCivil; // "S", "C", "D", "V" o "U"
  final String nacionalidad;
  final String ocupacion;
  final String empresa;
  final double ingresoMensual; // > 0
  final DomicilioDto domicilio;
  final String password; // min 8 chars, upper, lower, digit, special

  const ClienteCreateRequest({
    required this.primerNombre,
    this.segundoNombre,
    required this.apellidoPaterno,
    required this.apellidoMaterno,
    required this.curp,
    required this.rfc,
    required this.correo,
    required this.fechaNacimiento,
    required this.telefonoMovil,
    this.telefonoAlternativo,
    required this.sexo,
    required this.estadoCivil,
    required this.nacionalidad,
    required this.ocupacion,
    required this.empresa,
    required this.ingresoMensual,
    required this.domicilio,
    required this.password,
  });

  Map<String, dynamic> toJson() => {
        'primerNombre': primerNombre,
        'segundoNombre': (segundoNombre?.isEmpty ?? true) ? null : segundoNombre,
        'apellidoPaterno': apellidoPaterno,
        'apellidoMaterno': apellidoMaterno,
        'curp': curp,
        'rfc': rfc,
        'correo': correo,
        'fechaNacimiento': fechaNacimiento,
        'telefonoMovil': telefonoMovil,
        'telefonoAlternativo': (telefonoAlternativo?.isEmpty ?? true) ? null : telefonoAlternativo,
        'sexo': sexo,
        'estadoCivil': estadoCivil,
        'nacionalidad': nacionalidad,
        'ocupacion': ocupacion,
        'empresa': empresa,
        'ingresoMensual': ingresoMensual,
        'domicilio': domicilio.toJson(),
        'password': password,
      };
}

class DomicilioDto {
  final String calle;
  final String numeroExterior;
  final String? numeroInterior;
  final String colonia;
  final String municipio;
  final String estado;
  final String pais;
  final String codigoPostal;

  const DomicilioDto({
    required this.calle,
    required this.numeroExterior,
    this.numeroInterior,
    required this.colonia,
    required this.municipio,
    required this.estado,
    required this.pais,
    required this.codigoPostal,
  });

  Map<String, dynamic> toJson() => {
        'calle': calle,
        'numeroExterior': numeroExterior,
        'numeroInterior': (numeroInterior?.isEmpty ?? true) ? null : numeroInterior,
        'colonia': colonia,
        'municipio': municipio,
        'estado': estado,
        'pais': pais,
        'codigoPostal': codigoPostal,
      };
}
