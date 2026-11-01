import 'package:lock_engine/lock_engine.dart';

/// Los modelos generados por Pigeon no tienen `copyWith`; se añade aquí.
extension EngineSettingsCopy on EngineSettings {
  EngineSettings copyWith({
    RelockPolicy? relockPolicy,
    int? relockTimeoutMinutes,
    bool? biometricEnabled,
    bool? intruderEnabled,
    int? intruderThreshold,
    bool? hideNotifications,
    bool? antiUninstall,
    bool? fakeCrash,
    bool? blockScreenshots,
    String? themeId,
  }) => EngineSettings(
    relockPolicy: relockPolicy ?? this.relockPolicy,
    relockTimeoutMinutes: relockTimeoutMinutes ?? this.relockTimeoutMinutes,
    biometricEnabled: biometricEnabled ?? this.biometricEnabled,
    intruderEnabled: intruderEnabled ?? this.intruderEnabled,
    intruderThreshold: intruderThreshold ?? this.intruderThreshold,
    hideNotifications: hideNotifications ?? this.hideNotifications,
    antiUninstall: antiUninstall ?? this.antiUninstall,
    fakeCrash: fakeCrash ?? this.fakeCrash,
    blockScreenshots: blockScreenshots ?? this.blockScreenshots,
    themeId: themeId ?? this.themeId,
  );
}

extension LockProfileCopy on LockProfile {
  LockProfile copyWith({
    String? name,
    List<String>? packages,
    bool? enabled,
    Schedule? schedule,
    bool clearSchedule = false,
  }) => LockProfile(
    id: id,
    name: name ?? this.name,
    packages: packages ?? this.packages,
    enabled: enabled ?? this.enabled,
    isDefault: isDefault,
    schedule: clearSchedule ? null : (schedule ?? this.schedule),
  );
}

extension CredentialKindText on CredentialKind {
  /// "PIN", "contraseña", "patrón".
  String get noun => switch (this) {
    CredentialKind.pin => 'PIN',
    CredentialKind.password => 'contraseña',
    CredentialKind.pattern => 'patrón',
  };

  /// "tu PIN", "tu contraseña", "tu patrón".
  String get yours => 'tu $noun';

  String get title => switch (this) {
    CredentialKind.pin => 'PIN',
    CredentialKind.password => 'Contraseña',
    CredentialKind.pattern => 'Patrón',
  };
}

extension RelockPolicyText on RelockPolicy {
  String get title => switch (this) {
    RelockPolicy.immediately => 'Al salir de la app',
    RelockPolicy.afterTimeout => 'Tras unos minutos',
    RelockPolicy.onScreenOff => 'Al apagar la pantalla',
  };

  String get description => switch (this) {
    RelockPolicy.immediately => 'Máxima privacidad: pide la credencial cada vez.',
    RelockPolicy.afterTimeout => 'Si vuelves pronto a la app no se pide de nuevo.',
    RelockPolicy.onScreenOff => 'Una vez desbloqueada, sigue abierta hasta bloquear el teléfono.',
  };
}
