import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Muestra el código de recuperación con opción de copiarlo.
class RecoveryCodeCard extends StatelessWidget {
  const RecoveryCodeCard({required this.code, super.key});

  final String code;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      color: theme.colorScheme.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(Space.xl),
        child: Column(
          children: [
            Icon(Icons.key_rounded, size: 32, color: theme.colorScheme.onPrimaryContainer),
            const SizedBox(height: Space.md),
            SelectableText(
              code,
              textAlign: TextAlign.center,
              style: theme.textTheme.headlineMedium?.copyWith(
                fontFamily: 'monospace',
                letterSpacing: 3,
                color: theme.colorScheme.onPrimaryContainer,
              ),
            ),
            const SizedBox(height: Space.md),
            TextButton.icon(
              onPressed: () {
                Clipboard.setData(ClipboardData(text: code));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Código copiado. Guárdalo en un lugar seguro.')),
                );
              },
              icon: const Icon(Icons.copy_rounded),
              label: const Text('Copiar'),
            ),
          ],
        ),
      ),
    );
  }
}

/// Diálogo que obliga a confirmar que se guardó el código.
Future<void> showRecoveryCodeDialog(BuildContext context, String code) {
  return showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (context) => AlertDialog(
      title: const Text('Nuevo código de recuperación'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          RecoveryCodeCard(code: code),
          const SizedBox(height: Space.lg),
          const Text(
            'El código anterior ya no sirve. Guarda este fuera del teléfono: es la '
            'única forma de entrar si olvidas tu credencial.',
          ),
        ],
      ),
      actions: [
        FilledButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Lo he guardado'),
        ),
      ],
    ),
  );
}
