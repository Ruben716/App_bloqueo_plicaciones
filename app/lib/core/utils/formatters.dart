/// Formatos de fecha y hora en español sin depender de `intl`.
abstract final class Formatters {
  static const _months = [
    'ene',
    'feb',
    'mar',
    'abr',
    'may',
    'jun',
    'jul',
    'ago',
    'sept',
    'oct',
    'nov',
    'dic',
  ];

  /// Iniciales ISO-8601: índice 0 = lunes.
  static const dayInitials = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
  static const dayNames = [
    'lunes',
    'martes',
    'miércoles',
    'jueves',
    'viernes',
    'sábado',
    'domingo',
  ];

  static String twoDigits(int n) => n.toString().padLeft(2, '0');

  static String time(DateTime t) => '${twoDigits(t.hour)}:${twoDigits(t.minute)}';

  static String minuteOfDay(int minutes) =>
      '${twoDigits(minutes ~/ 60)}:${twoDigits(minutes % 60)}';

  /// "Hoy, 14:05", "Ayer, 09:12", "12 oct, 10:00" o "12 oct 2024, 10:00".
  static String relative(DateTime t, {DateTime? now}) {
    final reference = now ?? DateTime.now();
    final today = DateTime(reference.year, reference.month, reference.day);
    final day = DateTime(t.year, t.month, t.day);
    final diff = today.difference(day).inDays;
    if (diff == 0) return 'Hoy, ${time(t)}';
    if (diff == 1) return 'Ayer, ${time(t)}';
    final date = '${t.day} ${_months[t.month - 1]}';
    if (t.year == reference.year) return '$date, ${time(t)}';
    return '$date ${t.year}, ${time(t)}';
  }

  /// Resumen de días: "Todos los días", "Lunes a viernes", "Fines de semana"
  /// o "L, X, V".
  static String days(Iterable<int> isoDays) {
    final set = isoDays.toSet();
    if (set.length == 7) return 'Todos los días';
    if (set.isEmpty) return 'Ningún día';
    if (set.length == 5 && set.containsAll(const [1, 2, 3, 4, 5])) {
      return 'Lunes a viernes';
    }
    if (set.length == 2 && set.containsAll(const [6, 7])) return 'Fines de semana';
    final sorted = set.toList()..sort();
    return sorted.map((d) => dayInitials[d - 1]).join(', ');
  }

  static String duration(Duration d) {
    if (d.inMinutes >= 1) {
      final seconds = d.inSeconds % 60;
      return seconds == 0 ? '${d.inMinutes} min' : '${d.inMinutes} min $seconds s';
    }
    return '${d.inSeconds} s';
  }
}
