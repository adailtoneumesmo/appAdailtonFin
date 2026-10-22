package br.com.meubolso.domain.model

/**
 * De onde sai o dinheiro. Não é gravada no banco: deriva de [Renda.tipo] e de
 * [DespesaFixa.vinculo], que já guardam essa informação.
 */
enum class Origem { SALARIO, VALE }

/** Renda extra não tem período próprio; entra no mesmo bolso do salário. */
val TipoRenda.origem: Origem
    get() = when (this) {
        TipoRenda.SALARIO, TipoRenda.RENDA_EXTRA -> Origem.SALARIO
        TipoRenda.VALE -> Origem.VALE
    }
