import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:intl/intl.dart';

import 'currency_input.dart';
import 'finance_summary.dart';
import 'models/finance_data.dart';
import 'services/finance_storage.dart';

const _ink = Color(0xFF172B2A);
const _green = Color(0xFF167D67);
const _mint = Color(0xFFE7F4EE);
const _canvas = Color(0xFFF6F8F5);
const _muted = Color(0xFF75827D);
const _categories = [
  'Moradia',
  'Alimentação',
  'Transporte',
  'Saúde',
  'Educação',
  'Lazer',
  'Assinaturas',
  'Outros',
];
const _incomeCategories = ['Salário', 'Investimentos', 'Outros'];

final _currency = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
final _shortDate = DateFormat('dd MMM', 'pt_BR');
final _monthFormat = DateFormat('MMMM yyyy', 'pt_BR');

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('pt_BR');
  runApp(const MeuBolsoApp());
}

class MeuBolsoApp extends StatelessWidget {
  const MeuBolsoApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Meu Bolso',
        debugShowCheckedModeBanner: false,
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        supportedLocales: const [Locale('pt', 'BR'), Locale('en', 'US')],
        theme: ThemeData(
          useMaterial3: true,
          scaffoldBackgroundColor: _canvas,
          colorScheme: ColorScheme.fromSeed(
            seedColor: _green,
            primary: _green,
            surface: Colors.white,
          ),
          fontFamily: 'Roboto',
          appBarTheme: const AppBarTheme(
            backgroundColor: _canvas,
            foregroundColor: _ink,
            elevation: 0,
            centerTitle: false,
          ),
          inputDecorationTheme: InputDecorationTheme(
            filled: true,
            fillColor: _canvas,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: const BorderSide(color: _green),
            ),
          ),
        ),
        home: const FinanceHome(),
      );
}

class FinanceHome extends StatefulWidget {
  const FinanceHome({super.key});

  @override
  State<FinanceHome> createState() => _FinanceHomeState();
}

