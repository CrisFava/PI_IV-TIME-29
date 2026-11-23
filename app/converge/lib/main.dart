// Autor: Cristian Eduardo Fava - RA 25000636

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app_widget.dart';
import 'firebase_options.dart';

/// Ponto de entrada principal do aplicativo Converge Mobile.
/// Inicializa os bindings do Flutter, variáveis de ambiente (.env), Firebase e Riverpod.
///
/// A ordem importa:
/// 1. `ensureInitialized` antes de qualquer plugin nativo.
/// 2. O `.env` (com `API_BASE_URL` e `GOOGLE_MAPS_API_KEY`). Se ele faltar, o app abre mesmo assim e
///    avisa só quando precisar da variável, em vez de travar na tela preta.
/// 3. O Firebase, uma vez só. O `duplicate-app` acontece no hot restart, quando o lado nativo já
///    tem o app registrado, e pode ser ignorado.
/// 4. O [ProviderScope] do Riverpod envolvendo o [AppWidget].
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    await dotenv.load(fileName: '.env');
  } catch (e) {
    debugPrint('Erro ao carregar .env: $e');
  }

  if (Firebase.apps.isEmpty) {
    try {
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    } on FirebaseException catch (e) {
      if (e.code != 'duplicate-app') {
        rethrow;
      }
    }
  }

  runApp(const ProviderScope(child: AppWidget()));
}
