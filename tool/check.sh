#!/usr/bin/env bash
# Verificación completa del monorepo: formato, análisis estático y tests
# de los tres módulos Dart, y tests JVM del motor Kotlin (si hay Android SDK).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

for module in packages/design_system packages/lock_engine app; do
  echo "▶ $module"
  (
    cd "$ROOT/$module"
    flutter pub get >/dev/null
    dart format --output=none --set-exit-if-changed lib $( [ -d test ] && echo test )
    flutter analyze
    if [ -d test ]; then flutter test; fi
  )
done

if [ -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
  echo "▶ Tests Kotlin del motor (lock_engine)"
  # Genera el wrapper de Gradle y la configuración de plugins sin compilar.
  (cd "$ROOT/app" && flutter build apk --config-only >/dev/null)
  (cd "$ROOT/app/android" && ./gradlew :lock_engine:testDebugUnitTest)
else
  echo "⚠ ANDROID_HOME no definido: se omiten los tests Kotlin."
fi
