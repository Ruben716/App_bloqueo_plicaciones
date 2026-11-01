import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/routes.dart';
import 'widgets/recovery_code_card.dart';

/// Paso 2 del onboarding: guardar el código de recuperación.
class RecoveryCodePage extends StatefulWidget {
  const RecoveryCodePage({required this.code, super.key});

  final String? code;

  @override
  State<RecoveryCodePage> createState() => _RecoveryCodePageState();
}

class _RecoveryCodePageState extends State<RecoveryCodePage> {
  bool _saved = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final code = widget.code;
    return Scaffold(
      appBar: AppBar(title: const Text('Paso 2 de 4'), automaticallyImplyLeading: false),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(Space.xl),
          children: [
            Text('Tu código de recuperación', style: theme.textTheme.headlineMedium),
            const SizedBox(height: Space.sm),
            Text(
              'Como tu credencial es independiente del bloqueo del teléfono, '
              'este código es la única forma de recuperar el acceso si la olvidas.',
              style: theme.textTheme.bodyLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: Space.xl),
            if (code != null)
              RecoveryCodeCard(code: code)
            else
              const Card(
                child: ListTile(
                  leading: Icon(Icons.info_outline_rounded),
                  title: Text('Ya tienes un código de recuperación configurado.'),
                ),
              ),
            const SizedBox(height: Space.lg),
            const _Tip(
              icon: Icons.edit_note_rounded,
              text: 'Apúntalo en papel o en tu gestor de contraseñas.',
            ),
            const _Tip(
              icon: Icons.no_photography_rounded,
              text: 'No lo guardes como captura en este mismo teléfono.',
            ),
            const SizedBox(height: Space.lg),
            if (code != null)
              CheckboxListTile(
                value: _saved,
                onChanged: (value) => setState(() => _saved = value ?? false),
                title: const Text('Lo he guardado en un lugar seguro'),
                controlAffinity: ListTileControlAffinity.leading,
              ),
            const SizedBox(height: Space.lg),
            FilledButton(
              onPressed: code == null || _saved
                  ? () => context.go(Routes.onboardingPermissions)
                  : null,
              child: const Text('Continuar'),
            ),
          ],
        ),
      ),
    );
  }
}

class _Tip extends StatelessWidget {
  const _Tip({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
      title: Text(text),
    );
  }
}
