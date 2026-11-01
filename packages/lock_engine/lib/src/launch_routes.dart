/// Rutas que el sistema puede pedir a la app (deben coincidir con
/// `LaunchRoutes` en `EngineGraph.kt`).
abstract final class LaunchRoutes {
  /// Desactivar el modo invitado desde el mosaico de Ajustes rápidos.
  static const guestMode = '/guest';
}
