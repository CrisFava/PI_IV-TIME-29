import 'package:firebase_auth/firebase_auth.dart' as fb;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_service.dart';
import 'firebase_auth_service.dart';

/// Sobrescreva em testes com `authServiceProvider.overrideWithValue(...)`.
final authServiceProvider = Provider<AuthService>(
  (ref) => FirebaseAuthService(fb.FirebaseAuth.instance),
);
