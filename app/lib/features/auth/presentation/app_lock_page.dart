import 'dart:async';

import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/engine/model_extensions.dart';
import '../../../core/router/routes.dart';
import '../../../core/session/app_session.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/async_view.dart';
import '../../settings/application/settings_controller.dart';
import 'widgets/credential_input.dart';

/// Bloqueo de la propia app Sigilo (misma credencial que el overlay nativo).
class AppLockPage extends ConsumerStatefulWidget {
  const AppLockPage({super.key});

  @override
  ConsumerState<AppLockPage> createState() => _AppLockPageState();
}

class _AppLockPageState extends ConsumerState<AppLockPage> {
  int _errorTick = 0;
  bool _busy = false;
  bool _biometricReady = false;
  String? _message;
  Duration _lockout = Duration.zero;
  Timer? _timer;

  LockEngineHostApi get _api => ref.read(lockEngineApiProvider);

  @override
  void initState() {
    super.initState();
    _prepare();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _prepare() async {
    final lockoutMs = await _api.getLockoutRemainingMs();
    final settings = await ref.read(settingsControllerProvider.future);
    final biometric = settings.biometricEnabled
        ? await _api.getBiometricStatus()
        : BiometricStatus.unsupported;
    if (!mounted) return;
    setState(() => _biometricReady = biometric == BiometricStatus.available);
    if (lockoutMs > 0) {
      _startLockout(Duration(milliseconds: lockoutMs));
    } else if (_biometricReady) {
      await _biometric();
    }
  }

  Future<void> _submit(String secret) async {
    setState(() => _busy = true);
    final result = await _api.verifyCredential(secret);
    if (!mounted) return;
    if (result.success) return _unlock();
    setState(() {
      _busy = false;
      _errorTick++;
      _message = 'Incorrecto. Inténtalo de nuevo.';
    });
    if (result.lockoutRemainingMs > 0) {
      _startLockout(Duration(milliseconds: result.lockoutRemainingMs));
    }
  }

  Future<void> _biometric() async {
    final ok = await _api.authenticateBiometric('Desbloquear Sigilo', 'Confirma con tu huella');
    if (ok && mounted) _unlock();
  }

  void _unlock() {
    ref.invalidate(engineStatusProvider);
    ref.read(appSessionProvider.notifier).unlock();
  }

  void _startLockout(Duration duration) {
    _timer?.cancel();
    setState(() => _lockout = duration);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return timer.cancel();
      final next = _lockout - const Duration(seconds: 1);
      setState(() {
        if (next <= Duration.zero) {
          timer.cancel();
          _lockout = Duration.zero;
          _message = null;
        } else {
          _lockout = next;
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final lockTheme = LockThemes.byId(ref.watch(themeIdProvider));
    final status = ref.watch(engineStatusProvider);

    return Theme(
      data: AppTheme.dark(lockTheme),
      child: Scaffold(
        body: GradientBackdrop(
          theme: lockTheme,
          child: SafeArea(
            child: AsyncView(
              value: status,
              onRetry: () => ref.invalidate(engineStatusProvider),
              data: (s) => _content(context, s, lockTheme),
            ),
          ),
        ),
      ),
    );
  }

  Widget _content(BuildContext context, EngineStatus status, LockThemeData lockTheme) {
    final theme = Theme.of(context);
    final kind = status.credentialKind ?? CredentialKind.pin;
    final locked = _lockout > Duration.zero;
    final info = locked
        ? 'Demasiados intentos. Espera ${Formatters.duration(_lockout)}.'
        : _message;

    return LayoutBuilder(
      builder: (context, constraints) => SingleChildScrollView(
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: constraints.maxHeight),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const SizedBox(height: Space.xxl),
              Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(Space.lg),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: lockTheme.accent.withValues(alpha: 0.18),
                    ),
                    child: Icon(Icons.shield_rounded, size: 40, color: lockTheme.accent),
                  ),
                  const SizedBox(height: Space.lg),
                  Text(
                    'Sigilo',
                    style: theme.textTheme.headlineMedium?.copyWith(color: Colors.white),
                  ),
                  const SizedBox(height: Space.xs),
                  Text(
                    'Introduce ${kind.yours}',
                    style: theme.textTheme.bodyLarge?.copyWith(color: Colors.white70),
                  ),
                  SizedBox(
                    height: 48,
                    child: Center(
                      child: info == null
                          ? null
                          : Text(
                              info,
                              textAlign: TextAlign.center,
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: const Color(0xFFFCA5A5),
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                    ),
                  ),
                ],
              ),
              CredentialInput(
                kind: kind,
                pinLength: status.pinLength,
                errorTick: _errorTick,
                enabled: !_busy && !locked,
                foregroundColor: Colors.white,
                submitLabel: 'Desbloquear',
                onBiometric: _biometricReady && !locked ? _biometric : null,
                onSubmit: _submit,
              ),
              Padding(
                padding: const EdgeInsets.all(Space.lg),
                child: TextButton(
                  onPressed: () => context.go(Routes.recover),
                  child: Text(
                    '¿Olvidaste ${kind.yours}?',
                    style: const TextStyle(color: Colors.white70),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
