# lock_engine

Plugin Flutter con el motor nativo de bloqueo de Sigilo (Android, Kotlin).

- `pigeons/lock_engine_api.dart`: contrato tipado Dart ⇄ Kotlin.
- `lib/src/generated/`: código Dart generado (no editar).
- `android/src/main/kotlin/.../lock_engine/`: motor nativo, organizado por módulos
  (`security`, `rules`, `monitor`, `overlay`, `biometric`, `intruder`,
  `notifications`, `guest`, `apps`, `config`, `system`, `api`).

Regenerar el código tras cambiar el contrato:

```bash
dart run pigeon --input pigeons/lock_engine_api.dart
```
