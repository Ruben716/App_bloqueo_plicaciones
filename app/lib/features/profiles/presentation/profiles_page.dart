import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/engine/model_extensions.dart';
import '../../../core/router/routes.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/app_icon.dart';
import '../../../core/widgets/async_view.dart';
import '../application/profiles_controller.dart';

/// Perfiles: grupos de apps que se bloquean siempre o según un horario.
class ProfilesPage extends ConsumerWidget {
  const ProfilesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profiles = ref.watch(profilesControllerProvider);
    final activeIds = ref.watch(engineStatusProvider).value?.activeProfileIds.toSet() ?? const {};

    return Scaffold(
      appBar: AppBar(title: const Text('Perfiles')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push(Routes.profileEditor),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nuevo perfil'),
      ),
      body: AsyncView(
        value: profiles,
        onRetry: () => ref.invalidate(profilesControllerProvider),
        data: (list) => ListView(
          padding: const EdgeInsets.fromLTRB(Space.lg, 0, Space.lg, 96),
          children: [
            Text(
              'Cada perfil protege un grupo de apps. Los perfiles con horario solo '
              'están activos en su franja (por ejemplo, redes sociales en horario de trabajo).',
              style: Theme.of(context).textTheme.bodyMedium
                  ?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: Space.lg),
            for (final profile in list) ...[
              _ProfileCard(
                profile: profile,
                activeNow: activeIds.contains(profile.id),
                onTap: () => context.push(Routes.profileEditor, extra: profile),
                onToggle: profile.isDefault
                    ? null
                    : (enabled) => ref
                          .read(profilesControllerProvider.notifier)
                          .save(profile.copyWith(enabled: enabled)),
              ),
              const SizedBox(height: Space.md),
            ],
          ],
        ),
      ),
    );
  }
}

class _ProfileCard extends StatelessWidget {
  const _ProfileCard({
    required this.profile,
    required this.activeNow,
    required this.onTap,
    required this.onToggle,
  });

  final LockProfile profile;
  final bool activeNow;
  final VoidCallback onTap;
  final ValueChanged<bool>? onToggle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final schedule = profile.schedule;
    final when = schedule == null
        ? 'Siempre activo'
        : '${Formatters.days(schedule.days)} · '
              '${Formatters.minuteOfDay(schedule.startMinute)}–${Formatters.minuteOfDay(schedule.endMinute)}';

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(Space.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    profile.isDefault ? Icons.all_inclusive_rounded : Icons.schedule_rounded,
                    color: theme.colorScheme.primary,
                  ),
                  const SizedBox(width: Space.md),
                  Expanded(child: Text(profile.name, style: theme.textTheme.titleMedium)),
                  if (activeNow && profile.enabled)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: Space.sm, vertical: 2),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.primaryContainer,
                        borderRadius: BorderRadius.circular(Radii.sm),
                      ),
                      child: Text(
                        'Activo ahora',
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onPrimaryContainer,
                        ),
                      ),
                    ),
                  if (onToggle != null) Switch(value: profile.enabled, onChanged: onToggle),
                ],
              ),
              const SizedBox(height: Space.sm),
              Text(when, style: theme.textTheme.bodyMedium),
              const SizedBox(height: Space.md),
              _IconStrip(packages: profile.packages),
            ],
          ),
        ),
      ),
    );
  }
}

class _IconStrip extends StatelessWidget {
  const _IconStrip({required this.packages});

  final List<String> packages;

  @override
  Widget build(BuildContext context) {
    if (packages.isEmpty) {
      return Text('Sin apps', style: Theme.of(context).textTheme.bodySmall);
    }
    const maxIcons = 6;
    return Row(
      children: [
        for (final pkg in packages.take(maxIcons))
          Padding(
            padding: const EdgeInsets.only(right: Space.sm),
            child: AppIcon(packageName: pkg, size: 28),
          ),
        if (packages.length > maxIcons)
          Text('+${packages.length - maxIcons}', style: Theme.of(context).textTheme.labelLarge),
      ],
    );
  }
}
