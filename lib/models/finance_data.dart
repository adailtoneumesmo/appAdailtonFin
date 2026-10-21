enum EntryType { income, expense }

class FinanceEntry {
  const FinanceEntry({
    required this.id,
    required this.title,
    required this.amount,
    required this.type,
    required this.category,
    required this.date,
    this.paid = true,
  });

  final String id;
  final String title;
  final double amount;
  final EntryType type;
  final String category;
  final DateTime date;
  final bool paid;

  Map<String, Object?> toMap() => {
        'id': id,
        'title': title,
        'amount': amount,
        'type': type.name,
        'category': category,
        'date': date.toIso8601String(),
        'paid': paid ? 1 : 0,
      };

  factory FinanceEntry.fromMap(Map<String, Object?> map) {
    final typeName = map['type'] as String?;
    final type = EntryType.values.firstWhere(
      (value) => value.name == typeName,
      orElse: () => throw FormatException(
        'Tipo de lançamento inválido: $typeName',
      ),
    );
    final date = DateTime.tryParse(map['date'] as String? ?? '');
    if (date == null) {
      throw const FormatException('Data de lançamento inválida.');
    }
    final amount = map['amount'];
    if (amount is! num) {
      throw const FormatException('Valor de lançamento inválido.');
    }
    final paidValue = map['paid'];
    final paid = switch (paidValue) {
      bool value => value,
      num value => value != 0,
      _ => throw const FormatException('Estado de pagamento inválido.'),
    };

    return FinanceEntry(
      id: map['id'] as String,
      title: map['title'] as String,
      amount: amount.toDouble(),
      type: type,
      category: map['category'] as String,
      date: date,
      paid: paid,
    );
  }
}

class FinanceData {
  FinanceData({
    required List<FinanceEntry> entries,
    required Map<String, double> budgets,
  })  : entries = List.unmodifiable(entries),
        budgets = Map.unmodifiable(budgets);

  factory FinanceData.empty() => FinanceData(entries: [], budgets: {});

  final List<FinanceEntry> entries;
  final Map<String, double> budgets;

  FinanceData copyWith({
    List<FinanceEntry>? entries,
    Map<String, double>? budgets,
  }) =>
      FinanceData(
        entries: entries ?? this.entries,
        budgets: budgets ?? this.budgets,
      );
}
