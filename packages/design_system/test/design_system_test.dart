import 'package:design_system/design_system.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _wrap(Widget child) => MaterialApp(
  theme: AppTheme.dark(LockThemes.aurora),
  home: Scaffold(body: Center(child: child)),
);

void main() {
  group('PatternGeometry (mismas reglas que el motor Kotlin)', () {
    String draw(List<int> nodes) {
      final path = <int>[];
      for (final n in nodes) {
        PatternGeometry.append(path, n);
      }
      return PatternGeometry.encode(path);
    }

    test('agrega el nodo intermedio', () {
      expect(draw([0, 2]), '012');
      expect(draw([0, 8]), '048');
      expect(draw([2, 6]), '246');
    });

    test('no agrega intermedio en saltos no alineados', () {
      expect(PatternGeometry.middleNode(0, 5), isNull);
      expect(draw([0, 5]), '05');
    });

    test('no repite nodos', () {
      expect(draw([1, 4, 0, 2]), '1402');
      expect(draw([4, 1, 0, 1]), '410');
    });
  });

  testWidgets('PinPad emite dígitos y borrado', (tester) async {
    final digits = <String>[];
    var deletes = 0;
    await tester.pumpWidget(_wrap(PinPad(onDigit: digits.add, onDelete: () => deletes++)));
    await tester.tap(find.text('2'));
    await tester.tap(find.text('5'));
    await tester.tap(find.text('0'));
    await tester.tap(find.byTooltip('Borrar'));
    expect(digits, ['2', '5', '0']);
    expect(deletes, 1);
  });

  testWidgets('PinPad deshabilitado no emite dígitos', (tester) async {
    final digits = <String>[];
    await tester.pumpWidget(_wrap(PinPad(onDigit: digits.add, onDelete: () {}, enabled: false)));
    await tester.tap(find.text('7'));
    expect(digits, isEmpty);
  });

  testWidgets('PatternLock entrega la secuencia dibujada', (tester) async {
    List<int>? result;
    await tester.pumpWidget(_wrap(PatternLock(size: 300, onCompleted: (p) => result = p)));
    final origin = tester.getTopLeft(find.byType(PatternLock));
    // Centros de las celdas de 100 px: (50,50) nodo 0, (250,50) nodo 2, (250,250) nodo 8.
    final gesture = await tester.startGesture(origin + const Offset(50, 50));
    await gesture.moveTo(origin + const Offset(250, 50));
    await gesture.moveTo(origin + const Offset(250, 250));
    await gesture.up();
    await tester.pump();
    expect(result, [0, 1, 2, 5, 8]);
  });

  test('LockThemes.byId usa aurora por defecto', () {
    expect(LockThemes.byId('ocean').id, 'ocean');
    expect(LockThemes.byId('desconocido').id, 'aurora');
    expect(LockThemes.all.map((t) => t.id).toSet().length, LockThemes.all.length);
  });
}
