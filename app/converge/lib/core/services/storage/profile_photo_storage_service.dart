// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'dart:typed_data';

import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_exception.dart';

/// Contrato para upload da foto de perfil do usuário.
abstract class ProfilePhotoStorageService {
  /// Faz upload dos bytes da imagem e devolve a URL pública de download.
  Future<String> uploadProfilePhoto({
    required String firebaseUid,
    required Uint8List bytes,
    required String fileName,
  });
}

final profilePhotoStorageProvider = Provider<ProfilePhotoStorageService>(
  (_) => FirebaseProfilePhotoStorageService(FirebaseStorage.instance),
);

/// Implementação com Firebase Storage no caminho:
/// `profile/{firebaseUid}/pictures/{nomeDaFoto.ext}`.
class FirebaseProfilePhotoStorageService implements ProfilePhotoStorageService {
  final FirebaseStorage _storage;

  FirebaseProfilePhotoStorageService(this._storage);

  /// Constrói o caminho normalizado no bucket preservando a extensão original.
  static String buildStoragePath({
    required String firebaseUid,
    required String fileName,
  }) {
    final sanitized = sanitizeFileName(fileName);
    return 'profile/$firebaseUid/pictures/$sanitized';
  }

  /// Remove caminhos de diretório e caracteres inválidos preservando a extensão.
  static String sanitizeFileName(String rawName) {
    final trimmed = rawName.trim();
    final base = trimmed.split(RegExp(r'[/\\]')).last;
    final cleaned = base.replaceAll(RegExp(r'[^a-zA-Z0-9._-]'), '_');
    if (cleaned.isEmpty || cleaned == '.' || cleaned == '..') {
      return 'avatar_${DateTime.now().millisecondsSinceEpoch}.jpg';
    }
    if (!cleaned.contains('.')) {
      return '$cleaned.jpg';
    }
    return cleaned;
  }

  static String _contentTypeFor(String fileName) {
    final dot = fileName.lastIndexOf('.');
    if (dot == -1 || dot == fileName.length - 1) return 'image/jpeg';
    final ext = fileName.substring(dot + 1).toLowerCase();
    return switch (ext) {
      'png' => 'image/png',
      'jpg' || 'jpeg' => 'image/jpeg',
      'webp' => 'image/webp',
      'gif' => 'image/gif',
      'heic' => 'image/heic',
      'heif' => 'image/heif',
      'bmp' => 'image/bmp',
      _ => 'application/octet-stream',
    };
  }

  @override
  Future<String> uploadProfilePhoto({
    required String firebaseUid,
    required Uint8List bytes,
    required String fileName,
  }) async {
    final path = buildStoragePath(firebaseUid: firebaseUid, fileName: fileName);
    final metadata = SettableMetadata(contentType: _contentTypeFor(path));

    try {
      final ref = _storage.ref().child(path);
      await ref.putData(bytes, metadata);
      return await ref.getDownloadURL();
    } on FirebaseException catch (e) {
      throw AuthException(e.code, _messageFor(e.code));
    }
  }

  static String _messageFor(String code) => switch (code) {
    'unauthorized' || 'unauthenticated' =>
      'Sem permissão para enviar a foto de perfil.',
    'canceled' => 'Envio da foto cancelado.',
    'retry-limit-exceeded' =>
      'Tempo esgotado ao enviar a foto. Verifique sua conexão e tente novamente.',
    _ => 'Não foi possível enviar a foto de perfil. Tente novamente.',
  };
}
