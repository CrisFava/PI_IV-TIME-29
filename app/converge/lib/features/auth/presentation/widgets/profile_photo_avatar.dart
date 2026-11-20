// Autor: Allan Giovanni Matias Paes - RA 25008211

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../core/config/styles.dart';
import '../../../../core/widgets/show_app_notice.dart';
import '../../data/models/user_model.dart';
import '../../state/auth_action_controller.dart';
import '../../state/auth_controller.dart';
import 'profile_photo_viewer_dialog.dart';

/// Resultado de uma foto selecionada pelo usuário.
@immutable
class PickedProfilePhoto {
  final Uint8List bytes;
  final String fileName;

  const PickedProfilePhoto({required this.bytes, required this.fileName});
}

typedef ProfilePhotoPicker = Future<PickedProfilePhoto?> Function(
  ImageSource source,
);

/// Animação de shimmer circular exibida durante o upload ou carregamento
/// da foto de perfil.
class ProfilePhotoShimmer extends StatefulWidget {
  final double size;

  const ProfilePhotoShimmer({super.key, required this.size});

  @override
  State<ProfilePhotoShimmer> createState() => _ProfilePhotoShimmerState();
}

class _ProfilePhotoShimmerState extends State<ProfilePhotoShimmer>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1150),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (_, _) {
        final slide = -1.5 + (_controller.value * 3.0);
        return Container(
          key: const ValueKey('profile_photo_shimmer'),
          width: widget.size,
          height: widget.size,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: LinearGradient(
              begin: Alignment(slide - 1.0, -0.6),
              end: Alignment(slide + 1.0, 0.6),
              colors: const [
                AppColors.shimmerBase,
                AppColors.shimmerHighlight,
                AppColors.shimmerBase,
              ],
              stops: const [0.15, 0.5, 0.85],
            ),
          ),
        );
      },
    );
  }
}

/// Avatar interativo do perfil: exibe a foto do usuário (ou ícone padrão),
/// anima com shimmer ao atualizar e abre opções para "Ver foto" e "Alterar foto".
class ProfilePhotoAvatar extends ConsumerStatefulWidget {
  final UserModel user;
  final double radius;

  /// Injetável para testes; em produção usa [ImagePicker].
  final ProfilePhotoPicker? photoPicker;

  const ProfilePhotoAvatar({
    super.key,
    required this.user,
    this.radius = 42,
    this.photoPicker,
  });

  @override
  ConsumerState<ProfilePhotoAvatar> createState() => _ProfilePhotoAvatarState();
}

class _ProfilePhotoAvatarState extends ConsumerState<ProfilePhotoAvatar> {
  final ImagePicker _picker = ImagePicker();
  bool _isUpdating = false;

  String get _heroTag => 'profile_photo_${widget.user.id}';

  bool get _hasPhoto =>
      widget.user.photoUrl != null && widget.user.photoUrl!.trim().isNotEmpty;

  Future<PickedProfilePhoto?> _pickImage(ImageSource source) async {
    if (widget.photoPicker != null) {
      return widget.photoPicker!(source);
    }
    final picked = await _picker.pickImage(
      source: source,
      maxWidth: 1024,
      maxHeight: 1024,
      imageQuality: 85,
    );
    if (picked == null) return null;
    final bytes = await picked.readAsBytes();
    return PickedProfilePhoto(bytes: bytes, fileName: picked.name);
  }

  Future<void> _onAvatarTap() async {
    if (_isUpdating) return;
    HapticFeedback.lightImpact();

    final action = await showModalBottomSheet<_PhotoAction>(
      context: context,
      backgroundColor: AppColors.cardBackground,
      barrierColor: AppColors.barrier,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (sheetContext) => _ProfilePhotoOptionsSheet(hasPhoto: _hasPhoto),
    );

    if (!mounted || action == null) return;

    switch (action) {
      case _PhotoAction.view:
        if (_hasPhoto) {
          await showProfilePhotoViewer(
            context: context,
            photoUrl: widget.user.photoUrl!,
            userName: widget.user.name,
            heroTag: _heroTag,
          );
        }
      case _PhotoAction.change:
        await _chooseSourceAndUpload();
    }
  }

