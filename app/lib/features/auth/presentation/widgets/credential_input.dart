import 'dart:async';

import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:lock_engine/lock_engine.dart';

/// Entrada de la credencial propia según su tipo: PIN, contraseña o patrón.
///
/// - Con [pinLength] el PIN se confirma solo al completar los dígitos.
/// - Cada vez que cambia [errorTick] se sacude y se limpia la entrada.
class CredentialInput extends StatefulWidget {
  const CredentialInput({
    required this.kind,
    required this.onSubmit,
    this.pinLength,
    this.errorTick = 0,
    this.enabled = true,
    this.onBiometric,
    this.foregroundColor,
    this.submitLabel = 'Continuar',
    super.key,
  });

  final CredentialKind kind;
  final ValueChanged<String> onSubmit;
  final int? pinLength;
  final int errorTick;
  final bool enabled;
  final VoidCallback? onBiometric;
  final Color? foregroundColor;
  final String submitLabel;

  static const minPinLength = 4;
  static const maxPinLength = 12;

  @override
  State<CredentialInput> createState() => _CredentialInputState();
}

class _CredentialInputState extends State<CredentialInput> {
  final _password = TextEditingController();
  String _pin = '';
  bool _obscure = true;
  bool _showError = false;
  Timer? _errorTimer;

  @override
  void didUpdateWidget(CredentialInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.errorTick != widget.errorTick) {
      _pin = '';
      _password.clear();
      _showError = true;
      _errorTimer?.cancel();
      _errorTimer = Timer(const Duration(milliseconds: 900), () {
        if (mounted) setState(() => _showError = false);
      });
    }
  }

  @override
  void dispose() {
    _errorTimer?.cancel();
    _password.dispose();
    super.dispose();
  }

  void _onDigit(String digit) {
    if (_pin.length >= CredentialInput.maxPinLength) return;
    setState(() => _pin += digit);
    final expected = widget.pinLength;
    if (expected != null && _pin.length == expected) widget.onSubmit(_pin);
  }

  @override
  Widget build(BuildContext context) {
    return switch (widget.kind) {
      CredentialKind.pin => _buildPin(context),
      CredentialKind.password => _buildPassword(context),
      CredentialKind.pattern => _buildPattern(context),
    };
  }

  Widget _buildPin(BuildContext context) {
    final color = widget.foregroundColor ?? Theme.of(context).colorScheme.onSurface;
    final canConfirm = widget.pinLength == null && _pin.length >= CredentialInput.minPinLength;
    Widget? leading;
    if (widget.onBiometric != null) {
      leading = _BiometricButton(onPressed: widget.onBiometric!, color: color);
    } else if (widget.pinLength == null) {
      leading = IconButton.filledTonal(
        tooltip: widget.submitLabel,
        onPressed: canConfirm && widget.enabled ? () => widget.onSubmit(_pin) : null,
        icon: const Icon(Icons.arrow_forward_rounded),
      );
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Shake(
          trigger: widget.errorTick,
          child: PinDots(
            filled: _pin.length,
            total: widget.pinLength ?? CredentialInput.minPinLength,
            color: color,
            error: _showError,
          ),
        ),
        const SizedBox(height: Space.xxl),
        PinPad(
          enabled: widget.enabled,
          foregroundColor: color,
          leading: leading,
          onDigit: _onDigit,
          onDelete: () {
            if (_pin.isNotEmpty) setState(() => _pin = _pin.substring(0, _pin.length - 1));
          },
        ),
      ],
    );
  }

  Widget _buildPassword(BuildContext context) {
    void submit() {
      if (_password.text.isNotEmpty && widget.enabled) widget.onSubmit(_password.text);
    }

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Space.xl),
      child: Shake(
        trigger: widget.errorTick,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _password,
              enabled: widget.enabled,
              autofocus: true,
              obscureText: _obscure,
              enableSuggestions: false,
              autocorrect: false,
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => submit(),
              decoration: InputDecoration(
                hintText: 'Contraseña',
                errorText: _showError ? '' : null,
                prefixIcon: const Icon(Icons.key_rounded),
                suffixIcon: IconButton(
                  tooltip: _obscure ? 'Mostrar' : 'Ocultar',
                  onPressed: () => setState(() => _obscure = !_obscure),
                  icon: Icon(_obscure ? Icons.visibility_rounded : Icons.visibility_off_rounded),
                ),
              ),
            ),
            const SizedBox(height: Space.lg),
            FilledButton(
              onPressed: widget.enabled ? submit : null,
              child: Text(widget.submitLabel),
            ),
            if (widget.onBiometric != null) ...[
              const SizedBox(height: Space.xl),
              _BiometricButton(
                onPressed: widget.onBiometric!,
                color: widget.foregroundColor ?? Theme.of(context).colorScheme.onSurface,
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPattern(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Shake(
          trigger: widget.errorTick,
          child: PatternLock(
            enabled: widget.enabled,
            error: _showError,
            color: widget.foregroundColor,
            onCompleted: (path) => widget.onSubmit(PatternGeometry.encode(path)),
          ),
        ),
        if (widget.onBiometric != null) ...[
          const SizedBox(height: Space.lg),
          _BiometricButton(
            onPressed: widget.onBiometric!,
            color: widget.foregroundColor ?? Theme.of(context).colorScheme.onSurface,
          ),
        ],
      ],
    );
  }
}

class _BiometricButton extends StatelessWidget {
  const _BiometricButton({required this.onPressed, required this.color});

  final VoidCallback onPressed;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: 'Usar huella',
      iconSize: 32,
      onPressed: onPressed,
      icon: Icon(Icons.fingerprint_rounded, color: color),
    );
  }
}
