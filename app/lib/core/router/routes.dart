import 'package:lock_engine/lock_engine.dart';

abstract final class Routes {
  static const splash = '/';
  static const onboarding = '/onboarding';
  static const onboardingCredential = '/onboarding/credential';
  static const onboardingRecovery = '/onboarding/recovery';
  static const onboardingPermissions = '/onboarding/permissions';
  static const onboardingApps = '/onboarding/apps';
  static const lock = '/lock';
  static const recover = '/recover';

  static const home = '/home';
  static const apps = '/apps';
  static const profiles = '/profiles';
  static const settings = '/settings';

  static const profileEditor = '/profile';
  static const changeCredential = '/credential';
  static const guest = LaunchRoutes.guestMode;
  static const intruders = '/intruders';
  static const activity = '/activity';
  static const permissions = '/permissions';
}
