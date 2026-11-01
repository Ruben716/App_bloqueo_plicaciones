import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lock_engine/lock_engine.dart';

import '../../../core/engine/engine_providers.dart';
import '../../../core/session/app_session.dart';

final intrudersProvider = FutureProvider<List<IntruderPhoto>>((ref) {
  ref.watch(appResumeTickProvider);
  return ref.watch(lockEngineApiProvider).getIntruderPhotos();
});
