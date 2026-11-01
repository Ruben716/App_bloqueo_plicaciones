import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/routes.dart';
import '../../settings/application/settings_controller.dart';

class WelcomePage extends ConsumerStatefulWidget {
  const WelcomePage({super.key});

  @override
  ConsumerState<WelcomePage> createState() => _WelcomePageState();
}

class _WelcomePageState extends ConsumerState<WelcomePage> {
  final _controller = PageController();
  int _page = 0;

  static const _slides = [
    _Slide(
      icon: Icons.shield_rounded,
      title: 'Tus apps, solo para ti',
      text: 'Protege WhatsApp, Galería, Instagram o cualquier app con un bloqueo al abrirlas.',
    ),
    _Slide(
      icon: Icons.password_rounded,
      title: 'Un PIN solo de Sigilo',
      text:
          'Tu PIN, contraseña o patrón es independiente del bloqueo del teléfono. '
          'Aunque alguien conozca el PIN del móvil, no podrá abrir tus apps.',
    ),
    _Slide(
      icon: Icons.person_off_rounded,
      title: 'Presta el móvil sin miedo',
      text:
          'Modo invitado, foto del intruso, notificaciones ocultas y horarios. '
          'Todo funciona sin conexión: Sigilo no tiene acceso a Internet.',
    ),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final lockTheme = LockThemes.byId(ref.watch(themeIdProvider));
    final last = _page == _slides.length - 1;
    return Theme(
      data: AppTheme.dark(lockTheme),
      child: Scaffold(
        body: GradientBackdrop(
          theme: lockTheme,
          child: SafeArea(
            child: Column(
              children: [
                Expanded(
                  child: PageView(
                    controller: _controller,
                    onPageChanged: (page) => setState(() => _page = page),
                    children: [
                      for (final slide in _slides)
                        _SlideView(slide: slide, accent: lockTheme.accent),
                    ],
                  ),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    for (var i = 0; i < _slides.length; i++)
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
                        margin: const EdgeInsets.all(4),
                        width: i == _page ? 24 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: i == _page ? lockTheme.accent : Colors.white24,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.all(Space.xl),
                  child: FilledButton(
                    onPressed: () => last
                        ? context.go(Routes.onboardingCredential)
                        : _controller.nextPage(
                            duration: const Duration(milliseconds: 300),
                            curve: Curves.easeOutCubic,
                          ),
                    child: Text(last ? 'Empezar' : 'Siguiente'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Slide {
  const _Slide({required this.icon, required this.title, required this.text});

  final IconData icon;
  final String title;
  final String text;
}

class _SlideView extends StatelessWidget {
  const _SlideView({required this.slide, required this.accent});

  final _Slide slide;
  final Color accent;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(Space.xxl),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(Space.xxl),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: accent.withValues(alpha: 0.18),
            ),
            child: Icon(slide.icon, size: 72, color: accent),
          ),
          const SizedBox(height: Space.xxl),
          Text(
            slide.title,
            textAlign: TextAlign.center,
            style: theme.textTheme.headlineMedium?.copyWith(color: Colors.white),
          ),
          const SizedBox(height: Space.lg),
          Text(
            slide.text,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyLarge?.copyWith(color: Colors.white70, height: 1.5),
          ),
        ],
      ),
    );
  }
}
