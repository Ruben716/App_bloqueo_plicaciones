import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/router/navigation.dart';
import '../../../core/utils/formatters.dart';
import '../../apps/presentation/app_picker_page.dart';
import '../application/profiles_controller.dart';

/// Crear o editar un perfil: nombre, apps y horario opcional.
class ProfileEditorPage extends ConsumerStatefulWidget {
  const ProfileEditorPage({this.profile, super.key});

  final LockProfile? profile;

  @override
  ConsumerState<ProfileEditorPage> createState() => _ProfileEditorPageState();
}

class _ProfileEditorPageState extends ConsumerState<ProfileEditorPage> {
  late final _name = TextEditingController(text: widget.profile?.name ?? '');
  late Set<String> _packages = {...?widget.profile?.packages};
  late bool _enabled = widget.profile?.enabled ?? true;
  late bool _scheduled = widget.profile == null || widget.profile!.schedule != null;
  late Set<int> _days = {...?widget.profile?.schedule?.days};
  late int _start = widget.profile?.schedule?.startMinute ?? 9 * 60;
  late int _end = widget.profile?.schedule?.endMinute ?? 18 * 60;
  bool _saving = false;

  bool get _isDefault => widget.profile?.isDefault ?? false;

  @override
  void initState() {
    super.initState();
    if (widget.profile == null) _days = {1, 2, 3, 4, 5};
  }

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _pickTime(bool start) async {
    final current = start ? _start : _end;
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(hour: current ~/ 60, minute: current % 60),
    );
    if (picked == null) return;
    setState(() {
      final minutes = picked.hour * 60 + picked.minute;
      start ? _start = minutes : _end = minutes;
    });
  }

  Future<void> _pickApps() async {
    final result = await AppPickerPage.open(context, title: 'Apps del perfil', initial: _packages);
    if (result != null) setState(() => _packages = result);
  }

  Future<void> _save() async {
    final name = _name.text.trim();
    if (!_isDefault && name.isEmpty) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Ponle un nombre al perfil.')));
      return;
    }
    if (_scheduled && !_isDefault && _days.isEmpty) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Elige al menos un día.')));
      return;
    }
    setState(() => _saving = true);
    final profile = LockProfile(
      id: widget.profile?.id ?? ProfilesController.newId(),
      name: _isDefault ? widget.profile!.name : name,
      packages: _packages.toList(),
      enabled: _isDefault || _enabled,
      isDefault: _isDefault,
      schedule: _scheduled && !_isDefault
          ? Schedule(days: (_days.toList()..sort()), startMinute: _start, endMinute: _end)
          : null,
    );
    await ref.read(profilesControllerProvider.notifier).save(profile);
    if (mounted) popOrGoHome(context);
  }

  Future<void> _delete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Eliminar perfil?'),
        content: const Text('Las apps de este perfil dejarán de bloquearse por él.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    await ref.read(profilesControllerProvider.notifier).delete(widget.profile!.id);
    if (mounted) popOrGoHome(context);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.profile == null ? 'Nuevo perfil' : 'Editar perfil'),
        actions: [
          if (widget.profile != null && !_isDefault)
            IconButton(
              tooltip: 'Eliminar',
              onPressed: _delete,
              icon: const Icon(Icons.delete_outline_rounded),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(Space.lg),
        children: [
          if (_isDefault)
            Card(
              child: ListTile(
                leading: const Icon(Icons.all_inclusive_rounded),
                title: Text(widget.profile!.name),
                subtitle: const Text('Perfil principal: siempre activo.'),
              ),
            )
          else ...[
            TextField(
              controller: _name,
              textCapitalization: TextCapitalization.sentences,
              decoration: const InputDecoration(
                labelText: 'Nombre',
                hintText: 'Trabajo, Noche, Fin de semana...',
              ),
            ),
            const SizedBox(height: Space.md),
            SwitchListTile(
              value: _enabled,
              onChanged: (value) => setState(() => _enabled = value),
              title: const Text('Perfil activado'),
            ),
          ],
          const SectionHeader('Apps'),
          Card(
            child: ListTile(
              leading: const Icon(Icons.apps_rounded),
              title: Text('${_packages.length} apps seleccionadas'),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: _pickApps,
            ),
          ),
          if (!_isDefault) ...[
            const SectionHeader('Horario'),
            SwitchListTile(
              value: _scheduled,
              onChanged: (value) => setState(() => _scheduled = value),
              title: const Text('Solo en un horario'),
              subtitle: Text(_scheduled ? 'Se activa en la franja elegida' : 'Siempre activo'),
            ),
            if (_scheduled) ...[
              const SizedBox(height: Space.sm),
              Wrap(
                spacing: Space.sm,
                children: [
                  for (var day = 1; day <= 7; day++)
                    FilterChip(
                      label: Text(Formatters.dayInitials[day - 1]),
                      tooltip: Formatters.dayNames[day - 1],
                      selected: _days.contains(day),
                      onSelected: (selected) => setState(() {
                        selected ? _days.add(day) : _days.remove(day);
                      }),
                    ),
                ],
              ),
              const SizedBox(height: Space.md),
              Row(
                children: [
                  Expanded(
                    child: _TimeButton(
                      label: 'Desde',
                      minutes: _start,
                      onTap: () => _pickTime(true),
                    ),
                  ),
                  const SizedBox(width: Space.md),
                  Expanded(
                    child: _TimeButton(
                      label: 'Hasta',
                      minutes: _end,
                      onTap: () => _pickTime(false),
                    ),
                  ),
                ],
              ),
              if (_end <= _start)
                Padding(
                  padding: const EdgeInsets.only(top: Space.sm),
                  child: Text(
                    _end == _start ? 'Todo el día' : 'Termina al día siguiente',
                    style: theme.textTheme.bodySmall,
                  ),
                ),
            ],
          ],
          const SizedBox(height: Space.xxl),
          FilledButton(onPressed: _saving ? null : _save, child: const Text('Guardar')),
        ],
      ),
    );
  }
}

class _TimeButton extends StatelessWidget {
  const _TimeButton({required this.label, required this.minutes, required this.onTap});

  final String label;
  final int minutes;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onTap,
      style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(56)),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label, style: Theme.of(context).textTheme.labelSmall),
          Text(Formatters.minuteOfDay(minutes), style: Theme.of(context).textTheme.titleMedium),
        ],
      ),
    );
  }
}