class _FinanceHomeState extends State<FinanceHome> {
  final _storage = FinanceStorage();
  FinanceData? _data;
  Object? _loadError;
  bool _loading = true;
  int _selectedTab = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    try {
      final data = await _storage.load();
      if (!mounted) return;
      setState(() {
        _data = data;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loadError = error;
        _loading = false;
      });
    }
  }

  Future<void> _save(FinanceData data) async {
    try {
      await _storage.save(data);
      if (mounted) setState(() => _data = data);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Não foi possível salvar: $error'),
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  Future<void> _addEntry([FinanceEntry? existing]) async {
    final result = await showModalBottomSheet<FinanceEntry>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.white,
      builder: (_) => EntryForm(existing: existing),
    );
    if (result == null || _data == null) return;
    final entries = [..._data!.entries];
    final index = entries.indexWhere((entry) => entry.id == result.id);
    if (index == -1) {
      entries.add(result);
    } else {
      entries[index] = result;
    }
    await _save(_data!.copyWith(entries: entries));
  }

  Future<void> _deleteEntry(FinanceEntry entry) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir lançamento?'),
        content: Text('“${entry.title}” será removido do seu histórico.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red.shade700),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Excluir'),
          ),
        ],
      ),
    );
    if (confirmed != true || _data == null) return;
    await _save(
      _data!.copyWith(
        entries: _data!.entries.where((item) => item.id != entry.id).toList(),
      ),
    );
  }

  Future<void> _setBudget(String category) async {
    if (_data == null) return;
    final controller = TextEditingController(
      text: _data!.budgets[category]?.toStringAsFixed(2).replaceAll('.', ','),
    );
    final amount = await showDialog<double>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Limite: $category'),
        content: TextField(
          controller: controller,
          autofocus: true,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(
            labelText: 'Valor mensal',
            prefixText: 'R\$ ',
            hintText: 'Ex.: 500,00',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, 0.0),
            child: const Text('Remover limite'),
          ),
          FilledButton(
            onPressed: () {
              final value = parseCurrencyInput(controller.text);
              if (value == null || value <= 0) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Informe um valor maior que zero.')),
                );
                return;
              }
              Navigator.pop(context, value);
            },
            child: const Text('Salvar'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (amount == null || _data == null) return;
    final budgets = Map<String, double>.from(_data!.budgets);
    if (amount == 0) {
      budgets.remove(category);
    } else {
      budgets[category] = amount;
    }
    await _save(_data!.copyWith(budgets: budgets));
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    if (_loadError != null) {
      return Scaffold(
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: _green),
                const SizedBox(height: 16),
                const Text(
                  'Não foi possível carregar seus dados.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 8),
                Text('Detalhes: $_loadError', textAlign: TextAlign.center),
                const SizedBox(height: 20),
                FilledButton(onPressed: _load, child: const Text('Tentar novamente')),
              ],
            ),
          ),
        ),
      );
    }

    final data = _data ?? FinanceData.empty();
    final summary = summarizeFinance(data.entries);
    final pages = [
      _buildDashboard(data, summary),
      _buildTransactions(data.entries),
      _buildBills(summary.pendingBills),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: _mint,
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(Icons.savings_outlined, color: _green, size: 21),
            ),
            const SizedBox(width: 10),
            const Text(
              'meu bolso',
              style: TextStyle(fontSize: 21, fontWeight: FontWeight.w800),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Definir limites por categoria',
            onPressed: _openBudgetManager,
            icon: const Icon(Icons.tune_rounded),
          ),
        ],
      ),
      body: IndexedStack(index: _selectedTab, children: pages),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _addEntry(),
        backgroundColor: _green,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add_rounded),
        label: const Text('Novo lançamento'),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedTab,
        onDestinationSelected: (index) => setState(() => _selectedTab = index),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.space_dashboard_outlined),
            selectedIcon: Icon(Icons.space_dashboard_rounded),
            label: 'Início',
          ),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_outlined),
            selectedIcon: Icon(Icons.receipt_long_rounded),
            label: 'Lançamentos',
          ),
          NavigationDestination(
            icon: Icon(Icons.event_note_outlined),
            selectedIcon: Icon(Icons.event_note_rounded),
            label: 'Contas',
          ),
        ],
      ),
    );
  }

  Widget _buildDashboard(FinanceData data, FinanceSummary summary) {
    final now = DateTime.now();
    final categories = <String>{
      ...data.budgets.keys,
      ...summary.categoryExpenses.keys,
    }.toList()
      ..sort((a, b) => (summary.categoryExpenses[b] ?? 0)
          .compareTo(summary.categoryExpenses[a] ?? 0));

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
      children: [
        Text(
          _monthFormat.format(now).toUpperCase(),
          style: const TextStyle(
            color: _muted,
            fontSize: 11,
            fontWeight: FontWeight.w700,
            letterSpacing: 1.4,
          ),
        ),
        const SizedBox(height: 14),
        _BalanceCard(summary: summary),
        const SizedBox(height: 22),
        Row(
          children: [
            Expanded(
              child: _MetricCard(
                label: 'Entradas no mês',
                value: _currency.format(summary.monthIncome),
                icon: Icons.south_west_rounded,
                accent: _green,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _MetricCard(
                label: 'Despesas no mês',
                value: _currency.format(summary.monthExpenses),
                icon: Icons.north_east_rounded,
                accent: const Color(0xFFDC7259),
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),
        _SectionHeading(
          title: 'Visão dos últimos meses',
          actionLabel: 'Categorias',
          onAction: _openBudgetManager,
        ),
        const SizedBox(height: 12),
        _MonthlyChart(entries: data.entries),
        const SizedBox(height: 24),
        _SectionHeading(
          title: 'Limites por categoria',
          actionLabel: 'Editar',
          onAction: _openBudgetManager,
        ),
        const SizedBox(height: 12),
        if (categories.isEmpty)
          _EmptyCard(
            icon: Icons.pie_chart_outline_rounded,
            title: 'Planeje seus gastos',
            message: 'Defina limites mensais para acompanhar cada categoria.',
            action: 'Definir limites',
            onAction: _openBudgetManager,
          )
        else
          ...categories.take(4).map(
                (category) => _BudgetProgress(
                  category: category,
                  spent: summary.categoryExpenses[category] ?? 0,
                  budget: data.budgets[category] ?? 0,
                  onTap: () => _setBudget(category),
                ),
              ),
        const SizedBox(height: 24),
        _SectionHeading(
          title: 'Contas próximas',
          actionLabel: 'Ver todas',
          onAction: () => setState(() => _selectedTab = 2),
        ),
        const SizedBox(height: 12),
        if (summary.upcomingBills.isEmpty)
          _EmptyCard(
            icon: Icons.event_available_rounded,
            title: 'Tudo em dia',
            message: summary.pendingBills.isEmpty
                ? 'Cadastre suas contas para não perder nenhum vencimento.'
                : 'Nenhuma conta vence nos próximos sete dias.',
            action: 'Adicionar conta',
            onAction: () => _addEntry(),
          )
        else
          ...summary.upcomingBills.take(3).map(
                (entry) => _BillTile(
                  entry: entry,
                  onTap: () => _addEntry(entry),
                ),
              ),
        const SizedBox(height: 24),
        _SectionHeading(
          title: 'Últimos lançamentos',
          actionLabel: 'Ver todos',
          onAction: () => setState(() => _selectedTab = 1),
        ),
        const SizedBox(height: 12),
        if (data.entries.isEmpty)
          _EmptyCard(
            icon: Icons.wallet_outlined,
            title: 'Comece por aqui',
            message: 'Adicione sua primeira receita, despesa ou conta a pagar.',
            action: 'Criar lançamento',
            onAction: () => _addEntry(),
          )
        else
          ..._recentEntries(data.entries)
              .take(4)
              .map((entry) => _EntryTile(
                    entry: entry,
                    onTap: () => _addEntry(entry),
                    onDelete: () => _deleteEntry(entry),
                  )),
      ],
    );
  }

  Widget _buildTransactions(List<FinanceEntry> entries) {
    final sorted = [...entries]..sort((a, b) => b.date.compareTo(a.date));
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
      children: [
        const Text(
          'Seu histórico financeiro',
          style: TextStyle(fontSize: 25, fontWeight: FontWeight.w800, color: _ink),
        ),
        const SizedBox(height: 5),
        Text(
          '${entries.length} ${entries.length == 1 ? 'lançamento registrado' : 'lançamentos registrados'}',
          style: const TextStyle(color: _muted),
        ),
        const SizedBox(height: 20),
        if (sorted.isEmpty)
          _EmptyCard(
            icon: Icons.receipt_long_outlined,
            title: 'Nenhum lançamento ainda',
            message: 'Registre receitas e despesas para acompanhar suas finanças.',
            action: 'Adicionar lançamento',
            onAction: () => _addEntry(),
          )
        else
          ...sorted.map((entry) => _EntryTile(
                entry: entry,
                onTap: () => _addEntry(entry),
                onDelete: () => _deleteEntry(entry),
              )),
      ],
    );
  }

  Widget _buildBills(List<FinanceEntry> bills) {
    final overdue = bills.where((entry) => _isOverdue(entry.date)).toList();
    final upcoming = bills.where((entry) => !_isOverdue(entry.date)).toList();
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 110),
      children: [
        const Text(
          'Contas a pagar',
          style: TextStyle(fontSize: 25, fontWeight: FontWeight.w800, color: _ink),
        ),
        const SizedBox(height: 5),
        Text(
          '${bills.length} ${bills.length == 1 ? 'conta pendente' : 'contas pendentes'}',
          style: const TextStyle(color: _muted),
        ),
        const SizedBox(height: 22),
        if (bills.isEmpty)
          _EmptyCard(
            icon: Icons.event_available_rounded,
            title: 'Nenhuma conta pendente',
            message: 'Adicione uma despesa e marque-a como conta a pagar.',
            action: 'Adicionar conta',
            onAction: () => _addEntry(),
          )
        else ...[
          if (overdue.isNotEmpty) ...[
            const _SectionHeading(title: 'Vencidas'),
            const SizedBox(height: 10),
            ...overdue.map((entry) => _BillTile(
                  entry: entry,
                  onTap: () => _addEntry(entry),
                  onMarkPaid: () => _markPaid(entry),
                )),
            const SizedBox(height: 16),
          ],
          if (upcoming.isNotEmpty) ...[
            const _SectionHeading(title: 'Próximos vencimentos'),
            const SizedBox(height: 10),
            ...upcoming.map((entry) => _BillTile(
                  entry: entry,
                  onTap: () => _addEntry(entry),
                  onMarkPaid: () => _markPaid(entry),
                )),
          ],
        ],
      ],
    );
  }

  Future<void> _markPaid(FinanceEntry entry) async {
    if (_data == null) return;
    final entries = _data!.entries
        .map((item) => item.id == entry.id
            ? FinanceEntry(
                id: item.id,
                title: item.title,
                amount: item.amount,
                type: item.type,
                category: item.category,
                date: item.date,
                paid: true,
              )
            : item)
        .toList();
    await _save(_data!.copyWith(entries: entries));
  }

  Future<void> _openBudgetManager() async {
    if (_data == null) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.white,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 38,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.black12,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Limites mensais',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 6),
            const Text(
              'Defina quanto deseja gastar em cada categoria.',
              style: TextStyle(color: _muted),
            ),
            const SizedBox(height: 12),
            Flexible(
              child: ListView(
                shrinkWrap: true,
                children: _categories.map(
                  (category) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: _CategoryIcon(category: category),
                    title: Text(category),
                    subtitle: Text(_data!.budgets.containsKey(category)
                        ? 'Limite: ${_currency.format(_data!.budgets[category])}'
                        : 'Sem limite definido'),
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: () async {
                      Navigator.pop(context);
                      await _setBudget(category);
                    },
                  ),
                ).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  bool _isOverdue(DateTime date) {
    final today = DateTime.now();
    return DateTime(date.year, date.month, date.day)
        .isBefore(DateTime(today.year, today.month, today.day));
  }
}

List<FinanceEntry> _recentEntries(List<FinanceEntry> entries) =>
    [...entries]..sort((a, b) => b.date.compareTo(a.date));

class _BalanceCard extends StatelessWidget {
  const _BalanceCard({required this.summary});

  final FinanceSummary summary;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF195748), Color(0xFF123F38)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _green.withValues(alpha: 0.18),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Saldo disponível',
                  style: TextStyle(color: Colors.white70, fontSize: 13),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(30),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.shield_outlined, size: 14, color: Colors.white),
                    SizedBox(width: 5),
                    Text(
                      'No seu aparelho',
                      style: TextStyle(color: Colors.white, fontSize: 10),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            _currency.format(summary.balance),
            style: const TextStyle(
              color: Colors.white,
              fontSize: 32,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.8,
            ),
          ),
          const SizedBox(height: 17),
          Row(
            children: [
              Icon(Icons.calendar_month_rounded,
                  size: 15, color: Colors.white.withValues(alpha: 0.75)),
              const SizedBox(width: 6),
              Text(
                '${summary.pendingBills.length} ${summary.pendingBills.length == 1 ? 'conta pendente' : 'contas pendentes'}',
                style: const TextStyle(color: Colors.white70, fontSize: 12),
              ),
              const Spacer(),
              if (summary.upcomingBills.isNotEmpty)
                Text(
                  '${summary.upcomingBills.length} vencem em breve',
                  style: const TextStyle(
                    color: Color(0xFFD8EFA4),
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.accent,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color accent;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(15),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFFEDF0ED)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: accent, size: 19),
            const SizedBox(height: 11),
            Text(
              label,
              style: const TextStyle(color: _muted, fontSize: 11),
            ),
            const SizedBox(height: 4),
            FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Text(
                value,
                style: const TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                  color: _ink,
                ),
              ),
            ),
          ],
        ),
      );
}

