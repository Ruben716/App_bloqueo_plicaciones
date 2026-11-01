# design_system

Sistema de diseño de Sigilo, independiente de la lógica de negocio.

- `tokens/`: espaciado, radios y temas de la pantalla de bloqueo (`LockThemes`),
  sincronizados con `LockTheme.kt` del motor nativo.
- `theme/`: `AppTheme.light/dark` (Material 3 a partir del color de acento).
- `widgets/`: `PinPad`, `PinDots`, `PatternLock` (+ `PatternGeometry`, mismas
  reglas que el motor Kotlin), `Shake`, `GradientBackdrop`, `StatTile`,
  `SectionHeader`, `EmptyState`.
