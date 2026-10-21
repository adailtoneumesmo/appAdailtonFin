double? parseCurrencyInput(String input) {
  final value = input.trim().replaceAll(RegExp(r'\s+'), '');
  if (value.contains(',')) {
    return double.tryParse(
      value.replaceAll('.', '').replaceAll(',', '.'),
    );
  }
  if (RegExp(r'^\d{1,3}(\.\d{3})+$').hasMatch(value)) {
    return double.tryParse(value.replaceAll('.', ''));
  }
  return double.tryParse(value);
}
