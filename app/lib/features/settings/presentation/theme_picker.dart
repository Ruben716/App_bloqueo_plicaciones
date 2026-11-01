import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';

/// Selector del tema de la pantalla de bloqueo (y del acento de la app).
class ThemePicker extends StatelessWidget {
  const ThemePicker({required this.selectedId, required this.onSelected, super.key});

  final String selectedId;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 112,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: Space.lg),
        itemCount: LockThemes.all.length,
        separatorBuilder: (_, _) => const SizedBox(width: Space.md),
        itemBuilder: (context, index) {
          final theme = LockThemes.all[index];
          final selected = theme.id == selectedId;
          return Semantics(
            selected: selected,
            button: true,
            label: 'Tema ${theme.name}',
            child: GestureDetector(
              onTap: () => onSelected(theme.id),
              child: Column(
                children: [
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    width: 64,
                    height: 80,
                    decoration: BoxDecoration(
                      gradient: theme.gradient,
                      borderRadius: BorderRadius.circular(Radii.md),
                      border: Border.all(
                        color: selected ? theme.accent : Colors.transparent,
                        width: 3,
                      ),
                    ),
                    child: Icon(
                      selected ? Icons.check_rounded : Icons.lock_rounded,
                      color: theme.accent,
                    ),
                  ),
                  const SizedBox(height: Space.xs),
                  Text(theme.name, style: Theme.of(context).textTheme.labelSmall),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
