import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ProviderScope(retry: _noRetry, child: SigiloApp()));
}

/// Las llamadas al motor nativo son locales: reintentar no aporta nada.
Duration? _noRetry(int retryCount, Object error) => null;
