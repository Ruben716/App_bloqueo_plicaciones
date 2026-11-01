import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../session/app_session.dart';

/// Acceso al motor nativo. En los tests se sobrescribe con un fake.
final lockEngineApiProvider = Provider<LockEngineHostApi>((ref) => LockEngineHostApi());

/// Estado general del motor (protección activa, credencial, contadores...).
/// Se refresca cada vez que la app vuelve a primer plano.
final engineStatusProvider = FutureProvider<EngineStatus>((ref) {
  ref.watch(appResumeTickProvider);
  return ref.watch(lockEngineApiProvider).getStatus();
});
