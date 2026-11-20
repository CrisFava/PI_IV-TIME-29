// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/config/styles.dart';

/// Abre a foto de perfil expandida em tela cheia com suporte a zoom (pinch).
Future<void> showProfilePhotoViewer({
  required BuildContext context,
  required String photoUrl,
  required String userName,
  required String heroTag,
}) {
  HapticFeedback.lightImpact();
  return Navigator.of(context).push<void>(
    PageRouteBuilder<void>(
      opaque: false,
      barrierDismissible: true,
      barrierColor: AppColors.photoViewerBackdrop,
      transitionDuration: const Duration(milliseconds: 240),
      reverseTransitionDuration: const Duration(milliseconds: 200),
      pageBuilder: (_, _, _) => _ProfilePhotoViewerPage(
        photoUrl: photoUrl,
        userName: userName,
        heroTag: heroTag,
      ),
      transitionsBuilder: (_, animation, _, child) =>
          FadeTransition(opacity: animation, child: child),
    ),
  );
}

class _ProfilePhotoViewerPage extends StatelessWidget {
  final String photoUrl;
  final String userName;
  final String heroTag;

  const _ProfilePhotoViewerPage({
    required this.photoUrl,
    required this.userName,
    required this.heroTag,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.photoViewerBackdrop,
      body: SafeArea(
        child: Stack(
          children: [
            Center(
              child: InteractiveViewer(
                minScale: 0.8,
                maxScale: 4.0,
                child: Hero(
                  tag: heroTag,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(16),
                    child: Image.network(
                      photoUrl,
                      fit: BoxFit.contain,
                      loadingBuilder: (_, child, progress) {
                        if (progress == null) return child;
                        return const SizedBox(
                          width: 240,
                          height: 240,
                          child: Center(
                            child: CircularProgressIndicator(
                              color: AppColors.textLight,
                              strokeWidth: 2.5,
                            ),
                          ),
                        );
                      },
                      errorBuilder: (_, _, _) => const Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.broken_image_rounded,
                            size: 56,
                            color: AppColors.textLight,
                          ),
                          SizedBox(height: 12),
                          Text(
                            'Não foi possível carregar a foto.',
                            style: TextStyle(
                              fontSize: 14,
                              color: AppColors.textLight,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
            Positioned(
              top: 12,
              left: 16,
              right: 16,
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      userName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textLight,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  IconButton(
                    key: const ValueKey('close_profile_photo_viewer'),
                    tooltip: 'Fechar',
                    onPressed: () {
                      HapticFeedback.lightImpact();
                      Navigator.of(context).pop();
                    },
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.carButtonDepressed,
                      foregroundColor: AppColors.textLight,
                    ),
                    icon: const Icon(Icons.close_rounded, size: 22),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
