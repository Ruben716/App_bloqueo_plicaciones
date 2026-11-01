import 'dart:typed_data';

import 'package:lock_engine/lock_engine.dart';

/// Motor nativo simulado en memoria para tests de widgets y lógica.
class FakeEngineApi extends LockEngineHostApi {
  FakeEngineApi({
    this.credentialKind = CredentialKind.pin,
    this.secret = '2580',
    this.credentialSet = true,
  });

  CredentialKind credentialKind;
  String secret;
  bool credentialSet;
  bool engineEnabled = true;
  int failedAttempts = 0;
  String? launchRoute;
  final List<String> verifyCalls = [];

  EngineSettings settings = EngineSettings(
    relockPolicy: RelockPolicy.immediately,
    relockTimeoutMinutes: 5,
    biometricEnabled: false,
    intruderEnabled: false,
    intruderThreshold: 3,
    hideNotifications: false,
    antiUninstall: false,
    fakeCrash: false,
    blockScreenshots: true,
    themeId: 'aurora',
  );

  List<LockProfile> profiles = [
    LockProfile(
      id: 'default',
      name: 'Siempre',
      packages: ['com.whatsapp'],
      enabled: true,
      isDefault: true,
    ),
  ];

  GuestState guest = GuestState(active: false, allowedPackages: []);

  List<InstalledApp> apps = [
    InstalledApp(
      packageName: 'com.whatsapp',
      label: 'WhatsApp',
      isSystem: false,
      canLock: true,
      isRecommended: true,
    ),
    InstalledApp(
      packageName: 'com.android.chrome',
      label: 'Chrome',
      isSystem: false,
      canLock: true,
      isRecommended: false,
    ),
    InstalledApp(
      packageName: 'com.launcher',
      label: 'Launcher',
      isSystem: true,
      canLock: false,
      isRecommended: false,
    ),
  ];

  @override
  Future<EngineStatus> getStatus() async => EngineStatus(
    enabled: engineEnabled,
    running: engineEnabled,
    credentialSet: credentialSet,
    credentialKind: credentialSet ? credentialKind : null,
    pinLength: credentialSet && credentialKind == CredentialKind.pin ? secret.length : null,
    guestActive: guest.active,
    activeProfileIds: ['default'],
    lockedAppCount: profiles.first.packages.length,
    intruderCount: 0,
    missingRequiredPermissions: false,
  );

  @override
  Future<void> setEngineEnabled(bool enabled) async => engineEnabled = enabled;

  @override
  Future<String?> validateCredential(CredentialKind kind, String secret) async =>
      kind == CredentialKind.pin && secret == '1234' ? 'Evita secuencias como 1234' : null;

  @override
  Future<String?> setCredential(CredentialKind kind, String secret) async {
    final first = !credentialSet;
    credentialKind = kind;
    this.secret = secret;
    credentialSet = true;
    return first ? 'ABCD-EFGH-JKLM' : null;
  }

  @override
  Future<AuthResult> verifyCredential(String secret) async {
    verifyCalls.add(secret);
    final ok = secret == this.secret;
    failedAttempts = ok ? 0 : failedAttempts + 1;
    return AuthResult(success: ok, failedAttempts: failedAttempts, lockoutRemainingMs: 0);
  }

  @override
  Future<int> getLockoutRemainingMs() async => 0;

  @override
  Future<BiometricStatus> getBiometricStatus() async => BiometricStatus.unsupported;

  @override
  Future<EngineSettings> getSettings() async => settings;

  @override
  Future<void> updateSettings(EngineSettings settings) async => this.settings = settings;

  @override
  Future<List<LockProfile>> getProfiles() async => profiles;

  @override
  Future<void> setAppLocked(String packageName, bool locked) async {
    final current = profiles.first;
    final packages = {...current.packages};
    locked ? packages.add(packageName) : packages.remove(packageName);
    profiles = [
      LockProfile(
        id: current.id,
        name: current.name,
        packages: packages.toList(),
        enabled: true,
        isDefault: true,
      ),
      ...profiles.skip(1),
    ];
  }

  @override
  Future<GuestState> getGuestState() async => guest;

  @override
  Future<List<InstalledApp>> getInstalledApps() async => apps;

  @override
  Future<Uint8List?> getAppIcon(String packageName, int sizePx) async => null;

  @override
  Future<List<PermissionState>> getPermissions() async => [
    PermissionState(kind: PermissionKind.usageAccess, granted: true, required: true),
    PermissionState(kind: PermissionKind.overlay, granted: true, required: true),
  ];

  @override
  Future<List<IntruderPhoto>> getIntruderPhotos() async => [];

  @override
  Future<List<ActivityEntry>> getActivityLog() async => [
    ActivityEntry(
      timestampMs: DateTime(2026, 11, 1, 9, 30).millisecondsSinceEpoch,
      kind: ActivityKind.unlocked,
      packageName: 'com.whatsapp',
      appLabel: 'WhatsApp',
    ),
  ];

  @override
  Future<String?> consumeLaunchRoute() async {
    final route = launchRoute;
    launchRoute = null;
    return route;
  }
}
