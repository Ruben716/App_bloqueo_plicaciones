import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';

/// Tema visual de las pantallas de bloqueo.
///
/// Debe coincidir con `LockTheme.kt` del motor nativo para que el bloqueo
/// dentro de la app y el overlay sobre otras apps se vean idénticos.
@immutable
class LockThemeData {
  const LockThemeData({
    required this.id,
    required this.name,
    required this.backgroundTop,
    required this.backgroundBottom,
    required this.accent,
  });

  final String id;
  final String name;
  final Color backgroundTop;
  final Color backgroundBottom;
  final Color accent;

  LinearGradient get gradient => LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [backgroundTop, backgroundBottom],
  );
}

abstract final class LockThemes {
  static const aurora = LockThemeData(
    id: 'aurora',
    name: 'Aurora',
    backgroundTop: Color(0xFF1E1B4B),
    backgroundBottom: Color(0xFF4C1D95),
    accent: Color(0xFFA78BFA),
  );
  static const ocean = LockThemeData(
    id: 'ocean',
    name: 'Océano',
    backgroundTop: Color(0xFF082F49),
    backgroundBottom: Color(0xFF134E4A),
    accent: Color(0xFF38BDF8),
  );
  static const forest = LockThemeData(
    id: 'forest',
    name: 'Bosque',
    backgroundTop: Color(0xFF052E16),
    backgroundBottom: Color(0xFF14532D),
    accent: Color(0xFF4ADE80),
  );
  static const sunset = LockThemeData(
    id: 'sunset',
    name: 'Atardecer',
    backgroundTop: Color(0xFF431407),
    backgroundBottom: Color(0xFF831843),
    accent: Color(0xFFFB923C),
  );
  static const midnight = LockThemeData(
    id: 'midnight',
    name: 'Medianoche',
    backgroundTop: Color(0xFF020617),
    backgroundBottom: Color(0xFF1E293B),
    accent: Color(0xFF94A3B8),
  );

  static const all = [aurora, ocean, forest, sunset, midnight];

  static LockThemeData byId(String? id) => all.firstWhere((t) => t.id == id, orElse: () => aurora);
}
