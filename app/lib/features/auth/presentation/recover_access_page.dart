import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/router/routes.dart';
import '../../../core/session/app_session.dart';
import '../../../core/utils/formatters.dart';
import 'widgets/credential_creator.dart';
import 'widgets/recovery_code_card.dart';

/// Recuperar el acceso con el código de recuperación y crear una credencial nueva.
class RecoverAccessPage extends ConsumerStatefulWidget {
  const RecoverAccessPage({super.key});

  @override
  ConsumerState<RecoverAccessPage> createState() => _RecoverAccessPageState();
}

class _RecoverAccessPageState extends ConsumerState<RecoverAccessPage> {
  static final _validChars = RegExp(r'[2-9A-HJ-NP-Z]');

  final _codeController = TextEditingController();
  String? _code;
  String? _error;

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  static String normalize(String input) =>
      input.toUpperCase().split('').where((c) => _validChars.hasMatch(c)).join();

  void _continue() {
    final normalized = normalize(_codeController.text);
    if (normalized.length != 12) {
      setState(() => _error = 'El código tiene 12 caracteres (XXXX-XXXX-XXXX).');
      return;
    }
    setState(() {
      _code = normalized;
      _error = null;
    });
  }

  Future<void> _reset(CredentialKind kind, String secret) async {
    final result = await ref
        .read(lockEngineApiProvider)
        .resetWithRecoveryCode(_code!, kind, secret);
    if (!mounted) return;
    if (!result.success) {
      final wait = result.lockoutRemainingMs > 0
          ? ' Espera ${Formatters.duration(Duration(milliseconds: result.lockoutRemainingMs))}.'
          : '';
      setState(() {
        _code = null;
        _error = '${result.errorMessage ?? 'No se pudo recuperar el acceso.'}$wait';
      });
      return;
    }
    final newCode = result.newRecoveryCode;
    if (newCode != null) await showRecoveryCodeDialog(context, newCode);
    ref.invalidate(engineStatusProvider);
    ref.read(appSessionProvider.notifier).unlock();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Recuperar acceso'),
        leading: BackButton(onPressed: () => context.go(Routes.lock)),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(Space.xl),
          child: _code != null
              ? CredentialCreator(headline: 'Crea tu nueva credencial', onCreated: _reset)
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Icon(Icons.key_rounded, size: 48, color: theme.colorScheme.primary),
                    const SizedBox(height: Space.lg),
                    Text(
                      'Introduce tu código de recuperación',
                      style: theme.textTheme.headlineMedium,
                    ),
                    const SizedBox(height: Space.sm),
                    Text(
                      'Es el código de 12 caracteres que guardaste al configurar Sigilo.',
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: Space.xl),
                    TextField(
                      controller: _codeController,
                      autofocus: true,
                      textCapitalization: TextCapitalization.characters,
                      autocorrect: false,
                      enableSuggestions: false,
                      inputFormatters: [LengthLimitingTextInputFormatter(16)],
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontFamily: 'monospace',
                        letterSpacing: 2,
                      ),
                      decoration: InputDecoration(
                        hintText: 'XXXX-XXXX-XXXX',
                        errorText: _error,
                        errorMaxLines: 3,
                      ),
                      onSubmitted: (_) => _continue(),
                    ),
                    const SizedBox(height: Space.xl),
                    FilledButton(onPressed: _continue, child: const Text('Continuar')),
                  ],
                ),
        ),
      ),
    );
  }
}
