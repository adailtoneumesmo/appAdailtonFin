package br.com.meubolso.domain.dashboard

import br.com.meubolso.domain.calendario.CalendarioDiasUteis
import br.com.meubolso.domain.cartao.CalculadoraFatura
import br.com.meubolso.domain.despesa.CalculadoraOcorrencias
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaCalculoVale
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.Origem
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.domain.renda.CalculadoraRecebimento
import br.com.meubolso.domain.vale.CalculadoraValeAlimentacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculadoraDashboardTest {

    private val calculadora = CalculadoraDashboard(
        CalculadoraRecebimento(CalendarioDiasUteis()),
        CalculadoraOcorrencias(),
        CalculadoraFatura(),
        CalculadoraValeAlimentacao(CalendarioDiasUteis(), CalculadoraOcorrencias()),
    )

    private val outubro = YearMonth.of(2026, 10)

    private fun renda(
        id: Long,
        tipo: TipoRenda,
        valor: Long,
        dia: Int,
        ativo: Boolean = true,
        inicio: LocalDate = LocalDate.of(2026, 1, 1),
    ) = Renda(
        id = id,
        descricao = "Renda $id",
        tipo = tipo,
        valorCentavos = valor,
        modoDia = ModoDiaRecebimento.DIA_FIXO,
        dia = dia,
        ativo = ativo,
        dataInicio = inicio,
    )

    private fun despesa(
        id: Long,
        valor: Long,
        dia: Int = 10,
        vinculo: VinculoPeriodo = VinculoPeriodo.SALARIO,
        inicio: YearMonth = YearMonth.of(2026, 1),
    ) = DespesaFixa(
        id = id,
        descricao = "Despesa $id",
        categoriaId = 1,
        valorCentavos = valor,
        diaVencimento = dia,
        formaPagamento = FormaPagamento.PIX,
        vinculo = vinculo,
        mesInicio = inicio,
    )

    private fun resumir(
        rendas: List<Renda> = emptyList(),
        despesas: List<DespesaFixa> = emptyList(),
        pagamentos: List<PagamentoDespesa> = emptyList(),
        mes: YearMonth = outubro,
    ) = calculadora.resumir(rendas, despesas, pagamentos, mes, anteciparDiaNaoUtil = false)

    @Test
    fun `saldo positivo quando renda supera despesa`() {
        val resumo = resumir(
            rendas = listOf(renda(1, TipoRenda.SALARIO, 500_000, dia = 5)),
            despesas = listOf(despesa(1, 200_000)),
        )
        assertEquals(300_000, resumo.salario.saldoCentavos)
        assertEquals(SituacaoSaldo.POSITIVO, resumo.salario.situacao)
        assertEquals(SituacaoSaldo.POSITIVO, resumo.geral.situacao)
    }

    @Test
    fun `saldo negativo quando despesa supera renda`() {
        val resumo = resumir(
            rendas = listOf(renda(1, TipoRenda.VALE, 80_000, dia = 20)),
            despesas = listOf(despesa(1, 100_000, vinculo = VinculoPeriodo.VALE)),
        )
        assertEquals(-20_000, resumo.vale.saldoCentavos)
        assertEquals(SituacaoSaldo.NEGATIVO, resumo.vale.situacao)
        assertEquals(SituacaoSaldo.NEGATIVO, resumo.geral.situacao)
    }

    @Test
    fun `saldo zerado quando renda e despesa se igualam`() {
        val resumo = resumir(
            rendas = listOf(renda(1, TipoRenda.SALARIO, 150_000, dia = 5)),
            despesas = listOf(despesa(1, 100_000), despesa(2, 50_000)),
        )
        assertEquals(0, resumo.salario.saldoCentavos)
        assertEquals(SituacaoSaldo.ZERADO, resumo.salario.situacao)
    }

    @Test
    fun `mes sem lancamentos fica zerado e sinalizado`() {
        val resumo = resumir(
            rendas = listOf(renda(1, TipoRenda.SALARIO, 500_000, dia = 5, inicio = LocalDate.of(2026, 11, 1))),
            despesas = listOf(despesa(1, 100_000, inicio = YearMonth.of(2026, 11))),
        )
        assertFalse(resumo.possuiLancamentos)
        assertEquals(Balanco(), resumo.geral)
        assertEquals(SituacaoSaldo.ZERADO, resumo.geral.situacao)
        assertEquals(FaixaConsumo.NORMAL, resumo.geral.faixaConsumo)
    }

    @Test
    fun `separa salario e vale e soma os dois no resumo geral`() {
        val resumo = resumir(
            rendas = listOf(
                renda(1, TipoRenda.SALARIO, 500_000, dia = 5),
                renda(2, TipoRenda.VALE, 100_000, dia = 20),
            ),
            despesas = listOf(
                despesa(1, 300_000, vinculo = VinculoPeriodo.SALARIO),
                despesa(2, 40_000, vinculo = VinculoPeriodo.VALE),
                despesa(3, 70_000, vinculo = VinculoPeriodo.VALE),
            ),
        )
        assertTrue(resumo.possuiLancamentos)
        assertEquals(Balanco(500_000, 300_000), resumo.salario)
        assertEquals(Balanco(100_000, 110_000), resumo.vale)
        assertEquals(Balanco(600_000, 410_000), resumo.geral)
        assertEquals(190_000, resumo.geral.saldoCentavos)
    }

    @Test
    fun `vinculo automatico usa o periodo do ultimo recebimento antes do vencimento`() {
        val rendas = listOf(
            renda(1, TipoRenda.SALARIO, 500_000, dia = 5),
            renda(2, TipoRenda.VALE, 100_000, dia = 20),
        )
        val resumo = resumir(
            rendas = rendas,
            despesas = listOf(
                // 10/10: período do salário (5/10)
                despesa(1, 1_000, dia = 10, vinculo = VinculoPeriodo.AUTOMATICO),
                // 25/10: período do vale (20/10)
                despesa(2, 2_000, dia = 25, vinculo = VinculoPeriodo.AUTOMATICO),
                // 02/10: ainda no período do vale de 20/09
                despesa(3, 4_000, dia = 2, vinculo = VinculoPeriodo.AUTOMATICO),
                // 20/10: vence no dia em que o vale cai
                despesa(4, 8_000, dia = 20, vinculo = VinculoPeriodo.AUTOMATICO),
            ),
        )
        assertEquals(1_000, resumo.salario.despesaCentavos)
        assertEquals(14_000, resumo.vale.despesaCentavos)
    }

    @Test
    fun `vinculo automatico sem rendas cadastradas vai para o salario`() {
        val resumo = resumir(despesas = listOf(despesa(1, 5_000, vinculo = VinculoPeriodo.AUTOMATICO)))
        assertEquals(Balanco(0, 5_000), resumo.salario)
        assertEquals(Balanco(), resumo.vale)
    }

    @Test
    fun `renda extra entra no salario e renda inativa e ignorada`() {
        val resumo = resumir(
            rendas = listOf(
                renda(1, TipoRenda.SALARIO, 500_000, dia = 5),
                renda(2, TipoRenda.RENDA_EXTRA, 30_000, dia = 15),
                renda(3, TipoRenda.VALE, 100_000, dia = 20, ativo = false),
            ),
        )
        assertEquals(530_000, resumo.salario.rendaCentavos)
        assertEquals(30_000, resumo.rendaExtraCentavos)
        assertEquals(0, resumo.vale.rendaCentavos)
    }

    @Test
    fun `faixa de consumo verde ate 70, amarela ate 100 e vermelha acima`() {
        assertEquals(FaixaConsumo.NORMAL, Balanco(100_00, 70_00).faixaConsumo)
        assertEquals(FaixaConsumo.ALERTA, Balanco(100_00, 70_01).faixaConsumo)
        assertEquals(FaixaConsumo.ALERTA, Balanco(100_00, 100_00).faixaConsumo)
        assertEquals(FaixaConsumo.EXCEDIDO, Balanco(100_00, 100_01).faixaConsumo)
        assertEquals(FaixaConsumo.EXCEDIDO, Balanco(0, 1).faixaConsumo)
        assertEquals(0.5, Balanco(100_00, 50_00).fracaoConsumida, 0.0)
    }

    // Faturas de cartão

    private val calculadoraFatura = CalculadoraFatura()

    private fun cartao(id: Long, fonte: Origem, fechamento: Int = 10, vencimento: Int = 17) =
        Cartao(id = id, nome = "Cartão $id", diaFechamento = fechamento, diaVencimento = vencimento, fonte = fonte)

    private fun compra(cartao: Cartao, valor: Long, data: LocalDate, parcelas: Int? = null, id: Long = 1) = CompraCartao(
        id = id,
        cartaoId = cartao.id,
        descricao = "Compra $id",
        categoriaId = 1,
        valorCentavos = valor,
        dataCompra = data,
        modalidade = if (parcelas == null) ModalidadeCompra.A_VISTA else ModalidadeCompra.PARCELADO,
        totalParcelas = parcelas,
        mesPrimeiraFatura = calculadoraFatura.faturaDaCompra(cartao, data),
    )

    @Test
    fun `fatura entra como despesa da fonte do cartao no mes do vencimento`() {
        val aline = cartao(1, Origem.VALE)
        val resumo = calculadora.resumir(
            rendas = listOf(renda(1, TipoRenda.VALE, 100_000, dia = 15)),
            despesas = emptyList(),
            pagamentos = emptyList(),
            mes = outubro,
            anteciparDiaNaoUtil = false,
            cartoes = listOf(aline),
            compras = listOf(compra(aline, 80_000, LocalDate.of(2026, 10, 8), parcelas = 8)),
        )
        assertEquals(10_000L, resumo.vale.despesaCentavos)
        assertEquals(0L, resumo.salario.despesaCentavos)
        assertEquals(1, resumo.detalheVale.faturas.size)
        assertEquals(LocalDate.of(2026, 10, 17), resumo.detalheVale.faturas.single().vencimento)
    }

    @Test
    fun `fatura que vence no mes seguinte conta no mes do vencimento`() {
        // Fecha dia 28 e vence dia 5: a fatura de outubro vence em 05/11.
        val c = cartao(1, Origem.SALARIO, fechamento = 28, vencimento = 5)
        val compras = listOf(compra(c, 30_000, LocalDate.of(2026, 10, 8)))
        fun resumoDe(mes: YearMonth) = calculadora.resumir(
            emptyList(), emptyList(), emptyList(), mes, false, cartoes = listOf(c), compras = compras,
        )
        assertEquals(0L, resumoDe(outubro).salario.despesaCentavos)
        assertFalse(resumoDe(outubro).possuiLancamentos)
        assertEquals(30_000L, resumoDe(YearMonth.of(2026, 11)).salario.despesaCentavos)
        assertTrue(resumoDe(YearMonth.of(2026, 11)).possuiLancamentos)
    }

    @Test
    fun `detalhe lista rendas e despesas da origem`() {
        val resumo = resumir(
            rendas = listOf(renda(1, TipoRenda.SALARIO, 500_000, dia = 5), renda(2, TipoRenda.VALE, 100_000, dia = 20)),
            despesas = listOf(despesa(1, 150_000, dia = 10, vinculo = VinculoPeriodo.SALARIO)),
        )
        assertEquals(listOf(1L), resumo.detalheSalario.rendas.map { it.renda.id })
        assertEquals(LocalDate.of(2026, 10, 5), resumo.detalheSalario.rendas.single().data)
        assertEquals(listOf(1L), resumo.detalheSalario.despesas.map { it.despesa.id })
        assertEquals(listOf(2L), resumo.detalheVale.rendas.map { it.renda.id })
        assertTrue(resumo.detalheVale.despesas.isEmpty())
    }

    @Test
    fun `vale alimentacao fica fora do saldo geral e tem carteira propria`() {
        val vr = renda(2, TipoRenda.VALE_ALIMENTACAO, 2_000, dia = 25)
            .copy(formaCalculoVale = FormaCalculoVale.VALOR_POR_DIA)
        val resumo = resumir(
            rendas = listOf(renda(1, TipoRenda.SALARIO, 500_000, dia = 5), vr),
            despesas = listOf(
                despesa(1, 100_000),
                despesa(2, 15_000, vinculo = VinculoPeriodo.AUTOMATICO).copy(formaPagamento = FormaPagamento.VALE_ALIMENTACAO),
            ),
        )
        assertEquals(500_000L, resumo.geral.rendaCentavos)
        assertEquals(100_000L, resumo.geral.despesaCentavos)
        // Outubro/2026: R$ 20,00 × 21 dias úteis de outubro
        assertEquals(42_000L, resumo.valeAlimentacao.creditoCentavos)
        assertEquals(15_000L, resumo.valeAlimentacao.gastoCentavos)
        assertEquals(27_000L, resumo.valeAlimentacao.saldoCentavos)
    }

    @Test
    fun `mes so com vale alimentacao possui lancamentos`() {
        val vr = renda(1, TipoRenda.VALE_ALIMENTACAO, 60_000, dia = 1).copy(formaCalculoVale = FormaCalculoVale.VALOR_FIXO)
        val resumo = resumir(rendas = listOf(vr))
        assertTrue(resumo.possuiLancamentos)
        assertEquals(0L, resumo.geral.rendaCentavos)
    }

    @Test
    fun `consumo por categoria soma despesas e abre a fatura nas categorias das compras`() {
        val moradia = Categoria(id = 1, nome = "Moradia", cor = 0)
        val lazer = Categoria(id = 2, nome = "Lazer", cor = 0)
        val transporte = Categoria(id = 3, nome = "Transporte", cor = 0)
        val c = cartao(1, Origem.SALARIO)
        val resumo = calculadora.resumir(
            rendas = listOf(renda(1, TipoRenda.SALARIO, 500_000, dia = 5), renda(2, TipoRenda.VALE, 100_000, dia = 20)),
            despesas = listOf(
                despesa(1, 150_000, vinculo = VinculoPeriodo.SALARIO),
                despesa(2, 20_000, vinculo = VinculoPeriodo.VALE).copy(categoriaId = 2),
                // VR/VA fica fora do consumo, assim como fica fora do saldo geral.
                despesa(3, 9_000).copy(categoriaId = 2, formaPagamento = FormaPagamento.VALE_ALIMENTACAO),
            ),
            pagamentos = emptyList(),
            mes = outubro,
            anteciparDiaNaoUtil = false,
            cartoes = listOf(c),
            compras = listOf(
                compra(c, 30_000, LocalDate.of(2026, 10, 2), id = 1).copy(categoriaId = 2),
                compra(c, 5_000, LocalDate.of(2026, 10, 3), id = 2).copy(categoriaId = 3),
                // Estorno maior que a compra: a categoria some do gráfico.
                compra(c, -8_000, LocalDate.of(2026, 10, 4), id = 3).copy(categoriaId = 3),
            ),
        )
        val consumo = resumo.consumoPorCategoria(listOf(moradia, lazer, transporte))
        assertEquals(listOf(moradia to 150_000L, lazer to 50_000L), consumo.map { it.categoria to it.centavos })
    }
}
