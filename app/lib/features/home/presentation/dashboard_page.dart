import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/router/routes.dart';
import '../../../core/widgets/async_view.dart';
import '../../activity/application/activity_providers.dart';
import '../../activity/presentation/activity_page.dart';
import '../../settings/application/settings_controller.dart';

class DashboardPage extends ConsumerWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(engineStatusProvider);
    final lockTheme = LockThemes.byId(ref.watch(themeIdProvider));
    final recent = ref.watch(activityLogProvider).value ?? const <ActivityEntry>[];

    return Scaffold(
      body: SafeArea(
        child: AsyncView(
          value: status,
          onRetry: () => ref.invalidate(engineStatusProvider),
          data: (s) => RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(engineStatusProvider);
              ref.invalidate(activityLogProvider);
            },
            child: ListView(
              padding: const EdgeInsets.all(Space.lg),
              children: [
                Text('Sigilo', style: Theme.of(context).textTheme.headlineMedium),
                const SizedBox(height: Space.lg),
                _HeroCard(status: s, theme: lockTheme),
                if (s.missingRequiredPermissions) ...[
                  const SizedBox(height: Space.md),
                  _AlertCard(
                    icon: Icons.warning_amber_rounded,
                    text: 'Faltan permisos: la protección no puede funcionar.',
                    action: 'Revisar',
                    onTap: () => context.push(Routes.permissions),
                  ),
                ],
                if (s.guestActive) ...[
                  const SizedBox(height: Space.md),
                  _AlertCard(
                    icon: Icons.person_rounded,
                    text: 'Modo invitado activado.',
                    action: 'Gestionar',
                    onTap: () => context.push(Routes.guest),
                  ),
                ],
                const SizedBox(height: Space.lg),
                Row(
                  children: [
                    Expanded(
                      child: StatTile(
                        icon: Icons.lock_rounded,
                        value: '${s.lockedAppCount}',
                        label: 'Apps protegidas',
                        onTap: () => context.go(Routes.apps),
                      ),
                    ),
                    const SizedBox(width: Space.md),
                    Expanded(
                      child: StatTile(
                        icon: Icons.schedule_rounded,
                        value: '${s.activeProfileIds.length}',
                        label: 'Perfiles activos',
                        onTap: () => context.go(Routes.profiles),
                      ),
                    ),
                    const SizedBox(width: Space.md),
                    Expanded(
                      child: StatTile(
                        icon: Icons.photo_camera_front_rounded,
                        value: '${s.intruderCount}',
                        label: 'Intrusos',
                        onTap: () => context.push(Routes.intruders),
                      ),
                    ),
                  ],
                ),
                const SectionHeader('Accesos rápidos'),
                _QuickAction(
                  icon: Icons.person_outline_rounded,
                  title: 'Modo invitado',
                  subtitle: 'Antes de prestar el teléfono',
                  onTap: () => context.push(Routes.guest),
                ),
                _QuickAction(
                  icon: Icons.history_rounded,
                  title: 'Actividad',
                  subtitle: 'Desbloqueos e intentos fallidos',
                  onTap: () => context.push(Routes.activity),
                ),
                SectionHeader(
                  'Actividad reciente',
                  trailing: recent.isEmpty
                      ? null
                      : TextButton(
                          onPressed: () => context.push(Routes.activity),
                          child: const Text('Ver todo'),
                        ),
                ),
                if (recent.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: Space.lg),
                    child: Text('Aún no hay actividad.'),
                  )
                else
                  for (final entry in recent.take(5)) ActivityTile(entry: entry),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _HeroCard extends ConsumerWidget {
  const _HeroCard({required this.status, required this.theme});

  final EngineStatus status;
  final LockThemeData theme;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    final active = status.enabled && status.running;
    final subtitle = !status.enabled
        ? 'Tus apps no están protegidas'
        : status.running
        ? '${status.lockedAppCount} apps piden tu credencial al abrirse'
        : 'Activada, pero faltan permisos';

    return Container(
      padding: const EdgeInsets.all(Space.xl),
      decoration: BoxDecoration(
        gradient: theme.gradient,
        borderRadius: BorderRadius.circular(Radii.xl),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(Space.md),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: theme.accent.withValues(alpha: 0.2),
            ),
            child: Icon(
              active ? Icons.verified_user_rounded : Icons.gpp_maybe_rounded,
              color: theme.accent,
              size: 36,
            ),
          ),
          const SizedBox(width: Space.lg),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  status.enabled ? 'Protección activa' : 'Protección desactivada',
                  style: text.titleLarge?.copyWith(color: Colors.white),
                ),
                const SizedBox(height: Space.xs),
                Text(subtitle, style: text.bodyMedium?.copyWith(color: Colors.white70)),
              ],
            ),
          ),
          Switch(
            value: status.enabled,
            activeThumbColor: theme.accent,
            onChanged: (value) async {
              try {
                await ref.read(lockEngineApiProvider).setEngineEnabled(value);
              } on PlatformException catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context)
                      .showSnackBar(SnackBar(content: Text(e.message ?? 'Error')));
                }
              }
              ref.invalidate(engineStatusProvider);
            },
          ),
        ],
      ),
    );
  }
}

class _AlertCard extends StatelessWidget {
  const _AlertCard({
    required this.icon,
    required this.text,
    required this.action,
    required this.onTap,
  });

  final IconData icon;
  final String text;
  final String action;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      color: scheme.errorContainer,
      child: ListTile(
        leading: Icon(icon, color: scheme.onErrorContainer),
        title: Text(text, style: TextStyle(color: scheme.onErrorContainer)),
        trailing: TextButton(onPressed: onTap, child: Text(action)),
      ),
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Space.sm),
      child: Card(
        child: ListTile(
          leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
          title: Text(title),
          subtitle: Text(subtitle),
          trailing: const Icon(Icons.chevron_right_rounded),
          onTap: onTap,
        ),
      ),
    );
  }
}