class _SectionHeading extends StatelessWidget {
  const _SectionHeading({
    required this.title,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) => Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: _ink,
              ),
            ),
          ),
          if (actionLabel != null)
            TextButton(
              onPressed: onAction,
              style: TextButton.styleFrom(
                visualDensity: VisualDensity.compact,
                padding: const EdgeInsets.symmetric(horizontal: 6),
              ),
              child: Text(
                actionLabel!,
                style: const TextStyle(color: _green, fontSize: 12),
              ),
            ),
        ],
      );
}

class _MonthlyChart extends StatelessWidget {
  const _MonthlyChart({required this.entries});

  final List<FinanceEntry> entries;

  @override
  Widget build(BuildContext context) {
    final currentMonth = DateTime.now();
    final months = List.generate(
      6,
      (index) => DateTime(currentMonth.year, currentMonth.month - 5 + index),
    );
    final amounts = months.map((month) => monthlyExpensesFor(entries, month)).toList();
    final maxAmount = amounts.fold<double>(
      0,
      (maximum, amount) => amount > maximum ? amount : maximum,
    );
    final chartMax = maxAmount == 0 ? 1 : maxAmount;
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFEDF0ED)),
      ),
      child: Column(
        children: [
          SizedBox(
            height: 124,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: List.generate(months.length, (index) {
                final height = maxAmount == 0
                    ? 5.0
                    : (amounts[index] / chartMax * 88)
                        .clamp(5.0, 88.0)
                        .toDouble();
                final isCurrent = index == months.length - 1;
                return Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      if (isCurrent && amounts[index] > 0)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 5),
                          child: FittedBox(
                            child: Text(
                              _currency.format(amounts[index]),
                              style: const TextStyle(
                                color: _green,
                                fontSize: 9,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        ),
                      Container(
                        height: height,
                        width: 22,
                        decoration: BoxDecoration(
                          color: isCurrent ? _green : const Color(0xFFDDEDE4),
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                      const SizedBox(height: 7),
                      Text(
                        DateFormat('MMM', 'pt_BR').format(months[index]),
                        style: TextStyle(
                          color: isCurrent ? _green : _muted,
                          fontSize: 10,
                          fontWeight: isCurrent ? FontWeight.w700 : FontWeight.w400,
                        ),
                      ),
                    ],
                  ),
                );
              }),
            ),
          ),
          const SizedBox(height: 12),
          const Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.circle, color: _green, size: 8),
              SizedBox(width: 6),
              Text(
                'Despesas por mês',
                style: TextStyle(color: _muted, fontSize: 11),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _BudgetProgress extends StatelessWidget {
  const _BudgetProgress({
    required this.category,
    required this.spent,
    required this.budget,
    required this.onTap,
  });

  final String category;
  final double spent;
  final double budget;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ratio =
        budget > 0 ? (spent / budget).clamp(0.0, 1.0).toDouble() : 0.0;
    final exceeded = budget > 0 && spent > budget;
    final color = exceeded ? const Color(0xFFDC7259) : _green;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(
          children: [
            _CategoryIcon(category: category),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          category,
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      Text(
                        budget > 0
                            ? '${_currency.format(spent)} / ${_currency.format(budget)}'
                            : '${_currency.format(spent)} · Sem limite',
                        style: TextStyle(
                          color: exceeded ? color : _muted,
                          fontSize: 11,
                          fontWeight: exceeded ? FontWeight.w700 : FontWeight.w400,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 7),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: LinearProgressIndicator(
                      value: budget > 0 ? ratio : 0,
                      minHeight: 6,
                      backgroundColor: const Color(0xFFEDF0ED),
                      color: color,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({
    required this.icon,
    required this.title,
    required this.message,
    this.action,
    this.onAction,
  });

  final IconData icon;
  final String title;
  final String message;
  final String? action;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 22),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFFEDF0ED)),
        ),
        child: Column(
          children: [
            Icon(icon, color: _green, size: 26),
            const SizedBox(height: 10),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 5),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted, fontSize: 12, height: 1.4),
            ),
            if (action != null) ...[
              const SizedBox(height: 11),
              TextButton.icon(
                onPressed: onAction,
                icon: const Icon(Icons.add_rounded, size: 18),
                label: Text(action!),
                style: TextButton.styleFrom(foregroundColor: _green),
              ),
            ],
          ],
        ),
      );
}

