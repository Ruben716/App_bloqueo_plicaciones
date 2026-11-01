import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/session/app_session.dart';

/// Apps con icono en el launcher (sin Sigilo), ordenadas por nombre.
final installedAppsProvider = FutureProvider<List<InstalledApp>>((ref) {
  ref.watch(appResumeTickProvider);
  return ref.watch(lockEngineApiProvider).getInstalledApps();
});

enum AppFilter { all, locked, recommended }

/// Filtra por texto (nombre o paquete) y por categoría.
List<InstalledApp> filterApps(
  List<InstalledApp> apps, {
  required String query,
  required AppFilter filter,
  required Set<String> locked,
}) {
  final q = query.trim().toLowerCase();
  return apps.where((app) {
    final matchesQuery =
        q.isEmpty ||
        app.label.toLowerCase().contains(q) ||
        app.packageName.toLowerCase().contains(q);
    final matchesFilter = switch (filter) {
      AppFilter.all => true,
      AppFilter.locked => locked.contains(app.packageName),
      AppFilter.recommended => app.isRecommended,
    };
    return matchesQuery && matchesFilter;
  }).toList();
}
