// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

abstract final class SecurityEndpoints {
  static const String blocks = '/blocks';
  static String block(String blockedUserId) => '/blocks/$blockedUserId';
  static const String reports = '/reports';
  static const String myReports = '/reports/mine';
}
