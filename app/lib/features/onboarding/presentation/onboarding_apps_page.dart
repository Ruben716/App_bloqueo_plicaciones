import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/router/routes.dart';
import '../../../core/widgets/async_view.dart';
import '../../apps/application/apps_providers.dart';
import '../../apps/presentation/widgets/app_selection_list.dart';
import '../../profiles/application/profiles_controller.dart';

/// Paso final: elegir qué apps proteger (las recomendadas vienen marcadas).
class OnboardingAppsPage extends ConsumerStatefulWidget {
  const OnboardingAppsPage({super.key});

  @override
  ConsumerState<OnboardingAppsPage> createState() => _OnboardingAppsPageState();
}

class _OnboardingAppsPageState extends ConsumerState<OnboardingAppsPage> {
  Set<String>? _selected;
  bool _saving = false;

  Future<void> _finish() async {
    setState(() => _saving = true);
    try {
      await ref.read(profilesControllerProvider.notifier).setDefaultPackages(_selected ?? {});
      await ref.read(lockEngineApiProvider).setEngineEnabled(true);
      ref.invalidate(engineStatusProvider);
      if (mounted) context.go(Routes.home);
    } on PlatformException catch (e) {
      if (!mounted) return;
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message ?? 'Error')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final apps = ref.watch(installedAppsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Paso 4 de 4'), automaticallyImplyLeading: false),
      body: AsyncView(
        value: apps,
        onRetry: () => ref.invalidate(installedAppsProvider),
        data: (list) {
          final selected = _selected ??= {
            for (final app in list)
              if (app.isRecommended && app.canLock) app.packageName,
          };
          return AppSelectionList(
            apps: list,
            selected: selected,
            header: const Padding(
              padding: EdgeInsets.fromLTRB(Space.lg, 0, Space.lg, Space.sm),
              child: Text('Marcamos las apps más sensibles. Puedes cambiarlas cuando quieras.'),
            ),
            onChanged: (pkg, value) => setState(() {
              value ? selected.add(pkg) : selected.remove(pkg);
            }),
          );
        },
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Space.lg),
          child: FilledButton.icon(
            onPressed: _saving ? null : _finish,
            icon: const Icon(Icons.shield_rounded),
            label: Text('Proteger ${_selected?.length ?? 0} apps'),
          ),
        ),
      ),
    );
  }
}
