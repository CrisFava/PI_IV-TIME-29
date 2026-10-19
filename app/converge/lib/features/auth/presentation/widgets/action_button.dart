import 'package:flutter/material.dart';

Widget buildActionButtonLabel(String text){
  return Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    children: [
      Text(text),
      const Icon(Icons.navigate_next)
    ],
  );
}