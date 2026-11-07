// Autor: Allan Giovanni Matias Paes - RA 25008211

class CpfValidationResult {
  final bool valid;
  final String? formattedCpf;
  final String message;

  const CpfValidationResult({
    required this.valid,
    this.formattedCpf,
    required this.message,
  });

  factory CpfValidationResult.fromJson(Map<String, dynamic> json) =>
      CpfValidationResult(
        valid: json['valid'] as bool? ?? false,
        formattedCpf: json['formattedCpf'] as String?,
        message: json['message'] as String? ?? '',
      );
}
