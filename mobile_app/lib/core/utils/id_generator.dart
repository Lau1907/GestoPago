import 'package:intl/intl.dart';
import 'package:uuid/uuid.dart';

class IdGenerator {
  static const Uuid _uuid = Uuid();

  /// Genera una clave de idempotencia única estricta conforme a los requerimientos
  /// del backend (entre 8 y 64 caracteres, formato TX-yyyyMMdd-XXXX).
  static String generateIdempotencyKey() {
    final now = DateTime.now();
    final datePrefix = DateFormat('yyyyMMdd').format(now);
    final randomPart = _uuid.v4().substring(0, 13).toUpperCase();
    return 'TX-$datePrefix-$randomPart';
  }
}
