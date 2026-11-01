import 'package:flutter_test/flutter_test.dart';
import 'package:lock_engine/lock_engine.dart';
import 'package:sigilo/core/utils/formatters.dart';
import 'package:sigilo/features/apps/application/apps_providers.dart';

void main() {
  group('Formatters', () {
    final now = DateTime(2026, 11, 1, 18);

    test('relative', () {
      expect(Formatters.relative(DateTime(2026, 11, 1, 9, 5), now: now), 'Hoy, 09:05');
      expect(Formatters.relative(DateTime(2026, 10, 31, 23, 59), now: now), 'Ayer, 23:59');
      expect(Formatters.relative(DateTime(2026, 10, 12, 10), now: now), '12 oct, 10:00');
      expect(Formatters.relative(DateTime(2025, 1, 3, 7, 30), now: now), '3 ene 2025, 07:30');
    });

    test('days', () {
      expect(Formatters.days([1, 2, 3, 4, 5]), 'Lunes a viernes');
      expect(Formatters.days([6, 7]), 'Fines de semana');
      expect(Formatters.days([1, 2, 3, 4, 5, 6, 7]), 'Todos los días');
      expect(Formatters.days([5, 1, 3]), 'L, X, V');
    });

    test('minuteOfDay y duration', () {
      expect(Formatters.minuteOfDay(9 * 60 + 5), '09:05');
      expect(Formatters.duration(const Duration(seconds: 28)), '28 s');
      expect(Formatters.duration(const Duration(minutes: 2)), '2 min');
      expect(Formatters.duration(const Duration(seconds: 90)), '1 min 30 s');
    });
  });

  test('filterApps busca por nombre o paquete y filtra', () {
    final apps = [
      InstalledApp(
        packageName: 'com.whatsapp',
        label: 'WhatsApp',
        isSystem: false,
        canLock: true,
        isRecommended: true,
      ),
      InstalledApp(
        packageName: 'org.mozilla.firefox',
        label: 'Firefox',
        isSystem: false,
        canLock: true,
        isRecommended: false,
      ),
    ];
    expect(
      filterApps(apps, query: 'what', filter: AppFilter.all, locked: {}).single.label,
      'WhatsApp',
    );
    expect(
      filterApps(apps, query: 'mozilla', filter: AppFilter.all, locked: {}).single.label,
      'Firefox',
    );
    expect(
      filterApps(
        apps,
        query: '',
        filter: AppFilter.locked,
        locked: {'org.mozilla.firefox'},
      ).single.label,
      'Firefox',
    );
    expect(
      filterApps(apps, query: '', filter: AppFilter.recommended, locked: {}).single.label,
      'WhatsApp',
    );
  });
}
