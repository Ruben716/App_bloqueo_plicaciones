import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Reglas del patrón 3x3, idénticas a `PatternGeometry.kt` del motor nativo:
/// nodos 0..8 por filas; si se une a -> b saltando un nodo intermedio no
/// usado (p. ej. 0 -> 2), ese nodo se añade automáticamente.
abstract final class PatternGeometry {
  static int? middleNode(int from, int to) {
    final r1 = from ~/ 3, c1 = from % 3, r2 = to ~/ 3, c2 = to % 3;
    if ((r1 + r2).isOdd || (c1 + c2).isOdd) return null;
    final middle = ((r1 + r2) ~/ 2) * 3 + (c1 + c2) ~/ 2;
    return (middle == from || middle == to) ? null : middle;
  }

  static void append(List<int> path, int node) {
    if (path.contains(node)) return;
    if (path.isNotEmpty) {
      final middle = middleNode(path.last, node);
      if (middle != null && !path.contains(middle)) path.add(middle);
    }
    path.add(node);
  }

  static String encode(List<int> path) => path.join();
}

/// Cuadrícula 3x3 para dibujar un patrón.
class PatternLock extends StatefulWidget {
  const PatternLock({
    required this.onCompleted,
    this.color,
    this.error = false,
    this.enabled = true,
    this.size = 280,
    super.key,
  });

  /// Se llama al levantar el dedo con la secuencia de nodos (0..8).
  final ValueChanged<List<int>> onCompleted;
  final Color? color;
  final bool error;
  final bool enabled;
  final double size;

  @override
  State<PatternLock> createState() => _PatternLockState();
}

class _PatternLockState extends State<PatternLock> {
  final List<int> _path = [];
  Offset? _finger;

  @override
  void didUpdateWidget(PatternLock oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Al salir del estado de error se limpia el dibujo anterior.
    if (oldWidget.error && !widget.error) {
      _path.clear();
      _finger = null;
    }
  }

  Offset _center(int index, double size) {
    final cell = size / 3;
    return Offset(cell * (index % 3) + cell / 2, cell * (index ~/ 3) + cell / 2);
  }

  void _track(Offset position) {
    final hitRadius = widget.size / 3 * 0.38;
    for (var i = 0; i < 9; i++) {
      if (_path.contains(i)) continue;
      if ((_center(i, widget.size) - position).distance <= hitRadius) {
        PatternGeometry.append(_path, i);
        HapticFeedback.selectionClick();
        break;
      }
    }
    setState(() => _finger = position);
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final accent = widget.error ? scheme.error : (widget.color ?? scheme.primary);
    return Semantics(
      label: 'Cuadrícula de patrón de 3 por 3',
      child: SizedBox.square(
        dimension: widget.size,
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onPanStart: widget.enabled
              ? (details) {
                  setState(_path.clear);
                  _track(details.localPosition);
                }
              : null,
          onPanUpdate: widget.enabled ? (d) => _track(d.localPosition) : null,
          onPanEnd: widget.enabled
              ? (_) {
                  setState(() => _finger = null);
                  if (_path.isNotEmpty) widget.onCompleted(List.of(_path));
                }
              : null,
          child: CustomPaint(
            painter: _PatternPainter(
              path: List.of(_path),
              finger: _finger,
              accent: accent,
              idle: (widget.color ?? scheme.onSurface).withValues(alpha: 0.7),
              center: (i) => _center(i, widget.size),
            ),
          ),
        ),
      ),
    );
  }
}

class _PatternPainter extends CustomPainter {
  _PatternPainter({
    required this.path,
    required this.finger,
    required this.accent,
    required this.idle,
    required this.center,
  });

  final List<int> path;
  final Offset? finger;
  final Color accent;
  final Color idle;
  final Offset Function(int) center;

  @override
  void paint(Canvas canvas, Size size) {
    final line = Paint()
      ..color = accent.withValues(alpha: 0.85)
      ..strokeWidth = 4
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;
    for (var i = 1; i < path.length; i++) {
      canvas.drawLine(center(path[i - 1]), center(path[i]), line);
    }
    if (finger != null && path.isNotEmpty) {
      canvas.drawLine(center(path.last), finger!, line);
    }

    final dotRadius = math.max(5.0, size.width / 50);
    for (var i = 0; i < 9; i++) {
      final selected = path.contains(i);
      final c = center(i);
      canvas.drawCircle(
        c,
        selected ? dotRadius * 1.4 : dotRadius,
        Paint()..color = selected ? accent : idle,
      );
      if (selected) {
        canvas.drawCircle(
          c,
          dotRadius * 4,
          Paint()
            ..color = accent.withValues(alpha: 0.35)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2,
        );
      }
    }
  }

  @override
  bool shouldRepaint(_PatternPainter old) =>
      old.path.length != path.length ||
      old.finger != finger ||
      old.accent != accent ||
      old.idle != idle;
}
