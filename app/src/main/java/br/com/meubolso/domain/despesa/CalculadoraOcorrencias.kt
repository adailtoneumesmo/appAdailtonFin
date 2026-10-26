package br.com.meubolso.domain.despesa

import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.OcorrenciaDespesa
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Recorrencia
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CalculadoraOcorrencias @Inject constructor() {

    /**
     * Ocorrência da despesa no [mes], ou `null` se o mês for anterior ao início,
     * posterior à última parcela ou, na despesa única, diferente do mês de início. Não considera o flag [DespesaFixa.ativa].
     */
    fun ocorrenciaNoMes(
        despesa: DespesaFixa,
        mes: YearMonth,
        pagamento: PagamentoDespesa? = null,
    ): OcorrenciaDespesa? {
        val indice = ChronoUnit.MONTHS.between(despesa.mesInicio, mes)
        if (indice < 0) return null
        val parcela = when (despesa.recorrencia) {
            Recorrencia.MENSAL -> null
            Recorrencia.UNICA -> {
                if (indice > 0) return null
                null
            }
            Recorrencia.PARCELADA -> {
                val total = requireNotNull(despesa.totalParcelas) { "Despesa parcelada sem total de parcelas" }
                if (indice >= total) return null
                indice.toInt() + 1
            }
        }
        return OcorrenciaDespesa(
            despesa = despesa,
            competencia = mes,
            vencimento = mes.atDay(minOf(despesa.diaVencimento, mes.lengthOfMonth())),
            parcela = parcela,
            dataPagamento = pagamento?.dataPagamento,
        )
    }

    /**
     * Ocorrências do [mes] ordenadas por vencimento. Despesas inativas só aparecem
     * se já tiverem pagamento registrado no mês, para preservar o histórico.
     */
    fun ocorrenciasNoMes(
        despesas: List<DespesaFixa>,
        pagamentos: List<PagamentoDespesa>,
        mes: YearMonth,
    ): List<OcorrenciaDespesa> {
        val pagamentosDoMes = pagamentos
            .filter { it.competencia == mes }
            .associateBy { it.despesaId }
        return despesas
            .mapNotNull { despesa ->
                val pagamento = pagamentosDoMes[despesa.id]
                if (!despesa.ativa && pagamento == null) return@mapNotNull null
                ocorrenciaNoMes(despesa, mes, pagamento)
            }
            .sortedWith(compareBy({ it.vencimento }, { it.despesa.descricao.lowercase() }))
    }
}
