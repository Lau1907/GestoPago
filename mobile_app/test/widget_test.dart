import 'package:flutter_test/flutter_test.dart';
import 'package:mobile_app/main.dart';

void main() {
  testWidgets('BancaMovilApp smoke test inicializa correctamente', (WidgetTester tester) async {
    await tester.pumpWidget(const BancaMovilApp());
    expect(find.byType(BancaMovilApp), findsOneWidget);
  });
}
