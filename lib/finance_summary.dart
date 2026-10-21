import 'models/finance_data.dart';

class FinanceSummary {
  const FinanceSummary({
    required this.balance,
    required this.monthIncome,
    required this.monthExpenses,
    required this.paidExpenses,
    required this.pendingBills,
    required this.upcomingBills,
    required this.categoryExpenses,
  });

  final double balance;
  final double monthIncome;
  final double monthExpenses;
  final double paidExpenses;
  final List<FinanceEntry> pendingBills;
  final List<FinanceEntry> upcomingBills;
  final Map<String, double> categoryExpenses;
}

FinanceSummary summarizeFinance(
  List<FinanceEntry> entries, {
  DateTime? today,
}) {
  final now = today ?? DateTime.now();
  final monthEntries = entries.where(
    (entry) =>
        entry.date.year == now.year && entry.date.month == now.month,
  );
  final monthIncome = monthEntries
      .where((entry) => entry.type == EntryType.income)
      .fold<double>(0, (total, entry) => total + entry.amount);
  final expenses = monthEntries
      .where((entry) => entry.type == EntryType.expense)
      .toList();
  final paidExpenses = expenses
      .where((entry) => entry.paid)
      .fold<double>(0, (total, entry) => total + entry.amount);
  final categoryExpenses = <String, double>{};
  for (final entry in expenses) {
    categoryExpenses.update(
      entry.category,
      (value) => value + entry.amount,
      ifAbsent: () => entry.amount,
    );
  }

  final pendingBills = entries
      .where((entry) => entry.type == EntryType.expense && !entry.paid)
      .toList()
    ..sort((a, b) => a.date.compareTo(b.date));
  final todayDate = DateTime(now.year, now.month, now.day);
  final upcomingBills = pendingBills.where((entry) {
    final dueDate = DateTime(entry.date.year, entry.date.month, entry.date.day);
    final difference = dueDate.difference(todayDate).inDays;
    return difference >= 0 && difference <= 7;
  }).toList();

  final balance = entries.fold<double>(0, (total, entry) {
    if (entry.type == EntryType.income) {
      return total + entry.amount;
    }
    return entry.paid ? total - entry.amount : total;
  });

  return FinanceSummary(
    balance: balance,
    monthIncome: monthIncome,
    monthExpenses: expenses.fold<double>(
      0,
      (total, entry) => total + entry.amount,
    ),
    paidExpenses: paidExpenses,
    pendingBills: pendingBills,
    upcomingBills: upcomingBills,
    categoryExpenses: categoryExpenses,
  );
}

double monthlyExpensesFor(
  List<FinanceEntry> entries,
  DateTime month,
) =>
    entries
        .where(
          (entry) =>
              entry.type == EntryType.expense &&
              entry.date.year == month.year &&
              entry.date.month == month.month,
        )
        .fold<double>(0, (total, entry) => total + entry.amount);
