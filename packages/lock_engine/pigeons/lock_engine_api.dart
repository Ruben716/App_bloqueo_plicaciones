// Contrato tipado entre Flutter (Dart) y el motor nativo (Kotlin).
//
// Regenerar tras cualquier cambio:
//   cd packages/lock_engine && dart run pigeon --input pigeons/lock_engine_api.dart
import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/generated/lock_engine_api.g.dart',
    dartOptions: DartOptions(),
    kotlinOut:
        'android/src/main/kotlin/io/github/ruben716/lock_engine/api/LockEngineApi.g.kt',
    kotlinOptions: KotlinOptions(
      package: 'io.github.ruben716.lock_engine.api',
      errorClassName: 'LockEngineError',
    ),
    dartPackageName: 'lock_engine',
  ),
)
/// Tipo de credencial propia de Sigilo (independiente del bloqueo del sistema).
enum CredentialKind { pin, password, pattern }

/// Cuándo se vuelve a bloquear una app ya desbloqueada.
enum RelockPolicy {
  /// Al salir de la app.
  immediately,

  /// Tras X minutos fuera de la app.
  afterTimeout,

  /// Solo al apagar la pantalla.
  onScreenOff,
}

enum PermissionKind {
  usageAccess,
  overlay,
  notifications,
  notificationListener,
  camera,
  batteryOptimization,
}

enum BiometricStatus { available, notEnrolled, unsupported, invalidated }

enum ActivityKind {
  unlocked,
  failedAttempt,
  lockout,
  intruderCaptured,
  guestModeOn,
  guestModeOff,
  recoveryUsed,
  biometricInvalidated,
}

class InstalledApp {
  InstalledApp({
    required this.packageName,
    required this.label,
    required this.isSystem,
    required this.canLock,
    required this.isRecommended,
  });

  String packageName;
  String label;
  bool isSystem;

  /// `false` para apps que nunca se bloquean (launcher, teléfono, teclado...).
  bool canLock;

  /// Apps sensibles sugeridas (mensajería, galería, ajustes, tienda...).
  bool isRecommended;
}

/// Ventana horaria. `days` usa ISO-8601: 1 = lunes ... 7 = domingo.
/// Si `startMinute > endMinute` la ventana cruza la medianoche.
class Schedule {
  Schedule({
    required this.days,
    required this.startMinute,
    required this.endMinute,
  });

  List<int> days;
  int startMinute;
  int endMinute;
}

/// Grupo de apps bloqueadas. El perfil por defecto está siempre activo; el
/// resto se activa según su horario. Las apps bloqueadas efectivas son la
/// unión de todos los perfiles activos.
class LockProfile {
  LockProfile({
    required this.id,
    required this.name,
    required this.packages,
    required this.enabled,
    required this.isDefault,
    this.schedule,
  });

  String id;
  String name;
  List<String> packages;
  bool enabled;
  bool isDefault;
  Schedule? schedule;
}

class EngineSettings {
  EngineSettings({
    required this.relockPolicy,
    required this.relockTimeoutMinutes,
    required this.biometricEnabled,
    required this.intruderEnabled,
    required this.intruderThreshold,
    required this.hideNotifications,
    required this.antiUninstall,
    required this.fakeCrash,
    required this.blockScreenshots,
    required this.themeId,
  });

  RelockPolicy relockPolicy;
  int relockTimeoutMinutes;
  bool biometricEnabled;
  bool intruderEnabled;
  int intruderThreshold;
  bool hideNotifications;
  bool antiUninstall;
  bool fakeCrash;
  bool blockScreenshots;
  String themeId;
}

class GuestState {
  GuestState({required this.active, required this.allowedPackages});

  bool active;
  List<String> allowedPackages;
}

class EngineStatus {
  EngineStatus({
    required this.enabled,
    required this.running,
    required this.credentialSet,
    required this.guestActive,
    required this.activeProfileIds,
    required this.lockedAppCount,
    required this.intruderCount,
    required this.missingRequiredPermissions,
    this.credentialKind,
    this.pinLength,
  });

