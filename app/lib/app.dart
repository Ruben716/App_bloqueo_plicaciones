import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/router/app_router.dart';
import 'features/settings/application/settings_controller.dart';

class SigiloApp extends ConsumerWidget {
  const SigiloApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    final lockTheme = LockThemes.byId(ref.watch(themeIdProvider));

    return MaterialApp.router(
      title: 'Sigilo',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(lockTheme),
      darkTheme: AppTheme.dark(lockTheme),
      locale: const Locale('es'),
      supportedLocales: const [Locale('es')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      routerConfig: router,
    );
  }
}
