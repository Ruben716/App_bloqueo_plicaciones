import 'dart:io';

import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/router/navigation.dart';
import '../../../core/router/routes.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/async_view.dart';
import '../../settings/application/settings_controller.dart';
import '../application/intruders_providers.dart';

/// Galería privada de fotos de intrusos (nunca se guardan en la galería).
class IntrudersPage extends ConsumerWidget {
  const IntrudersPage({super.key});

  Future<void> _clearAll(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Borrar todas las fotos?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Borrar')),
        ],
      ),
    );
    if (confirmed != true) return;
    await ref.read(lockEngineApiProvider).clearIntruderPhotos();
    ref.invalidate(intrudersProvider);
    ref.invalidate(engineStatusProvider);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final photos = ref.watch(intrudersProvider);
    final enabled = ref.watch(settingsControllerProvider).value?.intruderEnabled ?? false;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Intrusos'),
        leading: BackButton(onPressed: () => popOrGoHome(context)),
        actions: [
          if (photos.value?.isNotEmpty ?? false)
            IconButton(
              tooltip: 'Borrar todo',
              onPressed: () => _clearAll(context, ref),
              icon: const Icon(Icons.delete_sweep_rounded),
            ),
        ],
      ),
      body: AsyncView(
        value: photos,
        onRetry: () => ref.invalidate(intrudersProvider),
        data: (list) {
          if (list.isEmpty) {
            return EmptyState(
              icon: Icons.photo_camera_front_rounded,
              title: 'Nadie ha intentado entrar',
              message: enabled
                  ? 'Si alguien falla varias veces, verás aquí su foto.'
                  : 'Activa la foto del intruso para saber quién intenta abrir tus apps.',
              action: enabled
                  ? null
                  : FilledButton.tonal(
                      onPressed: () => context.go(Routes.settings),
                      child: const Text('Ir a Ajustes'),
                    ),
            );
          }
          return GridView.builder(
            padding: const EdgeInsets.all(Space.lg),
            gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
              maxCrossAxisExtent: 200,
              mainAxisSpacing: Space.md,
              crossAxisSpacing: Space.md,
              childAspectRatio: 0.75,
            ),
            itemCount: list.length,
            itemBuilder: (context, index) => _PhotoTile(photo: list[index]),
          );
        },
      ),
    );
  }
}

class _PhotoTile extends ConsumerWidget {
  const _PhotoTile({required this.photo});

  final IntruderPhoto photo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final when = DateTime.fromMillisecondsSinceEpoch(photo.timestampMs);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () =>
            Navigator.of(context)
                .push(MaterialPageRoute(builder: (_) => _PhotoViewer(photo: photo))),
        child: Stack(
          fit: StackFit.expand,
          children: [
            Image.file(File(photo.path), fit: BoxFit.cover, cacheWidth: 400),
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: Container(
                padding: const EdgeInsets.all(Space.sm),
                color: Colors.black54,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      photo.appLabel,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                    ),
                    Text(
                      Formatters.relative(when),
                      style: const TextStyle(color: Colors.white70, fontSize: 12),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PhotoViewer extends ConsumerWidget {
  const _PhotoViewer({required this.photo});

  final IntruderPhoto photo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final when = DateTime.fromMillisecondsSinceEpoch(photo.timestampMs);
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: Text('${photo.appLabel} · ${Formatters.relative(when)}'),
        actions: [
          IconButton(
            tooltip: 'Borrar',
            icon: const Icon(Icons.delete_outline_rounded),
            onPressed: () async {
              await ref.read(lockEngineApiProvider).deleteIntruderPhoto(photo.path);
              ref.invalidate(intrudersProvider);
              ref.invalidate(engineStatusProvider);
              if (context.mounted) Navigator.of(context).pop();
            },
          ),
        ],
      ),
      body: InteractiveViewer(child: Center(child: Image.file(File(photo.path)))),
    );
  }
}
