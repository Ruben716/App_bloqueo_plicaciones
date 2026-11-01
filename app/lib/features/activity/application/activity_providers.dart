import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/session/app_session.dart';

final activityLogProvider = FutureProvider<List<ActivityEntry>>((ref) {
  ref.watch(appResumeTickProvider);
  return ref.watch(lockEngineApiProvider).getActivityLog();
});
