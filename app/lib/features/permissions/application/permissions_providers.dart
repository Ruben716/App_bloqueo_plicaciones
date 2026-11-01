import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/session/app_session.dart';
import '../../settings/application/settings_controller.dart';

final permissionsProvider = FutureProvider<List<PermissionState>>((ref) {
  ref.watch(appResumeTickProvider);
  // Qué permisos son obligatorios depende de las funciones activadas.
  ref.watch(settingsControllerProvider);
  return ref.watch(lockEngineApiProvider).getPermissions();
});

/// Abre el diálogo o la pantalla de Ajustes del permiso sin que Sigilo se
/// bloquee al volver.
Future<void> requestPermission(WidgetRef ref, PermissionKind kind) async {
  ref.read(appSessionProvider.notifier).keepUnlockedDuringExternalAction();
  await ref.read(lockEngineApiProvider).requestPermission(kind);
}

bool isGranted(List<PermissionState> permissions, PermissionKind kind) =>
    permissions.any((p) => p.kind == kind && p.granted);
