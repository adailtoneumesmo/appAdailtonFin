# Meu Bolso

Aplicativo Flutter para organizar receitas, despesas, vencimentos e limites de
gastos por categoria. Os dados ficam armazenados localmente em um banco SQLite
no aparelho.

## Funcionalidades

- Cadastro e edição de receitas e despesas em reais.
- Contas a pagar com data de vencimento e opção para marcar como pagas.
- Resumo de saldo, entradas, despesas e contas próximas do vencimento.
- Gráfico de despesas dos últimos seis meses.
- Categorias e limites mensais configuráveis por categoria.
- Histórico de lançamentos com opção de exclusão.

## Executar no Android

Instale o Flutter SDK e configure o Android SDK. Neste repositório, gere os
arquivos nativos da plataforma Android e instale as dependências:

```powershell
flutter create . --platforms=android
flutter pub get
flutter run
```

Para executar os testes:

```powershell
flutter test
```
