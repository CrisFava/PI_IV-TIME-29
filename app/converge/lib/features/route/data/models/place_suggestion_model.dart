import 'package:flutter/foundation.dart';

@immutable
class PlaceSuggestionModel {
  final String placeId;
  final String mainText;
  final String secondaryText;

  const PlaceSuggestionModel({
    required this.placeId,
    required this.mainText,
    required this.secondaryText,
  });

  factory PlaceSuggestionModel.fromJson(Map<String, dynamic> json) {
    return PlaceSuggestionModel(
      placeId: json['placeId'] as String? ?? '',
      mainText: json['mainText'] as String? ?? '',
      secondaryText: json['secondaryText'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'placeId': placeId,
      'mainText': mainText,
      'secondaryText': secondaryText,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PlaceSuggestionModel &&
          runtimeType == other.runtimeType &&
          placeId == other.placeId &&
          mainText == other.mainText &&
          secondaryText == other.secondaryText;

  @override
  int get hashCode => Object.hash(placeId, mainText, secondaryText);
}
