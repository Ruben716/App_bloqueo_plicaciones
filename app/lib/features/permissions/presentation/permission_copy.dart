import 'package:flutter/material.dart';
import 'package:lock_engine/lock_engine.dart';

/// Textos y por qué de cada permiso (transparencia con el usuario).
extension PermissionCopy on PermissionKind {
  IconData get icon => switch (this) {
    PermissionKind.usageAccess => Icons.query_stats_rounded,
    PermissionKind.overlay => Icons.layers_rounded,
    PermissionKind.notifications => Icons.notifications_rounded,
    PermissionKind.notificationListener => Icons.notifications_off_rounded,
    PermissionKind.camera => Icons.photo_camera_front_rounded,
    PermissionKind.batteryOptimization => Icons.battery_saver_rounded,
  };

  String get title => switch (this) {
    PermissionKind.usageAccess => 'Acceso a uso',
    PermissionKind.overlay => 'Mostrar sobre otras apps',
    PermissionKind.notifications => 'Notificaciones',
    PermissionKind.notificationListener => 'Acceso a notificaciones',
    PermissionKind.camera => 'Cámara',
    PermissionKind.batteryOptimization => 'Sin restricciones de batería',
  };

  String get reason => switch (this) {
    PermissionKind.usageAccess => 'Para saber qué app abres y bloquearla al instante.',
    PermissionKind.overlay => 'Para mostrar la pantalla de bloqueo encima de la app protegida.',
    PermissionKind.notifications => 'Para el aviso permanente que mantiene activa la protección.',
    PermissionKind.notificationListener =>
      'Para ocultar el contenido de las notificaciones de tus apps protegidas.',
    PermissionKind.camera => 'Para la foto del intruso tras varios intentos fallidos.',
    PermissionKind.batteryOptimization =>
      'Evita que el sistema (sobre todo Xiaomi, Huawei, Oppo...) cierre la protección.',
  };
}
