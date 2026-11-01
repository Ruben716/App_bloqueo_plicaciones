import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lock_engine/lock_engine.dart';
import 'package:sigilo/app.dart';
import 'package:sigilo/core/engine/engine_providers.dart';

import 'fakes/fake_engine_api.dart';

Future<void> _pumpApp(WidgetTester tester, FakeEngineApi api) async {
  tester.view.physicalSize = const Size(1080, 2400);
  tester.view.devicePixelRatio = 2.5;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [lockEngineApiProvider.overrideWithValue(api)],
      retry: (_, _) => null,
      child: const SigiloApp(),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> _enterPin(WidgetTester tester, String pin) async {
  for (final digit in pin.split('')) {
    await tester.tap(find.text(digit).last);
    await tester.pump();
  }
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('con credencial, la app arranca bloqueada y el PIN correcto la abre', (tester) async {
    final api = FakeEngineApi();
    await _pumpApp(tester, api);

    expect(find.text('Introduce tu PIN'), findsOneWidget);

    await _enterPin(tester, '1111');
    expect(find.text('Incorrecto. Inténtalo de nuevo.'), findsOneWidget);

    await _enterPin(tester, '2580');
    expect(api.verifyCalls, ['1111', '2580']);
    expect(find.text('Protección activa'), findsOneWidget);
    expect(find.text('Desbloqueaste WhatsApp'), findsOneWidget);
  });

  testWidgets('sin credencial se muestra el onboarding y se crea un PIN propio', (tester) async {
    final api = FakeEngineApi(credentialSet: false);
    await _pumpApp(tester, api);

    expect(find.text('Tus apps, solo para ti'), findsOneWidget);
    await tester.tap(find.text('Siguiente'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Siguiente'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Empezar'));
    await tester.pumpAndSettle();

    expect(find.text('Crea tu PIN'), findsOneWidget);

    // PIN débil: lo rechaza la política del motor.
    await _enterPin(tester, '1234');
    await tester.tap(find.byTooltip('Continuar'));
    await tester.pumpAndSettle();
    expect(find.text('Evita secuencias como 1234'), findsOneWidget);

    await _enterPin(tester, '2468');
    await tester.tap(find.byTooltip('Continuar'));
    await tester.pumpAndSettle();
    expect(find.text('Confirma tu PIN'), findsOneWidget);

    // Confirmación distinta.
    await _enterPin(tester, '2469');
    expect(find.text('No coincide. Inténtalo otra vez.'), findsOneWidget);

    await _enterPin(tester, '2468');
    expect(api.credentialSet, isTrue);
    expect(api.secret, '2468');
    expect(find.text('ABCD-EFGH-JKLM'), findsOneWidget);
  });

  testWidgets('la pestaña Apps bloquea y desbloquea apps del perfil principal', (tester) async {
    final api = FakeEngineApi();
    await _pumpApp(tester, api);
    await _enterPin(tester, '2580');

    await tester.tap(find.text('Apps'));
    await tester.pumpAndSettle();
    expect(find.text('1 apps siempre protegidas'), findsOneWidget);

    await tester.tap(find.widgetWithText(SwitchListTile, 'Chrome'));
    await tester.pumpAndSettle();
    expect(api.profiles.first.packages, containsAll(['com.whatsapp', 'com.android.chrome']));
    expect(find.text('2 apps siempre protegidas'), findsOneWidget);

    // Las apps esenciales no se pueden bloquear.
    final launcher = tester.widget<SwitchListTile>(find.widgetWithText(SwitchListTile, 'Launcher'));
    expect(launcher.onChanged, isNull);
  });

  testWidgets('la contraseña usa campo de texto', (tester) async {
    final api = FakeEngineApi(credentialKind: CredentialKind.password, secret: 'mi clave');
    await _pumpApp(tester, api);
    expect(find.text('Introduce tu contraseña'), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'mi clave');
    await tester.tap(find.text('Desbloquear'));
    await tester.pumpAndSettle();
    expect(find.text('Protección activa'), findsOneWidget);
  });
}