class _CategoryIcon extends StatelessWidget {
  const _CategoryIcon({required this.category});

  final String category;

  @override
  Widget build(BuildContext context) {
    final icon = switch (category) {
      'Moradia' => Icons.home_outlined,
      'Alimentação' => Icons.restaurant_outlined,
      'Transporte' => Icons.directions_car_outlined,
      'Saúde' => Icons.favorite_border_rounded,
      'Educação' => Icons.school_outlined,
      'Lazer' => Icons.sports_esports_outlined,
      'Assinaturas' => Icons.subscriptions_outlined,
      'Salário' => Icons.work_outline_rounded,
      'Investimentos' => Icons.trending_up_rounded,
      _ => Icons.category_outlined,
    };
    return Container(
      width: 38,
      height: 38,
      decoration: BoxDecoration(
        color: _mint,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Icon(icon, size: 19, color: _green),
    );
  }
}

class _EntryTile extends StatelessWidget {
  const _EntryTile({
    required this.entry,
    required this.onTap,
    required this.onDelete,
  });

  final FinanceEntry entry;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final income = entry.type == EntryType.income;
    return Padding(
      padding: const EdgeInsets.only(bottom: 9),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(17),
        child: Row(
          children: [
            Expanded(
              child: InkWell(
                onTap: onTap,
                borderRadius: BorderRadius.circular(17),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                  child: Row(
                    children: [
                      _CategoryIcon(category: entry.category),
                      const SizedBox(width: 11),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              entry.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                color: _ink,
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '${entry.category} · ${_shortDate.format(entry.date)}${entry.paid ? '' : ' · Pendente'}',
                              style: const TextStyle(color: _muted, fontSize: 10),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 5),
                      Text(
                        '${income ? '+' : '−'} ${_currency.format(entry.amount)}',
                        style: TextStyle(
                          color: income ? _green : _ink,
                          fontSize: 12,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            PopupMenuButton<String>(
              tooltip: 'Opções do lançamento',
              onSelected: (value) {
                if (value == 'edit') onTap();
                if (value == 'delete') onDelete();
              },
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'edit', child: Text('Editar')),
                PopupMenuItem(value: 'delete', child: Text('Excluir')),
              ],
              icon: const Icon(Icons.more_vert_rounded, color: _muted, size: 19),
            ),
          ],
        ),
      ),
    );
  }
}

class _BillTile extends StatelessWidget {
  const _BillTile({
    required this.entry,
    required this.onTap,
    this.onMarkPaid,
  });

