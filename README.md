# Sigilo — bloqueo de apps con credencial propia

App Android (Flutter + Kotlin) para proteger apps con un **PIN, contraseña o
patrón propio, totalmente independiente del bloqueo del teléfono**. Aunque
alguien conozca el PIN del móvil, no puede abrir tus apps protegidas.

## Funciones

| Función | Descripción |
|---|---|
| Bloqueo de apps | Pantalla de bloqueo nativa (overlay) al abrir una app protegida. |
| Credencial propia | PIN (4–12 dígitos), contraseña o patrón 3x3. Solo se guarda un hash (PBKDF2 + clave del Keystore). |
| Código de recuperación | 12 caracteres, se genera al crear la credencial. Sustituye al "olvidé mi PIN" sin depender del sistema. |
| Bloqueo progresivo | Tras 5 fallos, espera de 30 s que se duplica en cada fallo (máx. 15 min). |
| Huella (opcional) | Solo biometría fuerte y sin PIN del sistema como alternativa. **Se anula si se registra una huella nueva** en el teléfono. |
| Volver a bloquear | Al salir de la app, tras X minutos o al apagar la pantalla. |
| Perfiles con horario | Grupos de apps que solo se bloquean en ciertos días u horas (p. ej. redes sociales en horario laboral). |
| Modo invitado | Un toque (también desde Ajustes rápidos) bloquea todo salvo las apps permitidas. Desactivarlo pide la credencial. |
| Foto del intruso | Foto silenciosa con la cámara frontal tras N intentos fallidos, guardada en almacenamiento privado. |
| Ocultar notificaciones | Las notificaciones de apps protegidas se sustituyen por un aviso genérico, sin remitente ni texto. |
| Anti-desinstalación | Protege Ajustes, Play Store y el instalador de paquetes. |
| Pantalla falsa de error | Muestra "*App* se ha detenido"; mantén pulsado el título para desbloquear. |
| Privacidad de Sigilo | Sin permiso de Internet, sin copias de seguridad en la nube, capturas bloqueadas y la propia app se bloquea al salir. |
| Temas | 5 temas (Aurora, Océano, Bosque, Atardecer, Medianoche) compartidos por la app y el overlay nativo. |
| Historial | Desbloqueos, intentos fallidos, modo invitado... (sin contenido sensible). |

## Stack

- **Flutter 3.47 / Dart 3.13**: toda la UI de la app (Material 3, Riverpod 3, go_router 18).
- **Kotlin**: motor nativo de bloqueo, solo con APIs del framework de Android (sin androidx).
- **Pigeon 29**: contrato tipado entre Dart y Kotlin.
- Android 8.0+ (minSdk 26), compilado contra Android 16 (API 36).

## Estructura (monorepo modular)

```
app/                        App Flutter (composición, rutas y pantallas)
  lib/core/                 Proveedores del motor, sesión, router, utilidades
  lib/features/<función>/   application/ (estado) + presentation/ (pantallas)
packages/
  design_system/            Tokens, tema y componentes (PinPad, PatternLock...)
  lock_engine/              Plugin: contrato Pigeon + motor nativo Kotlin
    pigeons/                Definición de la API Dart ⇄ Kotlin
    android/.../lock_engine/
      security/  rules/  monitor/  overlay/  biometric/  intruder/
      notifications/  guest/  apps/  config/  system/  api/
docs/ARQUITECTURA.md        Diseño detallado y decisiones técnicas
tool/check.sh               Formato + análisis + tests de todo el repo
```

Detalles en [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

## Ejecutar

Requisitos: Flutter 3.47.9 (canal stable), Android SDK con la plataforma 36 y
un teléfono o emulador con Android 8.0 o superior.

```bash
cd app
flutter pub get
flutter run            # en un dispositivo Android conectado
```

Al abrir la app por primera vez:
1. Crea tu credencial (PIN, contraseña o patrón) y confírmala.
2. Guarda el código de recuperación.
3. Concede **Acceso a uso** y **Mostrar sobre otras apps** (obligatorios).
4. Elige las apps que quieres proteger.

Para regenerar el código Pigeon tras cambiar el contrato:

```bash
cd packages/lock_engine && dart run pigeon --input pigeons/lock_engine_api.dart
```

## Permisos

| Permiso | Para qué | ¿Obligatorio? |
|---|---|---|
| Acceso a uso (`PACKAGE_USAGE_STATS`) | Saber qué app se abre | Sí |
| Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`) | Dibujar la pantalla de bloqueo | Sí |
| Servicio en primer plano (`specialUse`) | Mantener vivo el motor | Automático |
| Notificaciones | Aviso permanente del servicio | Recomendado |
| Acceso a notificaciones | Ocultar notificaciones de apps protegidas | Solo si se activa |
| Cámara | Foto del intruso | Solo si se activa |
| Sin restricciones de batería | Evitar que Xiaomi, Huawei, Oppo... cierren el servicio | Recomendado |

No se usan el servicio de **Accesibilidad** ni `QUERY_ALL_PACKAGES`: Google
Play los restringe mucho.

## Verificación

```bash
./tool/check.sh   # formato, análisis estático y tests (Dart + Kotlin si hay Android SDK)
```

- **Dart**: `flutter analyze` sin avisos en los 3 módulos; 20 tests
  (redirecciones del router, formateadores, filtros, flujos de bloqueo,
  onboarding y gestión de apps con un motor simulado, PIN pad y patrón).
- **Kotlin**: 39 tests JVM de la lógica pura (reglas de horario, modo
  invitado, políticas de rebloqueo, credenciales, bloqueo progresivo,
  recuperación y geometría del patrón).
- **CI** (`.github/workflows/ci.yml`): ejecuta lo anterior y compila el APK.

## Limitaciones conocidas

- **Solo Android.** En iOS no se pueden bloquear otras apps con una pantalla
  propia: habría que usar la Screen Time API (`FamilyControls` +
  `ManagedSettings`), que necesita permiso especial de Apple. El contrato
  Pigeon ya está listo para añadir una implementación en Swift.
- Algunas pantallas del sistema (diálogo de desinstalación, ciertas
  pantallas de Ajustes) ocultan las ventanas superpuestas de otras apps
  (`HIDE_OVERLAY_WINDOWS`). La anti-desinstalación dificulta desinstalar la
  app, pero no es infalible.
- Detectar la app en primer plano con `UsageStats` tiene una latencia de
  unos 250 ms (intervalo de consulta).
- Google Play pide justificar el servicio en primer plano `specialUse` en
  la Play Console.

## Próximos pasos

- Dispositivos de confianza (Bluetooth / Wi-Fi) para no pedir la credencial en casa.
- Implementación iOS con Screen Time API.
- Bóveda privada de fotos y archivos.
- Traducciones (ARB/`gen-l10n`); ahora todos los textos están en español.
