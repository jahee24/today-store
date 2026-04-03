import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fronted/presentation/screens/splash/splash_screen.dart';

void main() {
  testWidgets('SplashScreen renders', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: SplashScreen(),
      ),
    );

    expect(find.byType(SplashScreen), findsOneWidget);
  });
}