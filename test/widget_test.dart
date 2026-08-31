import 'package:flutter_test/flutter_test.dart';
import 'package:takami_nexus/main.dart';

void main() {
  testWidgets('Takami app builds without crashing', (WidgetTester tester) async {
    await tester.pumpWidget(const TakamiApp());
    await tester.pump(const Duration(milliseconds: 100));
    expect(find.byType(TakamiApp), findsOneWidget);
  });
}
