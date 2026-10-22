import 'package:flutter/foundation.dart';

@immutable
class PlaceDetailsModel {
  final String address;
  final String? neighborhood;
  final String city;
  final double latitude;
  final double longitude;

  const PlaceDetailsModel({
    required this.address,
    this.neighborhood,
    required this.city,
    required this.latitude,
    required this.longitude,
  });

  factory PlaceDetailsModel.fromJson(Map<String, dynamic> json) {
    return PlaceDetailsModel(
      address: json['address'] as String? ?? '',
      neighborhood: json['neighborhood'] as String?,
      city: json['city'] as String? ?? '',
      latitude: (json['latitude'] as num?)?.toDouble() ?? 0.0,
      longitude: (json['longitude'] as num?)?.toDouble() ?? 0.0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'address': address,
      if (neighborhood != null) 'neighborhood': neighborhood,
      'city': city,
      'latitude': latitude,
      'longitude': longitude,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PlaceDetailsModel &&
          runtimeType == other.runtimeType &&
          address == other.address &&
          neighborhood == other.neighborhood &&
          city == other.city &&
          latitude == other.latitude &&
          longitude == other.longitude;

  @override
  int get hashCode => Object.hash(address, neighborhood, city, latitude, longitude);
}
