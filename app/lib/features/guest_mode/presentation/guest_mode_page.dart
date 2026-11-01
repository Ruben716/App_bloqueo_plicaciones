import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/router/navigation.dart';
import '../../../core/widgets/app_icon.dart';
import '../../../core/widgets/async_view.dart';
import '../../apps/presentation/app_picker_page.dart';
import '../application/guest_controller.dart';

/// Modo invitado: al prestar el móvil se bloquea todo salvo lo permitido.
class GuestModePage extends ConsumerWidget {
  const GuestModePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final guest = ref.watch(guestControllerProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Modo invitado'),
        leading: BackButton(onPressed: () => popOrGoHome(context)),
      ),
      body: AsyncView(
        value: guest,
        onRetry: () => ref.invalidate(guestControllerProvider),
        data: (state) {
          final controller = ref.read(guestControllerProvider.notifier);
          return ListView(
            padding: const EdgeInsets.all(Space.lg),
            children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                padding: const EdgeInsets.all(Space.xl),
                decoration: BoxDecoration(
                  color: state.active
                      ? theme.colorScheme.primaryContainer
                      : theme.colorScheme.surfaceContainer,
                  borderRadius: BorderRadius.circular(Radii.xl),
                ),
                child: Column(
                  children: [
                    Icon(
                      state.active ? Icons.person_rounded : Icons.person_outline_rounded,
                      size: 56,
                      color: state.active
                          ? theme.colorScheme.onPrimaryContainer
                          : theme.colorScheme.primary,
                    ),
                    const SizedBox(height: Space.md),
                    Text(
                      state.active ? 'Modo invitado activado' : 'Modo invitado desactivado',
                      style: theme.textTheme.titleLarge,
                    ),
                    const SizedBox(height: Space.sm),
                    Text(
                      state.active
                          ? 'Todas las apps piden tu credencial excepto las permitidas.'
                          : 'Actívalo antes de prestar el teléfono.',
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: Space.lg),
                    FilledButton.icon(
                      onPressed: () => controller.setActive(!state.active),
                      icon: Icon(state.active ? Icons.lock_open_rounded : Icons.lock_rounded),
                      label: Text(state.active ? 'Desactivar' : 'Activar ahora'),
                    ),
                  ],
                ),
              ),
              SectionHeader(
                'Apps permitidas (${state.allowedPackages.length})',
                trailing: TextButton(
                  onPressed: () async {
                    final result = await AppPickerPage.open(
                      context,
                      title: 'Apps permitidas',
                      initial: state.allowedPackages.toSet(),
                      allowSystemEssentials: true,
                    );
                    if (result != null) await controller.setAllowed(result);
                  },
                  child: const Text('Editar'),
                ),
              ),
              if (state.allowedPackages.isEmpty)
                const Card(
                  child: ListTile(
                    leading: Icon(Icons.info_outline_rounded),
                    title: Text('Ninguna app permitida'),
                    subtitle: Text(
                      'El invitado solo podrá usar el teléfono, la cámara... nada más.',
                    ),
                  ),
                )
              else
                Wrap(
                  spacing: Space.md,
                  runSpacing: Space.md,
                  children: [
                    for (final pkg in state.allowedPackages) AppIcon(packageName: pkg, size: 48),
                  ],
                ),
              const SectionHeader('Consejo'),
              const Card(
                child: ListTile(
                  leading: Icon(Icons.dashboard_customize_rounded),
                  title: Text('Añade el mosaico "Modo invitado"'),
                  subtitle: Text(
                    'Desliza dos veces desde arriba, toca el lápiz y arrastra el mosaico. '
                    'Activarlo es un toque; desactivarlo pide tu credencial.',
                  ),
                ),
              ),
              const SizedBox(height: Space.md),
              Text(
                'El teléfono, el teclado y el launcher nunca se bloquean para que '
                'puedas recibir llamadas.',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
