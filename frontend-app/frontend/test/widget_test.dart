import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:fronted/presentation/screens/splash/splash_screen.dart';

void main() {
  testWidgets('SplashScreen renders', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(
          home: SplashScreen(),
        ),
      ),
    );

    // Splash provider waits 1.8s before resolving; advance fake time
    // so no pending timer remains at test teardown.
    await tester.pump(const Duration(milliseconds: 1900));

    expect(find.byType(SplashScreen), findsOneWidget);
  });
}