// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

/// Situação da denúncia, igual ao enum `ReportStatus` do backend.
enum ReportStatus {
  pending('PENDING'),
  underReview('UNDER_REVIEW'),
  resolved('RESOLVED'),
  archived('ARCHIVED');

  final String value;

  const ReportStatus(this.value);

  static ReportStatus? fromJson(String? value) {
    for (final status in values) {
      if (status.value == value) return status;
    }
    return null;
  }
}