  Future<void> _chooseSourceAndUpload() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      backgroundColor: AppColors.cardBackground,
      barrierColor: AppColors.barrier,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => const _ImageSourceSheet(),
    );

    if (!mounted || source == null) return;
    await _uploadFromSource(source);
  }

  Future<void> _uploadFromSource(ImageSource source) async {
    try {
      final picked = await _pickImage(source);
      if (!mounted || picked == null) return;

      HapticFeedback.mediumImpact();
      setState(() => _isUpdating = true);

      await ref
          .read(authControllerProvider.notifier)
          .updateProfilePhoto(bytes: picked.bytes, fileName: picked.fileName);

      if (!mounted) return;
      showAppNotice(
        context,
        'Foto de perfil atualizada.',
        type: AppNoticeType.success,
      );
    } catch (error) {
      if (!mounted) return;
      showAppNotice(
        context,
        authErrorMessage(error),
        type: AppNoticeType.error,
      );
    } finally {
      if (mounted) {
        setState(() => _isUpdating = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final diameter = widget.radius * 2;
    final photoUrl = widget.user.photoUrl?.trim();

    Widget avatarContent;
    if (_isUpdating) {
      avatarContent = ProfilePhotoShimmer(size: diameter);
    } else if (photoUrl != null && photoUrl.isNotEmpty) {
      avatarContent = Hero(
        tag: _heroTag,
        child: Image.network(
          photoUrl,
          key: ValueKey('profile_photo_image_$photoUrl'),
          width: diameter,
          height: diameter,
          fit: BoxFit.cover,
          loadingBuilder: (_, child, progress) {
            if (progress == null) return child;
            return ProfilePhotoShimmer(size: diameter);
          },
          errorBuilder: (_, _, _) => _DefaultAvatarContent(size: diameter),
        ),
      );
    } else {
      avatarContent = _DefaultAvatarContent(size: diameter);
    }

    return Semantics(
      button: true,
      enabled: !_isUpdating,
      label: 'Foto de perfil. Toque para ver ou alterar.',
      child: GestureDetector(
        key: const ValueKey('profile_photo_avatar'),
        onTap: _isUpdating ? null : _onAvatarTap,
        child: SizedBox(
          width: diameter,
          height: diameter,
          child: Stack(
            clipBehavior: Clip.none,
            children: [
              Container(
                width: diameter,
                height: diameter,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.neutralLight,
                  border: Border.all(color: AppColors.borderSubtle, width: 1.5),
                ),
                clipBehavior: Clip.antiAlias,
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 280),
                  child: avatarContent,
                ),
              ),
              Positioned(
                right: -2,
                bottom: -2,
                child: Container(
                  width: 28,
                  height: 28,
                  decoration: BoxDecoration(
                    color: _isUpdating
                        ? AppColors.textMuted
                        : AppColors.primary,
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: AppColors.cardBackground,
                      width: 2.5,
                    ),
                  ),
                  child: Icon(
                    _isUpdating ? Icons.sync_rounded : Icons.camera_alt_rounded,
                    size: 14,
                    color: AppColors.textLight,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DefaultAvatarContent extends StatelessWidget {
  final double size;

  const _DefaultAvatarContent({required this.size});

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const ValueKey('profile_photo_placeholder'),
      width: size,
      height: size,
      color: AppColors.neutralLight,
      alignment: Alignment.center,
      child: Icon(
        Icons.person_rounded,
        size: size * 0.52,
        color: AppColors.textPrimary,
      ),
    );
  }
}

enum _PhotoAction { view, change }

class _ProfilePhotoOptionsSheet extends StatelessWidget {
  final bool hasPhoto;

  const _ProfilePhotoOptionsSheet({required this.hasPhoto});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 38,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.borderMedium,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Foto de perfil',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            _SheetOptionTile(
              key: const ValueKey('profile_photo_view_option'),
              icon: Icons.fullscreen_rounded,
              title: 'Ver foto',
              subtitle: hasPhoto
                  ? 'Expandir foto de perfil em tela cheia'
                  : 'Nenhuma foto cadastrada ainda',
              enabled: hasPhoto,
              onTap: () {
                HapticFeedback.lightImpact();
                Navigator.of(context).pop(_PhotoAction.view);
              },
            ),
            const SizedBox(height: 8),
            _SheetOptionTile(
              key: const ValueKey('profile_photo_change_option'),
              icon: Icons.photo_camera_rounded,
              title: 'Alterar foto',
              subtitle: 'Escolher da galeria ou tirar uma nova foto',
              enabled: true,
              onTap: () {
                HapticFeedback.lightImpact();
                Navigator.of(context).pop(_PhotoAction.change);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _ImageSourceSheet extends StatelessWidget {
  const _ImageSourceSheet();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 38,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.borderMedium,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Escolher foto',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            _SheetOptionTile(
              key: const ValueKey('profile_photo_gallery_option'),
              icon: Icons.photo_library_rounded,
              title: 'Galeria',
              subtitle: 'Selecionar uma imagem do dispositivo',
              enabled: true,
              onTap: () {
                HapticFeedback.lightImpact();
                Navigator.of(context).pop(ImageSource.gallery);
              },
            ),
            const SizedBox(height: 8),
            _SheetOptionTile(
              key: const ValueKey('profile_photo_camera_option'),
              icon: Icons.camera_alt_rounded,
              title: 'Câmera',
              subtitle: 'Tirar uma foto agora',
              enabled: true,
              onTap: () {
                HapticFeedback.lightImpact();
                Navigator.of(context).pop(ImageSource.camera);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _SheetOptionTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final bool enabled;
  final VoidCallback onTap;

  const _SheetOptionTile({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.neutralLight,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: AppColors.cardBackground,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  icon,
                  size: 20,
                  color: enabled ? AppColors.textPrimary : AppColors.iconMuted,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: enabled
                            ? AppColors.textPrimary
                            : AppColors.textMuted,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: TextStyle(
                        fontSize: 12.5,
                        color: enabled
                            ? AppColors.textSecondary
                            : AppColors.textMuted,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                size: 20,
                color: enabled ? AppColors.textMuted : AppColors.borderMedium,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
