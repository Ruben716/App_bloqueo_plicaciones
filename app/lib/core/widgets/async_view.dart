import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Pinta un [AsyncValue] con estados de carga y error coherentes.
class AsyncView<T> extends StatelessWidget {
  const AsyncView({required this.value, required this.data, this.onRetry, super.key});

  final AsyncValue<T> value;
  final Widget Function(T data) data;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return value.when(
      data: data,
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 40),
              const SizedBox(height: 12),
              Text('Algo salió mal', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 4),
              Text('$error', textAlign: TextAlign.center),
              if (onRetry != null) ...[
                const SizedBox(height: 12),
                OutlinedButton(onPressed: onRetry, child: const Text('Reintentar')),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
