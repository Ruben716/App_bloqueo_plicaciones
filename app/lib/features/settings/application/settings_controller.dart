import 'package:design_system/design_system.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/session/app_session.dart';

final settingsControllerProvider = AsyncNotifierProvider<SettingsController, EngineSettings>(
  SettingsController.new,
);

class SettingsController extends AsyncNotifier<EngineSettings> {
  LockEngineHostApi get _api => ref.read(lockEngineApiProvider);

  @override
  Future<EngineSettings> build() => ref.watch(lockEngineApiProvider).getSettings();

  /// Aplica un cambio de forma optimista y lo persiste en el motor nativo.
  Future<void> change(EngineSettings Function(EngineSettings current) update) async {
    final current = state.value ?? await future;
    final next = update(current);
    state = AsyncData(next);
    await _api.updateSettings(next);
    ref.invalidate(engineStatusProvider);
  }

  /// La biometría tiene su propio flujo porque crea una clave en el Keystore.
  Future<bool> setBiometricEnabled(bool enabled) async {
    final result = await _api.setBiometricEnabled(enabled);
    ref.invalidateSelf();
    ref.invalidate(biometricStatusProvider);
    return result;
  }
}

/// Tema elegido (afecta a la app y a la pantalla de bloqueo nativa).
final themeIdProvider = Provider<String>(
  (ref) => ref.watch(settingsControllerProvider).value?.themeId ?? LockThemes.aurora.id,
);

final biometricStatusProvider = FutureProvider<BiometricStatus>((ref) {
  ref.watch(appResumeTickProvider);
  return ref.watch(lockEngineApiProvider).getBiometricStatus();
});
