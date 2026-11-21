import 'package:intl/intl.dart';

class Formatters {
  static final NumberFormat _currencyFormat = NumberFormat.currency(
    locale: 'es_MX',
    symbol: '\$',
    decimalDigits: 2,
  );

  static final DateFormat _dateFormat = DateFormat('dd/MM/yyyy HH:mm', 'es_MX');
  static final DateFormat _shortDateFormat = DateFormat('dd MMM yyyy', 'es_MX');

  /// Formatea fechas cortas (ej. 27 Oct 2026)
  static String formatShortDate(dynamic date) {
    if (date == null) return '-';
    if (date is DateTime) {
      return _shortDateFormat.format(date);
    }
    if (date is String) {
      try {
        final parsed = DateTime.parse(date);
        return _shortDateFormat.format(parsed);
      } catch (_) {
        return date;
      }
    }
    return date.toString();
  }

  /// Formatea cantidades a moneda mexicana ($150.00 MXN)
  static String formatCurrency(dynamic amount) {
    if (amount == null) return '\$0.00';
    if (amount is num) {
      return _currencyFormat.format(amount);
    }
    if (amount is String) {
      final parsed = double.tryParse(amount);
      if (parsed != null) {
        return _currencyFormat.format(parsed);
      }
    }
    return '\$$amount';
  }

  /// Formatea fechas ISO a legible
  static String formatDateTime(dynamic date) {
    if (date == null) return '-';
    if (date is DateTime) {
      return _dateFormat.format(date);
    }
    if (date is String) {
      try {
        final parsed = DateTime.parse(date);
        return _dateFormat.format(parsed);
      } catch (_) {
        return date;
      }
    }
    return date.toString();
  }

  /// Oculta dígitos de tarjeta/cuenta dejando solo los últimos 4 visibles
  static String maskAccount(String? account) {
    if (account == null || account.isEmpty) return '----------';
    if (account.length <= 4) return account;
    final lastFour = account.substring(account.length - 4);
    return '•••• •••• $lastFour';
  }
}