  final FinanceEntry entry;
  final VoidCallback onTap;
  final VoidCallback? onMarkPaid;

  bool get _overdue {
    final today = DateTime.now();
    return DateTime(entry.date.year, entry.date.month, entry.date.day)
        .isBefore(DateTime(today.year, today.month, today.day));
  }

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 9),
        child: Material(
          color: Colors.white,
          borderRadius: BorderRadius.circular(17),
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(17),
            child: Padding(
              padding: const EdgeInsets.all(13),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 48,
                    decoration: BoxDecoration(
                      color: _overdue
                          ? const Color(0xFFFFF0ED)
                          : const Color(0xFFFFF7E8),
                      borderRadius: BorderRadius.circular(13),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          DateFormat('dd').format(entry.date),
                          style: TextStyle(
                            color: _overdue
                                ? const Color(0xFFCB5A43)
                                : const Color(0xFFA16B1C),
                            fontWeight: FontWeight.w800,
                            fontSize: 16,
                          ),
                        ),
                        Text(
                          DateFormat('MMM', 'pt_BR')
                              .format(entry.date)
                              .toUpperCase(),
                          style: const TextStyle(fontSize: 8, color: _muted),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          entry.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: _ink,
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          _overdue ? 'Vencida' : entry.category,
                          style: TextStyle(
                            color: _overdue
                                ? const Color(0xFFCB5A43)
                                : _muted,
                            fontSize: 11,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        _currency.format(entry.amount),
                        style: const TextStyle(
                          color: _ink,
                          fontWeight: FontWeight.w800,
                          fontSize: 12,
                        ),
                      ),
                      if (onMarkPaid != null)
                        GestureDetector(
                          onTap: onMarkPaid,
                          child: const Padding(
                            padding: EdgeInsets.only(top: 5),
                            child: Text(
                              'Marcar paga',
                              style: TextStyle(
                                color: _green,
                                fontSize: 10,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      );
}

class EntryForm extends StatefulWidget {
  const EntryForm({super.key, this.existing});

  final FinanceEntry? existing;

  @override
  State<EntryForm> createState() => _EntryFormState();
}

class _EntryFormState extends State<EntryForm> {
  late final TextEditingController _titleController;
  late final TextEditingController _amountController;
  late EntryType _type;
  late String _category;
  late DateTime _date;
  late bool _isBill;
  late bool _paid;

  bool get _editing => widget.existing != null;

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    _titleController = TextEditingController(text: existing?.title ?? '');
    _amountController = TextEditingController(
      text: existing?.amount.toStringAsFixed(2).replaceAll('.', ',') ?? '',
    );
    _type = existing?.type ?? EntryType.expense;
    _category = existing?.category ?? 'Alimentação';
    _date = existing?.date ?? DateTime.now();
    _isBill = existing != null && !existing.paid;
    _paid = existing?.paid ?? true;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _amountController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final categories =
        _type == EntryType.income ? _incomeCategories : _categories;
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        12,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 20,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 38,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.black12,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              _editing ? 'Editar lançamento' : 'Novo lançamento',
              style: const TextStyle(
                fontSize: 22,
                color: _ink,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 18),
            SegmentedButton<EntryType>(
              segments: const [
                ButtonSegment(
                  value: EntryType.expense,
                  icon: Icon(Icons.north_east_rounded),
                  label: Text('Despesa'),
                ),
                ButtonSegment(
                  value: EntryType.income,
                  icon: Icon(Icons.south_west_rounded),
                  label: Text('Receita'),
                ),
              ],
              selected: {_type},
              onSelectionChanged: (selection) => setState(() {
                _type = selection.first;
                final availableCategories =
                    _type == EntryType.income ? _incomeCategories : _categories;
                if (!availableCategories.contains(_category)) {
                  _category = 'Outros';
                }
                if (_type == EntryType.income) {
                  _isBill = false;
                  _paid = true;
                }
              }),
            ),
            const SizedBox(height: 15),
            TextField(
              controller: _titleController,
              textCapitalization: TextCapitalization.sentences,
              decoration: const InputDecoration(
                labelText: 'Descrição',
                hintText: 'Ex.: Conta de luz, salário...',
                prefixIcon: Icon(Icons.edit_note_rounded),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _amountController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Valor',
                prefixText: 'R\$ ',
                prefixIcon: Icon(Icons.attach_money_rounded),
                hintText: '0,00',
              ),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              key: ValueKey(_type),
              initialValue: _category,
              decoration: const InputDecoration(
                labelText: 'Categoria',
                prefixIcon: Icon(Icons.category_outlined),
              ),
              items: categories
                  .map((category) => DropdownMenuItem(
                        value: category,
                        child: Text(category),
                      ))
                  .toList(),
              onChanged: (value) {
                if (value != null) setState(() => _category = value);
              },
            ),
            const SizedBox(height: 10),
            ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 4),
              leading: const Icon(Icons.calendar_today_outlined, color: _green),
              title: const Text('Data'),
              subtitle: Text(DateFormat('dd/MM/yyyy', 'pt_BR').format(_date)),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: _pickDate,
            ),
            if (_type == EntryType.expense)
              SwitchListTile(
                contentPadding: const EdgeInsets.symmetric(horizontal: 4),
                title: const Text('Conta a pagar'),
                subtitle: const Text('Acompanhar esta despesa como pendente'),
                value: _isBill,
                activeThumbColor: _green,
                onChanged: (value) => setState(() {
                  _isBill = value;
                  _paid = !value;
                }),
              ),
            if (_editing && _isBill)
              SwitchListTile(
                contentPadding: const EdgeInsets.symmetric(horizontal: 4),
                title: const Text('Conta paga'),
                value: _paid,
                activeThumbColor: _green,
                onChanged: (value) => setState(() => _paid = value),
              ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 15),
                  backgroundColor: _green,
                ),
                onPressed: _save,
                child: Text(_editing ? 'Salvar alterações' : 'Salvar lançamento'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _pickDate() async {
    final selected = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
      locale: const Locale('pt', 'BR'),
      helpText: 'Escolha a data do lançamento ou vencimento',
      cancelText: 'Cancelar',
      confirmText: 'Confirmar',
    );
    if (selected != null) setState(() => _date = selected);
  }

  void _save() {
    final title = _titleController.text.trim();
    final amount = parseCurrencyInput(_amountController.text);
    if (title.isEmpty) {
      _showError('Informe uma descrição para o lançamento.');
      return;
    }
    if (amount == null || !amount.isFinite || amount <= 0) {
      _showError('Informe um valor válido maior que zero.');
      return;
    }
    Navigator.pop(
      context,
      FinanceEntry(
        id: widget.existing?.id ??
            '${DateTime.now().microsecondsSinceEpoch}',
        title: title,
        amount: amount,
        type: _type,
        category: _category,
        date: _date,
        paid: _type == EntryType.income || !_isBill || _paid,
      ),
    );
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
  }
}