  bool enabled;
  bool running;
  bool credentialSet;
  CredentialKind? credentialKind;

  /// Longitud del PIN para confirmar automáticamente al completar los dígitos.
  int? pinLength;
  bool guestActive;
  List<String> activeProfileIds;
  int lockedAppCount;
  int intruderCount;
  bool missingRequiredPermissions;
}

class PermissionState {
  PermissionState({
    required this.kind,
    required this.granted,
    required this.required,
  });

  PermissionKind kind;
  bool granted;
  bool required;
}

class AuthResult {
  AuthResult({
    required this.success,
    required this.failedAttempts,
    required this.lockoutRemainingMs,
  });

  bool success;
  int failedAttempts;
  int lockoutRemainingMs;
}

class RecoveryResult {
  RecoveryResult({
    required this.success,
    required this.lockoutRemainingMs,
    this.newRecoveryCode,
    this.errorMessage,
  });

  bool success;
  int lockoutRemainingMs;
  String? newRecoveryCode;
  String? errorMessage;
}

class IntruderPhoto {
  IntruderPhoto({
    required this.path,
    required this.timestampMs,
    required this.packageName,
    required this.appLabel,
  });

  String path;
  int timestampMs;
  String packageName;
  String appLabel;
}

class ActivityEntry {
  ActivityEntry({
    required this.timestampMs,
    required this.kind,
    this.packageName,
    this.appLabel,
  });

  int timestampMs;
  ActivityKind kind;
  String? packageName;
  String? appLabel;
}

@HostApi()
abstract class LockEngineHostApi {
  // ---------------------------------------------------------------- Estado
  EngineStatus getStatus();

  void setEngineEnabled(bool enabled);

  // ----------------------------------------------------------- Credencial
  /// Devuelve un mensaje de error si la credencial es débil o inválida.
  String? validateCredential(CredentialKind kind, String secret);

  /// Guarda la credencial. La primera vez devuelve el código de recuperación.
  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  String? setCredential(CredentialKind kind, String secret);

  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  AuthResult verifyCredential(String secret);

  int getLockoutRemainingMs();

  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  RecoveryResult resetWithRecoveryCode(
    String code,
    CredentialKind kind,
    String newSecret,
  );

  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  String regenerateRecoveryCode();

  // ------------------------------------------------------------ Biometría
  BiometricStatus getBiometricStatus();

  /// Devuelve el estado final (puede fallar si no hay huellas registradas).
  bool setBiometricEnabled(bool enabled);

  @asyncCallback
  bool authenticateBiometric(String title, String subtitle);

  // ----------------------------------------------------------------- Apps
  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  List<InstalledApp> getInstalledApps();

  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  Uint8List? getAppIcon(String packageName, int sizePx);

  // ------------------------------------------------------- Configuración
  EngineSettings getSettings();

  void updateSettings(EngineSettings settings);

  List<LockProfile> getProfiles();

  void saveProfile(LockProfile profile);

  void deleteProfile(String id);

  /// Atajo: bloquea/desbloquea una app en el perfil por defecto.
  void setAppLocked(String packageName, bool locked);

  GuestState getGuestState();

  void setGuestMode(bool active);

  void setGuestAllowedPackages(List<String> packages);

  // -------------------------------------------------------------- Permisos
  List<PermissionState> getPermissions();

  void requestPermission(PermissionKind kind);

  // ------------------------------------------------- Intrusos / actividad
  @TaskQueue(type: TaskQueueType.serialBackgroundThread)
  List<IntruderPhoto> getIntruderPhotos();

  void deleteIntruderPhoto(String path);

  void clearIntruderPhotos();

  List<ActivityEntry> getActivityLog();

  void clearActivityLog();

  // ------------------------------------------------------------ Navegación
  /// Ruta solicitada al abrir la app desde el mosaico de Ajustes rápidos.
  String? consumeLaunchRoute();
}
