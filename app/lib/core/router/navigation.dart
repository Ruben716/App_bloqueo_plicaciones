import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import 'routes.dart';

/// Vuelve atrás o, si la pantalla se abrió directamente (p. ej. desde el
/// mosaico de Ajustes rápidos), al inicio.
void popOrGoHome(BuildContext context) {
  if (context.canPop()) {
    context.pop();
  } else {
    context.go(Routes.home);
  }
}
