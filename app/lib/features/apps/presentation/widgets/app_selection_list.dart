import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../../core/widgets/app_icon.dart';
import '../../application/apps_providers.dart';

/// Lista de apps con buscador, filtros y un interruptor por app.
/// La reutilizan la pestaña Apps, el onboarding, perfiles y modo invitado.
class AppSelectionList extends StatefulWidget {
  const AppSelectionList({
    required this.apps,
    required this.selected,
    required this.onChanged,
    this.header,
    this.allowSystemEssentials = false,
    super.key,
  });

  final List<InstalledApp> apps;
  final Set<String> selected;
  final void Function(String packageName, bool selected) onChanged;
  final Widget? header;

  /// En el modo invitado se pueden "permitir" incluso apps no bloqueables.
  final bool allowSystemEssentials;

  @override
  State<AppSelectionList> createState() => _AppSelectionListState();
}

class _AppSelectionListState extends State<AppSelectionList> {
  String _query = '';
  AppFilter _filter = AppFilter.all;

  @override
  Widget build(BuildContext context) {
    final filtered = filterApps(
      widget.apps,
      query: _query,
      filter: _filter,
      locked: widget.selected,
    );
    return CustomScrollView(
      slivers: [
        if (widget.header != null) SliverToBoxAdapter(child: widget.header),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(Space.lg, Space.sm, Space.lg, Space.sm),
            child: TextField(
              onChanged: (value) => setState(() => _query = value),
              decoration: const InputDecoration(
                hintText: 'Buscar apps',
                prefixIcon: Icon(Icons.search_rounded),
              ),
            ),
          ),
        ),
        SliverToBoxAdapter(
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: Space.lg),
            child: Row(
              children: [
                for (final filter in AppFilter.values)
                  Padding(
                    padding: const EdgeInsets.only(right: Space.sm),
                    child: ChoiceChip(
                      label: Text(_filterLabel(filter)),
                      selected: _filter == filter,
                      onSelected: (_) => setState(() => _filter = filter),
                    ),
                  ),
              ],
            ),
          ),
        ),
        if (filtered.isEmpty)
          const SliverFillRemaining(
            hasScrollBody: false,
            child: EmptyState(icon: Icons.search_off_rounded, title: 'Sin resultados'),
          )
        else
          SliverList.builder(
            itemCount: filtered.length,
            itemBuilder: (context, index) {
              final app = filtered[index];
              final selectable = app.canLock || widget.allowSystemEssentials;
              final selected = widget.selected.contains(app.packageName);
              return SwitchListTile(
                value: selected,
                onChanged: selectable ? (value) => widget.onChanged(app.packageName, value) : null,
                secondary: AppIcon(packageName: app.packageName, label: app.label),
                title: Text(app.label, maxLines: 1, overflow: TextOverflow.ellipsis),
                subtitle: !selectable
                    ? const Text('Esencial del sistema: no se bloquea')
                    : app.isRecommended
                    ? const Text('Recomendada')
                    : null,
              );
            },
          ),
        const SliverPadding(padding: EdgeInsets.only(bottom: Space.xxxl)),
      ],
    );
  }

  String _filterLabel(AppFilter filter) => switch (filter) {
    AppFilter.all => 'Todas',
    AppFilter.locked => widget.allowSystemEssentials ? 'Elegidas' : 'Protegidas',
    AppFilter.recommended => 'Recomendadas',
  };
}
