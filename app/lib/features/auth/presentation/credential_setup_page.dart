import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/engine/model_extensions.dart';
import '../../../core/router/routes.dart';
import '../../../core/session/app_session.dart';
import '../../../core/widgets/async_view.dart';
import 'widgets/credential_creator.dart';
import 'widgets/credential_input.dart';
import 'widgets/recovery_code_card.dart';

enum CredentialSetupMode { onboarding, change }

/// Crear (onboarding) o cambiar (ajustes) la credencial propia de Sigilo.
class CredentialSetupPage extends ConsumerStatefulWidget {
  const CredentialSetupPage({required this.mode, super.key});

  final CredentialSetupMode mode;

  @override
  ConsumerState<CredentialSetupPage> createState() => _CredentialSetupPageState();
}

class _CredentialSetupPageState extends ConsumerState<CredentialSetupPage> {
  /// Para cambiarla primero hay que demostrar que se conoce la actual.
  late bool _verified = widget.mode == CredentialSetupMode.onboarding;

  Future<void> _create(CredentialKind kind, String secret) async {
    final code = await ref.read(lockEngineApiProvider).setCredential(kind, secret);
    ref.invalidate(engineStatusProvider);
    if (!mounted) return;

    if (widget.mode == CredentialSetupMode.onboarding) {
      ref.read(appSessionProvider.notifier).markCredentialCreated();
      context.go(Routes.onboardingRecovery, extra: code);
      return;
    }

    if (code != null) await showRecoveryCodeDialog(context, code);
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text('Listo: ${kind.yours} ya protege tus apps.')));
    context.pop();
  }

  @override
  Widget build(BuildContext context) {
    final onboarding = widget.mode == CredentialSetupMode.onboarding;
    return Scaffold(
      appBar: AppBar(title: Text(onboarding ? 'Paso 1 de 4' : 'Cambiar credencial')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: Space.xl),
          child: _verified
              ? CredentialCreator(onCreated: _create)
              : _CurrentCredentialCheck(onVerified: () => setState(() => _verified = true)),
        ),
      ),
    );
  }
}

class _CurrentCredentialCheck extends ConsumerStatefulWidget {
  const _CurrentCredentialCheck({required this.onVerified});

  final VoidCallback onVerified;

  @override
  ConsumerState<_CurrentCredentialCheck> createState() => _CurrentCredentialCheckState();
}

class _CurrentCredentialCheckState extends ConsumerState<_CurrentCredentialCheck> {
  int _errorTick = 0;
  bool _busy = false;
  String? _message;

  Future<void> _verify(String secret) async {
    setState(() => _busy = true);
    final result = await ref.read(lockEngineApiProvider).verifyCredential(secret);
    if (!mounted) return;
    if (result.success) return widget.onVerified();
    setState(() {
      _busy = false;
      _errorTick++;
      _message = result.lockoutRemainingMs > 0
          ? 'Demasiados intentos. Inténtalo más tarde.'
          : 'Incorrecto.';
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return AsyncView(
      value: ref.watch(engineStatusProvider),
      data: (status) {
        final kind = status.credentialKind ?? CredentialKind.pin;
        return Column(
          children: [
            Text('Introduce ${kind.yours} actual', style: theme.textTheme.headlineMedium),
            SizedBox(
              height: 40,
              child: Center(
                child: Text(
                  _message ?? '',
                  style: TextStyle(color: theme.colorScheme.error, fontWeight: FontWeight.w600),
                ),
              ),
            ),
            CredentialInput(
              kind: kind,
              pinLength: status.pinLength,
              errorTick: _errorTick,
              enabled: !_busy,
              onSubmit: _verify,
            ),
          ],
        );
      },
    );
  }
}
