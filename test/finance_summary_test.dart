import 'package:flutter_test/flutter_test.dart';
import 'package:meu_bolso/currency_input.dart';
import 'package:meu_bolso/finance_summary.dart';
import 'package:meu_bolso/models/finance_data.dart';

void main() {
  group('summarizeFinance', () {
    final today = DateTime(2026, 4, 10);
    final entries = [
      FinanceEntry(
        id: 'income',
        title: 'Salário',
        amount: 3000,
        type: EntryType.income,
        category: 'Salário',
        date: today,
      ),
      FinanceEntry(
        id: 'paid',
        title: 'Mercado',
        amount: 250,
        type: EntryType.expense,
        category: 'Alimentação',
        date: today,
      ),
      FinanceEntry(
        id: 'pending',
        title: 'Internet',
        amount: 100,
        type: EntryType.expense,
        category: 'Moradia',
        date: DateTime(2026, 4, 15),
        paid: false,
      ),
      FinanceEntry(
        id: 'old-month',
        title: 'Despesa antiga',
        amount: 75,
        type: EntryType.expense,
        category: 'Lazer',
        date: DateTime(2026, 3, 31),
      ),
    ];

    test('separa saldo, totais mensais e contas próximas', () {
      final summary = summarizeFinance(entries, today: today);

      expect(summary.balance, 2675);
      expect(summary.monthIncome, 3000);
      expect(summary.monthExpenses, 350);
      expect(summary.paidExpenses, 250);
      expect(summary.pendingBills.map((entry) => entry.id), ['pending']);
      expect(summary.upcomingBills.map((entry) => entry.id), ['pending']);
    });

    test('soma despesas mensais por categoria, incluindo contas pendentes', () {
      final summary = summarizeFinance(entries, today: today);

      expect(summary.categoryExpenses, {
        'Alimentação': 250,
        'Moradia': 100,
      });
    });

    test('não considera vencida uma conta com vencimento hoje', () {
      final billDueToday = FinanceEntry(
        id: 'today',
        title: 'Conta de água',
        amount: 60,
        type: EntryType.expense,
        category: 'Moradia',
        date: today,
        paid: false,
      );

      final summary = summarizeFinance([billDueToday], today: today);

      expect(summary.upcomingBills, [billDueToday]);
    });
  });

  group('parseCurrencyInput', () {
    test('interpreta centavos e separadores brasileiros', () {
      expect(parseCurrencyInput('1.234,56'), 1234.56);
      expect(parseCurrencyInput('1234,56'), 1234.56);
      expect(parseCurrencyInput('1.234'), 1234);
    });

    test('aceita ponto como separador decimal do teclado', () {
      expect(parseCurrencyInput('1234.56'), 1234.56);
    });
  });

  test('converte um lançamento para o formato de armazenamento local', () {
    final entry = FinanceEntry(
      id: 'account',
      title: 'Conta de luz',
      amount: 95.5,
      type: EntryType.expense,
      category: 'Moradia',
      date: DateTime(2026, 4, 12),
      paid: false,
    );

    expect(FinanceEntry.fromMap(entry.toMap()).toMap(), entry.toMap());
  });
}
