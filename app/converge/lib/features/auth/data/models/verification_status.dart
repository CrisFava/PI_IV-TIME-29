/// Espelha `VerificationStatus` do backend.
enum VerificationStatus {
  verified,
  underReview,
  rejected,
  pending,
  expired;

  String toJson() => switch (this) {
    verified => 'VERIFIED',
    underReview => 'UNDER_REVIEW',
    rejected => 'REJECTED',
    pending => 'PENDING',
    expired => 'EXPIRED',
  };

  static VerificationStatus fromJson(String? value) => values.firstWhere(
    (status) => status.toJson() == value,
    orElse: () => pending,
  );
}
