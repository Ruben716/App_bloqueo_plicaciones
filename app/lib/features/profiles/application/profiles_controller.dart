import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/engine/model_extensions.dart';

final profilesControllerProvider = AsyncNotifierProvider<ProfilesController, List<LockProfile>>(
  ProfilesController.new,
);

class ProfilesController extends AsyncNotifier<List<LockProfile>> {
  LockEngineHostApi get _api => ref.read(lockEngineApiProvider);

  @override
  Future<List<LockProfile>> build() => ref.watch(lockEngineApiProvider).getProfiles();

  /// Bloquea o desbloquea una app en el perfil "Siempre" (cambio optimista).
  Future<void> setAppLocked(String packageName, bool locked) async {
    final profiles = state.value;
    if (profiles != null) {
      state = AsyncData([
        for (final p in profiles)
          if (p.isDefault)
            p.copyWith(
              packages: locked
                  ? {...p.packages, packageName}.toList()
                  : p.packages.where((x) => x != packageName).toList(),
            )
          else
            p,
      ]);
    }
    await _api.setAppLocked(packageName, locked);
    ref.invalidate(engineStatusProvider);
  }

  Future<void> setDefaultPackages(Set<String> packages) async {
    final profiles = state.value ?? await future;
    final current = profiles.firstWhere((p) => p.isDefault);
    await save(current.copyWith(packages: packages.toList()));
  }

  Future<void> save(LockProfile profile) async {
    await _api.saveProfile(profile);
    state = AsyncData(await _api.getProfiles());
    ref.invalidate(engineStatusProvider);
  }

  Future<void> delete(String id) async {
    await _api.deleteProfile(id);
    state = AsyncData(await _api.getProfiles());
    ref.invalidate(engineStatusProvider);
  }

  static String newId() => 'p${DateTime.now().microsecondsSinceEpoch}';
}

/// Apps del perfil "Siempre" (las que se gestionan desde la pestaña Apps).
final lockedPackagesProvider = Provider<Set<String>>((ref) {
  final profiles = ref.watch(profilesControllerProvider).value ?? const [];
  return {
    for (final p in profiles)
      if (p.isDefault) ...p.packages,
  };
});
