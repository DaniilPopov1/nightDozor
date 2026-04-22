import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/presentation/pages/splash_page.dart';

void main() {
  testWidgets('Splash page renders loader', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: SplashPage(),
      ),
    );

    expect(find.byType(SplashPage), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
