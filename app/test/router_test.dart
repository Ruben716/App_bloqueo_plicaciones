import 'package:flutter_test/flutter_test.dart';
import 'package:sigilo/core/router/app_router.dart';
import 'package:sigilo/core/router/routes.dart';
import 'package:sigilo/core/session/app_session.dart';

void main() {
  String? redirect(AppSession session, String location, {String? pending}) {
    var pendingRoute = pending;
    return resolveRedirect(
      session: session,
      location: location,
      takePendingRoute: () {
        final r = pendingRoute;
        pendingRoute = null;
        return r;
      },
    );
  }

  const loading = AppSession.loading();
  const noCredential = AppSession(loaded: true, credentialSet: false, unlocked: false);
  const locked = AppSession(loaded: true, credentialSet: true, unlocked: false);
  const unlocked = AppSession(loaded: true, credentialSet: true, unlocked: true);

  test('mientras carga solo se ve el splash', () {
    expect(redirect(loading, Routes.home), Routes.splash);
    expect(redirect(loading, Routes.splash), isNull);
  });

  test('sin credencial se fuerza el onboarding', () {
    expect(redirect(noCredential, Routes.home), Routes.onboarding);
    expect(redirect(noCredential, Routes.onboardingCredential), isNull);
  });

  test('bloqueada solo permite bloqueo y recuperación', () {
    expect(redirect(locked, Routes.settings), Routes.lock);
    expect(redirect(locked, Routes.guest), Routes.lock);
    expect(redirect(locked, Routes.lock), isNull);
    expect(redirect(locked, Routes.recover), isNull);
  });

  test('desbloqueada sale del bloqueo hacia inicio o la ruta pendiente', () {
    expect(redirect(unlocked, Routes.lock), Routes.home);
    expect(redirect(unlocked, Routes.lock, pending: Routes.guest), Routes.guest);
    expect(redirect(unlocked, Routes.settings), isNull);
  });

  test('los pasos del onboarding siguen accesibles tras crear la credencial', () {
    expect(redirect(unlocked, Routes.onboardingRecovery), isNull);
    expect(redirect(unlocked, Routes.onboardingCredential), isNull);
    expect(redirect(unlocked, Routes.onboarding), Routes.home);
  });
}
