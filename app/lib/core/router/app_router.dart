import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../features/activity/presentation/activity_page.dart';
import '../../features/apps/presentation/apps_page.dart';
import '../../features/auth/presentation/app_lock_page.dart';
import '../../features/auth/presentation/credential_setup_page.dart';
import '../../features/auth/presentation/recover_access_page.dart';
import '../../features/auth/presentation/recovery_code_page.dart';
import '../../features/guest_mode/presentation/guest_mode_page.dart';
import '../../features/home/presentation/dashboard_page.dart';
import '../../features/home/presentation/home_shell.dart';
import '../../features/intruders/presentation/intruders_page.dart';
import '../../features/onboarding/presentation/onboarding_apps_page.dart';
import '../../features/onboarding/presentation/splash_page.dart';
import '../../features/onboarding/presentation/welcome_page.dart';
import '../../features/permissions/presentation/permissions_page.dart';
import '../../features/profiles/presentation/profile_editor_page.dart';
import '../../features/profiles/presentation/profiles_page.dart';
import '../../features/settings/presentation/settings_page.dart';
import '../session/app_session.dart';
import 'routes.dart';

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  ref.listen(appSessionProvider, (_, _) => refresh.value++);

  final router = GoRouter(
    initialLocation: Routes.splash,
    refreshListenable: refresh,
    redirect: (context, state) => resolveRedirect(
      session: ref.read(appSessionProvider),
      location: state.matchedLocation,
      takePendingRoute: ref.read(appSessionProvider.notifier).takePendingRoute,
    ),
    routes: [
      GoRoute(path: Routes.splash, builder: (_, _) => const SplashPage()),
      GoRoute(path: Routes.onboarding, builder: (_, _) => const WelcomePage()),
      GoRoute(
        path: Routes.onboardingCredential,
        builder: (_, _) => const CredentialSetupPage(mode: CredentialSetupMode.onboarding),
      ),
      GoRoute(
        path: Routes.onboardingRecovery,
        builder: (_, state) => RecoveryCodePage(code: state.extra as String?),
      ),
      GoRoute(
        path: Routes.onboardingPermissions,
        builder: (_, _) => const PermissionsPage(onboarding: true),
      ),
      GoRoute(path: Routes.onboardingApps, builder: (_, _) => const OnboardingAppsPage()),
      GoRoute(path: Routes.lock, builder: (_, _) => const AppLockPage()),
      GoRoute(path: Routes.recover, builder: (_, _) => const RecoverAccessPage()),
      StatefulShellRoute.indexedStack(
        builder: (_, _, shell) => HomeShell(shell: shell),
        branches: [
          StatefulShellBranch(
            routes: [GoRoute(path: Routes.home, builder: (_, _) => const DashboardPage())],
          ),
          StatefulShellBranch(
            routes: [GoRoute(path: Routes.apps, builder: (_, _) => const AppsPage())],
          ),
          StatefulShellBranch(
            routes: [GoRoute(path: Routes.profiles, builder: (_, _) => const ProfilesPage())],
          ),
          StatefulShellBranch(
            routes: [GoRoute(path: Routes.settings, builder: (_, _) => const SettingsPage())],
          ),
        ],
      ),
      GoRoute(
        path: Routes.profileEditor,
        builder: (_, state) => ProfileEditorPage(profile: state.extra as LockProfile?),
      ),
      GoRoute(
        path: Routes.changeCredential,
        builder: (_, _) => const CredentialSetupPage(mode: CredentialSetupMode.change),
      ),
      GoRoute(path: Routes.guest, builder: (_, _) => const GuestModePage()),
      GoRoute(path: Routes.intruders, builder: (_, _) => const IntrudersPage()),
      GoRoute(path: Routes.activity, builder: (_, _) => const ActivityPage()),
      GoRoute(
        path: Routes.permissions,
        builder: (_, _) => const PermissionsPage(onboarding: false),
      ),
    ],
  );

  ref.onDispose(() {
    router.dispose();
    refresh.dispose();
  });
  return router;
});

/// Reglas de navegación (función pura, probada en `test/router_test.dart`):
/// - Sin credencial: solo onboarding.
/// - Con credencial y bloqueada: solo bloqueo y recuperación.
/// - Desbloqueada: las pantallas de entrada llevan al inicio (o a la ruta
///   pendiente pedida por el sistema, p. ej. el modo invitado).
String? resolveRedirect({
  required AppSession session,
  required String location,
  required String? Function() takePendingRoute,
}) {
  if (!session.loaded) return location == Routes.splash ? null : Routes.splash;

  if (!session.credentialSet) {
    return location.startsWith(Routes.onboarding) ? null : Routes.onboarding;
  }

  if (!session.unlocked) {
    return (location == Routes.lock || location == Routes.recover) ? null : Routes.lock;
  }

  const entryPoints = {Routes.splash, Routes.lock, Routes.recover, Routes.onboarding};
  if (entryPoints.contains(location)) return takePendingRoute() ?? Routes.home;
  return null;
}
