import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/session/app_session.dart';

final guestControllerProvider = AsyncNotifierProvider<GuestController, GuestState>(
  GuestController.new,
);

class GuestController extends AsyncNotifier<GuestState> {
  LockEngineHostApi get _api => ref.read(lockEngineApiProvider);

  @override
  Future<GuestState> build() {
    // El modo invitado puede activarse desde el mosaico con la app cerrada.
    ref.watch(appResumeTickProvider);
    return ref.watch(lockEngineApiProvider).getGuestState();
  }

  Future<void> setActive(bool active) async {
    await _api.setGuestMode(active);
    state = AsyncData(await _api.getGuestState());
    ref.invalidate(engineStatusProvider);
  }

  Future<void> setAllowed(Set<String> packages) async {
    await _api.setGuestAllowedPackages(packages.toList());
    state = AsyncData(await _api.getGuestState());
  }
}
