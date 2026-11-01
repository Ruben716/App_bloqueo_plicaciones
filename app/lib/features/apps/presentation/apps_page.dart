import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/async_view.dart';
import '../../profiles/application/profiles_controller.dart';
import '../application/apps_providers.dart';
import 'widgets/app_selection_list.dart';

/// Pestaña "Apps": qué apps están siempre protegidas (perfil por defecto).
class AppsPage extends ConsumerWidget {
  const AppsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final apps = ref.watch(installedAppsProvider);
    final locked = ref.watch(lockedPackagesProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Apps protegidas')),
      body: AsyncView(
        value: apps,
        onRetry: () => ref.invalidate(installedAppsProvider),
        data: (list) => AppSelectionList(
          apps: list,
          selected: locked,
          header: Padding(
            padding: const EdgeInsets.fromLTRB(Space.lg, 0, Space.lg, Space.sm),
            child: Card(
              color: theme.colorScheme.primaryContainer,
              child: ListTile(
                leading: Icon(Icons.shield_rounded, color: theme.colorScheme.onPrimaryContainer),
                title: Text(
                  '${locked.length} apps siempre protegidas',
                  style: TextStyle(color: theme.colorScheme.onPrimaryContainer),
                ),
                subtitle: Text(
                  'Usa Perfiles para proteger otras solo en ciertos horarios.',
                  style: TextStyle(color: theme.colorScheme.onPrimaryContainer),
                ),
              ),
            ),
          ),
          onChanged: (pkg, value) =>
              ref.read(profilesControllerProvider.notifier).setAppLocked(pkg, value),
        ),
      ),
    );
  }
}
