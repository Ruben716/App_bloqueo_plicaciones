import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../engine/engine_providers.dart';
import '../router/app_router.dart';

/// Estado del bloqueo de la propia app Sigilo.
@immutable
class AppSession {
  const AppSession({required this.loaded, required this.credentialSet, required this.unlocked});

  const AppSession.loading() : loaded = false, credentialSet = false, unlocked = false;

  final bool loaded;
  final bool credentialSet;
  final bool unlocked;

  AppSession copyWith({bool? loaded, bool? credentialSet, bool? unlocked}) => AppSession(
    loaded: loaded ?? this.loaded,
    credentialSet: credentialSet ?? this.credentialSet,
    unlocked: unlocked ?? this.unlocked,
  );
}

/// Contador que aumenta cada vez que la app vuelve a primer plano. Los
/// proveedores que lo observan se recargan (permisos concedidos en Ajustes,
/// fotos de intrusos nuevas, etc.).
final appResumeTickProvider = NotifierProvider<ResumeTick, int>(ResumeTick.new);

class ResumeTick extends Notifier<int> {
  @override
  int build() => 0;

  void bump() => state++;
}

final appSessionProvider = NotifierProvider<AppSessionController, AppSession>(
  AppSessionController.new,
);

/// Bloquea Sigilo al pasar a segundo plano y gestiona la ruta pendiente
/// solicitada desde el mosaico de Ajustes rápidos.
class AppSessionController extends Notifier<AppSession> {
  AppLifecycleListener? _lifecycle;
  bool _keepUnlockedOnce = false;
  String? _pendingRoute;

  @override
  AppSession build() {
    _lifecycle = AppLifecycleListener(onPause: _onPause, onResume: _onResume);
    ref.onDispose(() => _lifecycle?.dispose());
    Future.microtask(_load);
    return const AppSession.loading();
  }

  Future<void> _load() async {
    final api = ref.read(lockEngineApiProvider);
    try {
      final status = await api.getStatus();
      _pendingRoute = await api.consumeLaunchRoute();
      state = AppSession(loaded: true, credentialSet: status.credentialSet, unlocked: false);
    } catch (_) {
      state = const AppSession(loaded: true, credentialSet: false, unlocked: false);
    }
  }

  /// Tras crear la credencial en el onboarding la sesión queda abierta.
  void markCredentialCreated() => state = state.copyWith(credentialSet: true, unlocked: true);

  void unlock() => state = state.copyWith(unlocked: true);

  void lock() {
    if (state.credentialSet && state.unlocked) {
      state = state.copyWith(unlocked: false);
    }
  }

  /// Llamar antes de abrir Ajustes del sistema (permisos) para que la app no
  /// pida la credencial al volver.
  void keepUnlockedDuringExternalAction() => _keepUnlockedOnce = true;

  String? takePendingRoute() {
    final route = _pendingRoute;
    _pendingRoute = null;
    return route;
  }

  void _onPause() {
    if (_keepUnlockedOnce) return;
    lock();
  }

  Future<void> _onResume() async {
    _keepUnlockedOnce = false;
    ref.read(appResumeTickProvider.notifier).bump();
    final route = await ref.read(lockEngineApiProvider).consumeLaunchRoute();
    if (route == null) return;
    if (state.unlocked) {
      ref.read(routerProvider).push(route);
    } else {
      _pendingRoute = route;
    }
  }
}
