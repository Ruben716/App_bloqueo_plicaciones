import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/engine/model_extensions.dart';
import '../../../core/router/routes.dart';
import '../../../core/widgets/async_view.dart';
import '../../auth/presentation/widgets/recovery_code_card.dart';
import '../../permissions/application/permissions_providers.dart';
import '../application/settings_controller.dart';
import 'theme_picker.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsControllerProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Ajustes')),
      body: AsyncView(
        value: settings,
        onRetry: () => ref.invalidate(settingsControllerProvider),
        data: (s) => _SettingsList(settings: s),
      ),
    );
  }
}

class _SettingsList extends ConsumerWidget {
  const _SettingsList({required this.settings});

  final EngineSettings settings;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = ref.read(settingsControllerProvider.notifier);
    final status = ref.watch(engineStatusProvider).value;
    final biometric = ref.watch(biometricStatusProvider).value;
    final permissions = ref.watch(permissionsProvider).value ?? const <PermissionState>[];
    final kind = status?.credentialKind ?? CredentialKind.pin;
    final missing = permissions.where((p) => p.required && !p.granted).length;

    Future<void> change(EngineSettings Function(EngineSettings) update) =>
        controller.change(update);

    return ListView(
      padding: const EdgeInsets.only(bottom: Space.xxxl),
      children: [
        const SectionHeader('Protección'),
        SwitchListTile(
          secondary: const Icon(Icons.shield_rounded),
          title: const Text('Protección activa'),
          subtitle: Text(
            status?.enabled == true && status?.running != true
                ? 'Activada, pero faltan permisos'
                : 'Bloquea las apps elegidas al abrirlas',
          ),
          value: status?.enabled ?? false,
          onChanged: (value) => _setEngine(context, ref, value),
        ),
        ListTile(
          leading: const Icon(Icons.timer_outlined),
          title: const Text('Volver a bloquear'),
          subtitle: Text(
            settings.relockPolicy == RelockPolicy.afterTimeout
                ? 'Tras ${settings.relockTimeoutMinutes} min fuera de la app'
                : settings.relockPolicy.title,
          ),
          onTap: () => _pickRelock(context, ref),
        ),

        const SectionHeader('Desbloqueo'),
        ListTile(
          leading: const Icon(Icons.password_rounded),
          title: Text('Cambiar ${kind.noun}'),
          subtitle: const Text('Independiente del PIN o patrón del teléfono'),
          onTap: () => context.push(Routes.changeCredential),
        ),
        SwitchListTile(
          secondary: const Icon(Icons.fingerprint_rounded),
          title: const Text('Desbloquear con huella'),
          subtitle: Text(_biometricText(biometric)),
          value: settings.biometricEnabled,
          onChanged: biometric == BiometricStatus.available || settings.biometricEnabled
              ? (value) async {
                  final enabled = await controller.setBiometricEnabled(value);
                  if (value && !enabled && context.mounted) {
                    _snack(context, 'No se pudo activar la huella en este dispositivo.');
                  }
                }
              : null,
        ),
        ListTile(
          leading: const Icon(Icons.key_rounded),
          title: const Text('Nuevo código de recuperación'),
          subtitle: const Text('Invalida el anterior'),
          onTap: () => _regenerateRecovery(context, ref),
        ),

        const SectionHeader('Privacidad'),
        SwitchListTile(
          secondary: const Icon(Icons.photo_camera_front_rounded),
          title: const Text('Foto del intruso'),
          subtitle: Text(
            'Foto con la cámara frontal tras ${settings.intruderThreshold} intentos fallidos',
          ),
          value: settings.intruderEnabled,
          onChanged: (value) async {
            if (value && !isGranted(permissions, PermissionKind.camera)) {
              await requestPermission(ref, PermissionKind.camera);
            }
            await change((s) => s.copyWith(intruderEnabled: value));
          },
        ),
        if (settings.intruderEnabled)
          Padding(
            padding: const EdgeInsets.fromLTRB(72, 0, Space.lg, Space.sm),
            child: SegmentedButton<int>(
              segments: const [
                ButtonSegment(value: 1, label: Text('1')),
                ButtonSegment(value: 2, label: Text('2')),
                ButtonSegment(value: 3, label: Text('3')),
                ButtonSegment(value: 5, label: Text('5')),
              ],
              selected: {settings.intruderThreshold},
              onSelectionChanged: (value) =>
                  change((s) => s.copyWith(intruderThreshold: value.first)),
            ),
          ),
        SwitchListTile(
          secondary: const Icon(Icons.notifications_off_rounded),
          title: const Text('Ocultar notificaciones'),
          subtitle: const Text('Las de apps protegidas se sustituyen por un aviso genérico'),
          value: settings.hideNotifications,
          onChanged: (value) async {
            if (value && !isGranted(permissions, PermissionKind.notificationListener)) {
              await requestPermission(ref, PermissionKind.notificationListener);
            }
            await change((s) => s.copyWith(hideNotifications: value));
          },
        ),
        SwitchListTile(
          secondary: const Icon(Icons.delete_forever_rounded),
          title: const Text('Anti-desinstalación'),
          subtitle: const Text('Protege Ajustes, Play Store y el instalador de apps'),
          value: settings.antiUninstall,
          onChanged: (value) => change((s) => s.copyWith(antiUninstall: value)),
        ),
        SwitchListTile(
          secondary: const Icon(Icons.theater_comedy_rounded),
          title: const Text('Pantalla falsa de error'),
          subtitle: const Text(
            'Las apps parecen "detenidas". Mantén pulsado el título para desbloquear.',
          ),
          value: settings.fakeCrash,
          onChanged: (value) async {
            if (value && !await _confirmFakeCrash(context)) return;
            await change((s) => s.copyWith(fakeCrash: value));
          },
        ),
        SwitchListTile(
          secondary: const Icon(Icons.screenshot_monitor_rounded),
          title: const Text('Bloquear capturas de pantalla'),
          subtitle: const Text('También oculta Sigilo en la vista de apps recientes'),
          value: settings.blockScreenshots,
          onChanged: (value) => change((s) => s.copyWith(blockScreenshots: value)),
        ),

        const SectionHeader('Apariencia'),
        ThemePicker(
          selectedId: settings.themeId,
          onSelected: (id) => change((s) => s.copyWith(themeId: id)),
        ),

        const SectionHeader('Sistema'),
        ListTile(
          leading: Badge(
            isLabelVisible: missing > 0,
            label: Text('$missing'),
            child: const Icon(Icons.verified_user_rounded),
          ),
          title: const Text('Permisos'),
          subtitle: Text(missing > 0 ? 'Faltan $missing imprescindibles' : 'Todo en orden'),
          onTap: () => context.push(Routes.permissions),
        ),
        ListTile(
          leading: const Icon(Icons.history_rounded),
          title: const Text('Actividad'),
          onTap: () => context.push(Routes.activity),
        ),
        ListTile(
          leading: const Icon(Icons.photo_library_rounded),
          title: const Text('Fotos de intrusos'),
          onTap: () => context.push(Routes.intruders),
        ),

        const SectionHeader('Acerca de'),
        const ListTile(
          leading: Icon(Icons.wifi_off_rounded),
          title: Text('100 % sin conexión'),
          subtitle: Text('Sigilo no tiene permiso de Internet. Nada sale de tu teléfono.'),
        ),
      ],
    );
  }

  String _biometricText(BiometricStatus? status) => switch (status) {
    BiometricStatus.available => 'Atajo opcional; tu credencial sigue siendo la principal',
    BiometricStatus.notEnrolled => 'Registra una huella en los ajustes del teléfono',
    BiometricStatus.invalidated => 'Se registró una huella nueva: vuelve a activarla',
    BiometricStatus.unsupported || null => 'No disponible en este dispositivo',
  };

  Future<void> _setEngine(BuildContext context, WidgetRef ref, bool enabled) async {
    try {
      await ref.read(lockEngineApiProvider).setEngineEnabled(enabled);
    } on PlatformException catch (e) {
      if (context.mounted) _snack(context, e.message ?? 'Error');
    }
    ref.invalidate(engineStatusProvider);
  }

  Future<void> _pickRelock(BuildContext context, WidgetRef ref) {
    return showModalBottomSheet<void>(
      context: context,
      builder: (context) => Consumer(
        builder: (context, ref, _) {
          final current = ref.watch(settingsControllerProvider).value ?? settings;
          final controller = ref.read(settingsControllerProvider.notifier);
          return SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                for (final policy in RelockPolicy.values)
                  ListTile(
                    title: Text(policy.title),
                    subtitle: Text(policy.description),
                    trailing: current.relockPolicy == policy
                        ? Icon(
                            Icons.check_circle_rounded,
                            color: Theme.of(context).colorScheme.primary,
                          )
                        : null,
                    onTap: () => controller.change((s) => s.copyWith(relockPolicy: policy)),
                  ),
                if (current.relockPolicy == RelockPolicy.afterTimeout)
                  Padding(
                    padding: const EdgeInsets.all(Space.lg),
                    child: Wrap(
                      spacing: Space.sm,
                      children: [
                        for (final minutes in const [1, 5, 15, 30, 60])
                          ChoiceChip(
                            label: Text('$minutes min'),
                            selected: current.relockTimeoutMinutes == minutes,
                            onSelected: (_) =>
                                controller.change((s) => s.copyWith(relockTimeoutMinutes: minutes)),
                          ),
                      ],
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _regenerateRecovery(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Generar un código nuevo?'),
        content: const Text('El código actual dejará de funcionar.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Generar')),
        ],
      ),
    );
    if (confirmed != true) return;
    final code = await ref.read(lockEngineApiProvider).regenerateRecoveryCode();
    if (context.mounted) await showRecoveryCodeDialog(context, code);
  }

  Future<bool> _confirmFakeCrash(BuildContext context) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Pantalla falsa de error'),
        content: const Text(
          'Al abrir una app protegida verás un aviso de "la app se ha detenido". '
          'Para desbloquear, MANTÉN PULSADO el título del aviso. '
          'Pulsar "Cerrar app" vuelve al inicio.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Entendido'),
          ),
        ],
      ),
    );
    return ok ?? false;
  }

  void _snack(BuildContext context, String message) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
