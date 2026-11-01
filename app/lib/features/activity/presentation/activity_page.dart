import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/router/navigation.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/async_view.dart';
import '../application/activity_providers.dart';

/// Historial local de desbloqueos, intentos fallidos y cambios de modo.
class ActivityPage extends ConsumerWidget {
  const ActivityPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final log = ref.watch(activityLogProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Actividad'),
        leading: BackButton(onPressed: () => popOrGoHome(context)),
        actions: [
          if (log.value?.isNotEmpty ?? false)
            IconButton(
              tooltip: 'Borrar historial',
              icon: const Icon(Icons.delete_sweep_rounded),
              onPressed: () async {
                await ref.read(lockEngineApiProvider).clearActivityLog();
                ref.invalidate(activityLogProvider);
              },
            ),
        ],
      ),
      body: AsyncView(
        value: log,
        onRetry: () => ref.invalidate(activityLogProvider),
        data: (entries) => entries.isEmpty
            ? const EmptyState(icon: Icons.history_rounded, title: 'Sin actividad todavía')
            : ListView.builder(
                padding: const EdgeInsets.only(bottom: Space.xxl),
                itemCount: entries.length,
                itemBuilder: (context, index) => ActivityTile(entry: entries[index]),
              ),
      ),
    );
  }
}

class ActivityTile extends StatelessWidget {
  const ActivityTile({required this.entry, super.key});

  final ActivityEntry entry;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (icon, color) = switch (entry.kind) {
      ActivityKind.unlocked => (Icons.lock_open_rounded, scheme.primary),
      ActivityKind.failedAttempt => (Icons.error_outline_rounded, scheme.error),
      ActivityKind.lockout => (Icons.timer_off_rounded, scheme.error),
      ActivityKind.intruderCaptured => (Icons.photo_camera_front_rounded, scheme.error),
      ActivityKind.guestModeOn => (Icons.person_rounded, scheme.tertiary),
      ActivityKind.guestModeOff => (Icons.person_off_rounded, scheme.tertiary),
      ActivityKind.recoveryUsed => (Icons.key_rounded, scheme.secondary),
      ActivityKind.biometricInvalidated => (Icons.fingerprint_rounded, scheme.error),
    };
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: color.withValues(alpha: 0.15),
        child: Icon(icon, color: color, size: 20),
      ),
      title: Text(describe(entry)),
      subtitle: Text(Formatters.relative(DateTime.fromMillisecondsSinceEpoch(entry.timestampMs))),
    );
  }

  static String describe(ActivityEntry entry) {
    final app = entry.appLabel ?? entry.packageName ?? 'una app';
    return switch (entry.kind) {
      ActivityKind.unlocked => 'Desbloqueaste $app',
      ActivityKind.failedAttempt => 'Intento fallido en $app',
      ActivityKind.lockout => 'Bloqueo temporal por demasiados intentos',
      ActivityKind.intruderCaptured => 'Foto de intruso en $app',
      ActivityKind.guestModeOn => 'Modo invitado activado',
      ActivityKind.guestModeOff => 'Modo invitado desactivado',
      ActivityKind.recoveryUsed => 'Acceso recuperado con el código',
      ActivityKind.biometricInvalidated =>
        'Huella desactivada: se registró una huella nueva en el teléfono',
    };
  }
}
