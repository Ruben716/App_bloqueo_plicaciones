import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../../core/engine/engine_providers.dart';
import '../../../../core/engine/model_extensions.dart';
import 'credential_input.dart';

/// Flujo para crear una credencial: elegir tipo → introducir → confirmar.
/// La validación (PIN débil, longitud...) la hace el motor nativo.
class CredentialCreator extends ConsumerStatefulWidget {
  const CredentialCreator({
    required this.onCreated,
    this.headline,
    this.foregroundColor,
    super.key,
  });

  final Future<void> Function(CredentialKind kind, String secret) onCreated;
  final String? headline;
  final Color? foregroundColor;

  @override
  ConsumerState<CredentialCreator> createState() => _CredentialCreatorState();
}

class _CredentialCreatorState extends ConsumerState<CredentialCreator> {
  CredentialKind _kind = CredentialKind.pin;
  String? _first;
  String? _error;
  int _errorTick = 0;
  bool _busy = false;

  bool get _confirming => _first != null;

  void _fail(String message, {bool restart = false}) {
    setState(() {
      _error = message;
      _errorTick++;
      if (restart) _first = null;
    });
  }

  Future<void> _handle(String secret) async {
    if (!_confirming) {
      final error = await ref.read(lockEngineApiProvider).validateCredential(_kind, secret);
      if (!mounted) return;
      if (error != null) return _fail(error);
      setState(() {
        _first = secret;
        _error = null;
      });
      return;
    }

    if (secret != _first) return _fail('No coincide. Inténtalo otra vez.');

    setState(() => _busy = true);
    try {
      await widget.onCreated(_kind, secret);
    } on PlatformException catch (e) {
      _fail(e.message ?? 'No se pudo guardar.', restart: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = widget.foregroundColor ?? theme.colorScheme.onSurface;
    final title = _confirming
        ? 'Confirma ${_kind.yours}'
        : (widget.headline ?? 'Crea ${_kind.yours}');
    final subtitle = _confirming
        ? 'Vuelve a introducirlo para comprobar que lo recuerdas.'
        : 'Es solo de Sigilo: no es el PIN ni el patrón de tu teléfono.';

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          title,
          style: theme.textTheme.headlineMedium?.copyWith(color: color),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Space.sm),
        Text(
          subtitle,
          style: theme.textTheme.bodyMedium?.copyWith(color: color.withValues(alpha: 0.75)),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Space.lg),
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 200),
          child: _confirming
              ? const SizedBox(height: 48)
              : SegmentedButton<CredentialKind>(
                  showSelectedIcon: false,
                  segments: [
                    for (final kind in CredentialKind.values)
                      ButtonSegment(value: kind, label: Text(kind.title), icon: Icon(_icon(kind))),
                  ],
                  selected: {_kind},
                  onSelectionChanged: (selection) => setState(() {
                    _kind = selection.first;
                    _error = null;
                  }),
                ),
        ),
        SizedBox(
          height: 40,
          child: Center(
            child: _error == null
                ? null
                : Text(
                    _error!,
                    textAlign: TextAlign.center,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.error,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
          ),
        ),
        CredentialInput(
          key: ValueKey('${_kind.name}-$_confirming'),
          kind: _kind,
          pinLength: _confirming && _kind == CredentialKind.pin ? _first!.length : null,
          errorTick: _errorTick,
          enabled: !_busy,
          foregroundColor: widget.foregroundColor,
          submitLabel: _confirming ? 'Confirmar' : 'Continuar',
          onSubmit: _handle,
        ),
        if (_confirming)
          TextButton(
            onPressed: _busy ? null : () => setState(() => _first = null),
            child: const Text('Empezar de nuevo'),
          ),
      ],
    );
  }

  IconData _icon(CredentialKind kind) => switch (kind) {
    CredentialKind.pin => Icons.dialpad_rounded,
    CredentialKind.password => Icons.password_rounded,
    CredentialKind.pattern => Icons.pattern_rounded,
  };
}
