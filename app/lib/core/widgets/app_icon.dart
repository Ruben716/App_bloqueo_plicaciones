import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../engine/engine_providers.dart';

/// Iconos de apps cacheados en memoria durante la sesión.
final appIconProvider = FutureProvider.family<Uint8List?, String>((ref, packageName) {
  return ref.watch(lockEngineApiProvider).getAppIcon(packageName, 144);
});

class AppIcon extends ConsumerWidget {
  const AppIcon({required this.packageName, this.label, this.size = 40, super.key});

  final String packageName;
  final String? label;
  final double size;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bytes = ref.watch(appIconProvider(packageName)).value;
    final radius = BorderRadius.circular(size * 0.28);
    if (bytes != null) {
      return ClipRRect(
        borderRadius: radius,
        child: Image.memory(bytes, width: size, height: size, gaplessPlayback: true),
      );
    }
    final scheme = Theme.of(context).colorScheme;
    final initial = (label?.isNotEmpty ?? false) ? label!.characters.first.toUpperCase() : '?';
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: scheme.secondaryContainer, borderRadius: radius),
      child: Text(
        initial,
        style: TextStyle(color: scheme.onSecondaryContainer, fontWeight: FontWeight.w700),
      ),
    );
  }
}
