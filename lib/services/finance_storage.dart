import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';

import '../models/finance_data.dart';

class FinanceStorage {
  late final Future<Database> _database = _openDatabase();

  Future<FinanceData> load() async {
    final database = await _database;
    final rows = await database.query('entries', orderBy: 'date DESC');
    final budgetRows = await database.query('budgets');
    final entries = rows.map(FinanceEntry.fromMap).toList();
    final budgets = {
      for (final row in budgetRows)
        row['category'] as String: (row['amount'] as num).toDouble(),
    };
    return FinanceData(entries: entries, budgets: budgets);
  }

  Future<void> save(FinanceData data) async {
    final database = await _database;
    await database.transaction((transaction) async {
      final batch = transaction.batch();
      batch.delete('entries');
      for (final entry in data.entries) {
        batch.insert(
          'entries',
          entry.toMap(),
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      batch.delete('budgets');
      for (final budget in data.budgets.entries) {
        batch.insert(
          'budgets',
          {'category': budget.key, 'amount': budget.value},
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      await batch.commit(noResult: true);
    });
  }

  Future<Database> _openDatabase() async {
    final databasesPath = await getDatabasesPath();
    return openDatabase(
      path.join(databasesPath, 'meu_bolso.db'),
      version: 1,
      onCreate: (database, _) async {
        await database.execute('''
          CREATE TABLE entries (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            amount REAL NOT NULL CHECK (amount > 0),
            type TEXT NOT NULL CHECK (type IN ('income', 'expense')),
            category TEXT NOT NULL,
            date TEXT NOT NULL,
            paid INTEGER NOT NULL CHECK (paid IN (0, 1))
          )
        ''');
        await database.execute(
          'CREATE INDEX entries_date_idx ON entries (date)',
        );
        await database.execute('''
          CREATE TABLE budgets (
            category TEXT PRIMARY KEY,
            amount REAL NOT NULL CHECK (amount > 0)
          )
        ''');
      },
    );
  }
}
