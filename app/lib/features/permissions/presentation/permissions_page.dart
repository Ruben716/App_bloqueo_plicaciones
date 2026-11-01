import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/router/routes.dart';
import '../../../core/widgets/async_view.dart';
import '../application/permissions_providers.dart';
import 'permission_copy.dart';

/// Lista de permisos con su motivo y estado. Se refresca al volver de Ajustes.
class PermissionsPage extends ConsumerWidget {
  const PermissionsPage({required this.onboarding, super.key});

  final bool onboarding;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final permissions = ref.watch(permissionsProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(onboarding ? 'Paso 3 de 4' : 'Permisos'),
        automaticallyImplyLeading: !onboarding,
      ),
      body: AsyncView(
        value: permissions,
        onRetry: () => ref.invalidate(permissionsProvider),
        data: (list) {
          final missingRequired = list.where((p) => p.required && !p.granted).length;
          return ListView(
            padding: const EdgeInsets.only(bottom: Space.xxxl),
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(Space.lg, 0, Space.lg, Space.lg),
                child: Text(
                  missingRequired == 0
                      ? 'Todo listo. Los permisos opcionales mejoran la fiabilidad.'
                      : 'Faltan $missingRequired permisos imprescindibles para bloquear apps.',
                  style: theme.textTheme.bodyLarge,
                ),
              ),
              for (final permission in _sorted(list))
                _PermissionTile(
                  state: permission,
                  onRequest: () => requestPermission(ref, permission.kind),
                ),
              Padding(
                padding: const EdgeInsets.all(Space.lg),
                child: Card(
                  child: ListTile(
                    leading: const Icon(Icons.wifi_off_rounded),
                    title: const Text('Sin acceso a Internet'),
                    subtitle: const Text(
                      'Sigilo no declara el permiso de Internet: tus datos nunca salen del teléfono.',
                    ),
                    iconColor: theme.colorScheme.primary,
                  ),
                ),
              ),
            ],
          );
        },
      ),
      bottomNavigationBar: onboarding
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(Space.lg),
                child: FilledButton(
                  onPressed: () => context.go(Routes.onboardingApps),
                  child: const Text('Continuar'),
                ),
              ),
            )
          : null,
    );
  }

  List<PermissionState> _sorted(List<PermissionState> list) => [
    ...list.where((p) => p.required),
    ...list.where((p) => !p.required),
  ];
}

class _PermissionTile extends StatelessWidget {
  const _PermissionTile({required this.state, required this.onRequest});

  final PermissionState state;
  final VoidCallback onRequest;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final kind = state.kind;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: Space.xs),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(Space.lg),
          child: Row(
            children: [
              Icon(
                kind.icon,
                color: state.granted
                    ? theme.colorScheme.primary
                    : theme.colorScheme.onSurfaceVariant,
              ),
              const SizedBox(width: Space.lg),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(child: Text(kind.title, style: theme.textTheme.titleMedium)),
                        if (state.required) ...[
                          const SizedBox(width: Space.sm),
                          Text(
                            'Obligatorio',
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: theme.colorScheme.error,
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: Space.xs),
                    Text(
                      kind.reason,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: Space.sm),
              state.granted
                  ? Icon(Icons.check_circle_rounded, color: theme.colorScheme.primary)
                  : FilledButton.tonal(
                      style: FilledButton.styleFrom(minimumSize: const Size(0, 40)),
                      onPressed: onRequest,
                      child: const Text('Conceder'),
                    ),
            ],
          ),
        ),
      ),
    );
  }
}
