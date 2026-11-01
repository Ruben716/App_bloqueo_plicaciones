import 'package:flutter/material.dart';

/// Indicador de dígitos introducidos.
class PinDots extends StatelessWidget {
  const PinDots({
    required this.filled,
    required this.total,
    this.color,
    this.error = false,
    super.key,
  });

  final int filled;
  final int total;
  final Color? color;
  final bool error;

  @override
  Widget build(BuildContext context) {
    final base = error
        ? Theme.of(context).colorScheme.error
        : (color ?? Theme.of(context).colorScheme.onSurface);
    final count = filled > total ? filled : total;
    return Semantics(
      label: '$filled de $total dígitos',
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 0; i < count; i++)
            AnimatedContainer(
              duration: const Duration(milliseconds: 160),
              curve: Curves.easeOut,
              margin: const EdgeInsets.symmetric(horizontal: 9),
              width: i < filled ? 16 : 14,
              height: i < filled ? 16 : 14,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: i < filled ? base : Colors.transparent,
                border: Border.all(color: base.withValues(alpha: 0.6), width: 1.5),
              ),
            ),
        ],
      ),
    );
  }
}
