import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/async_view.dart';
import '../application/apps_providers.dart';
import 'widgets/app_selection_list.dart';

/// Selector de apps reutilizable. Devuelve el conjunto elegido al confirmar.
class AppPickerPage extends ConsumerStatefulWidget {
  const AppPickerPage({
    required this.title,
    required this.initial,
    this.allowSystemEssentials = false,
    super.key,
  });

  final String title;
  final Set<String> initial;
  final bool allowSystemEssentials;

  static Future<Set<String>?> open(
    BuildContext context, {
    required String title,
    required Set<String> initial,
    bool allowSystemEssentials = false,
  }) {
    return Navigator.of(context).push<Set<String>>(
      MaterialPageRoute(
        builder: (_) => AppPickerPage(
          title: title,
          initial: initial,
          allowSystemEssentials: allowSystemEssentials,
        ),
      ),
    );
  }

  @override
  ConsumerState<AppPickerPage> createState() => _AppPickerPageState();
}

class _AppPickerPageState extends ConsumerState<AppPickerPage> {
  late final Set<String> _selected = {...widget.initial};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(_selected),
            child: Text('Guardar (${_selected.length})'),
          ),
        ],
      ),
      body: AsyncView(
        value: ref.watch(installedAppsProvider),
        data: (apps) => AppSelectionList(
          apps: apps,
          selected: _selected,
          allowSystemEssentials: widget.allowSystemEssentials,
          onChanged: (pkg, value) => setState(() {
            value ? _selected.add(pkg) : _selected.remove(pkg);
          }),
        ),
      ),
    );
  }
}
