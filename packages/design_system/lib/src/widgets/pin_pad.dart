import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Teclado numérico circular. Fila inferior: [acción opcional] 0 [borrar].
class PinPad extends StatelessWidget {
  const PinPad({
    required this.onDigit,
    required this.onDelete,
    this.leading,
    this.enabled = true,
    this.foregroundColor,
    super.key,
  });

  final ValueChanged<String> onDigit;
  final VoidCallback onDelete;

  /// Botón de la esquina inferior izquierda (p. ej. huella o "Aceptar").
  final Widget? leading;
  final bool enabled;
  final Color? foregroundColor;

  static const _keySize = 72.0;

  @override
  Widget build(BuildContext context) {
    final color = foregroundColor ?? Theme.of(context).colorScheme.onSurface;
    Widget digit(String value) =>
        _PadKey(label: value, color: color, enabled: enabled, onTap: () => onDigit(value));

    return AnimatedOpacity(
      opacity: enabled ? 1 : 0.4,
      duration: const Duration(milliseconds: 200),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final row in const [
            ['1', '2', '3'],
            ['4', '5', '6'],
            ['7', '8', '9'],
          ])
            _row(row.map(digit).toList()),
          _row([
            SizedBox.square(dimension: _keySize, child: leading),
            digit('0'),
            SizedBox.square(
              dimension: _keySize,
              child: IconButton(
                tooltip: 'Borrar',
                onPressed: enabled
                    ? () {
                        HapticFeedback.selectionClick();
                        onDelete();
                      }
                    : null,
                icon: Icon(Icons.backspace_outlined, color: color),
              ),
            ),
          ]),
        ],
      ),
    );
  }

  Widget _row(List<Widget> children) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 6),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (final child in children)
          Padding(padding: const EdgeInsets.symmetric(horizontal: 14), child: child),
      ],
    ),
  );
}

class _PadKey extends StatelessWidget {
  const _PadKey({
    required this.label,
    required this.color,
    required this.enabled,
    required this.onTap,
  });

  final String label;
  final Color color;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: PinPad._keySize,
      child: Material(
        color: color.withValues(alpha: 0.08),
        shape: const CircleBorder(),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: enabled
              ? () {
                  HapticFeedback.selectionClick();
                  onTap();
                }
              : null,
          child: Center(
            child: Text(
              label,
              style: Theme.of(context).textTheme.headlineMedium
                  ?.copyWith(color: color, fontWeight: FontWeight.w300),
            ),
          ),
        ),
      ),
    );
  }
}
