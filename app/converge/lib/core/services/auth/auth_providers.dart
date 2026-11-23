// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:firebase_auth/firebase_auth.dart' as fb;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_service.dart';
import 'firebase_auth_service.dart';

/// Serviço de identidade da aplicação, ligado ao Firebase Auth.
///
/// Sobrescreva em testes com `authServiceProvider.overrideWithValue(...)`.
final authServiceProvider = Provider<AuthService>(
  (ref) => FirebaseAuthService(fb.FirebaseAuth.instance),
);
