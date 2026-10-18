import 'package:flutter/material.dart';

/// Tela provisória enquanto a interface real não é implementada.
class PlaceholderPage extends StatelessWidget {
  final String title;

  const PlaceholderPage(this.title, {super.key});

  @override
  Widget build(BuildContext context) =>
      Scaffold(body: Center(child: Text(title)));
}
