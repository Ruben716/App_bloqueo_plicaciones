import 'package:flutter/material.dart';

import '../tokens/lock_themes.dart';

/// Fondo degradado de las pantallas de bloqueo, con un halo del color de acento.
class GradientBackdrop extends StatelessWidget {
  const GradientBackdrop({required this.theme, required this.child, super.key});

  final LockThemeData theme;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(gradient: theme.gradient),
      child: Stack(
        children: [
          Positioned(
            top: -120,
            right: -80,
            child: _Glow(color: theme.accent.withValues(alpha: 0.25), size: 320),
          ),
          Positioned(
            bottom: -140,
            left: -100,
            child: _Glow(color: theme.accent.withValues(alpha: 0.15), size: 360),
          ),
          Positioned.fill(child: child),
        ],
      ),
    );
  }
}

class _Glow extends StatelessWidget {
  const _Glow({required this.color, required this.size});

  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: RadialGradient(colors: [color, color.withValues(alpha: 0)]),
        ),
      ),
    );
  }
}
