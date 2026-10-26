package br.com.meubolso.ui.despesas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.despesa.CampoDespesa
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.ui.components.AcaoMenu
import br.com.meubolso.ui.components.CampoData
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.ui.components.CampoSelecao
import br.com.meubolso.ui.components.PontoCor
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.ui.components.mensagem
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespesaFormScreen(
    onVoltar: () -> Unit,
    onGerenciarCategorias: () -> Unit,
    onNovoCartao: () -> Unit,
    viewModel: DespesaFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val cartoes by viewModel.cartoes.collectAsStateWithLifecycle()
    val previa by viewModel.previaFatura.collectAsStateWithLifecycle()
    val avisoVale by viewModel.avisoVale.collectAsStateWithLifecycle()
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.concluido) {
        if (estado.concluido) onVoltar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when {
                                estado.editando -> R.string.despesa_form_titulo_editar
                                estado.pendenteId != null -> R.string.despesa_form_titulo_confirmar
                                else -> R.string.despesa_form_titulo_nova
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
                actions = {
                    if (estado.editando) {
                        IconButton(onClick = { confirmarExclusao = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.acao_excluir))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            estado.textoNotificacao?.let { texto ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.despesa_notificacao_original), style = MaterialTheme.typography.labelMedium)
                        Text(texto, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // Na edição o tipo não muda: despesa fixa e compra no cartão ficam em tabelas diferentes.
            if (!estado.editando) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.despesa_incluir_fatura),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = estado.emFatura, onCheckedChange = viewModel::alterarEmFatura)
                }
            }

            if (estado.emFatura) {
                CamposCompraCartao(estado, categorias, cartoes, previa, viewModel, onGerenciarCategorias, onNovoCartao)
            } else {
                CamposDespesaFixa(estado, categorias, avisoVale, viewModel, onGerenciarCategorias)
            }

            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acao_salvar))
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text(stringResource(R.string.despesa_excluir_titulo)) },
            text = {
                Text(
                    stringResource(
                        when {
                            !estado.emFatura -> R.string.despesa_excluir_mensagem
                            estado.modalidade == ModalidadeCompra.FIXO -> R.string.despesa_compra_excluir_mensagem
                            else -> R.string.despesa_compra_excluir_mensagem_simples
                        },
                        estado.descricao,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    viewModel.excluir()
                }) { Text(stringResource(R.string.acao_excluir)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) {
                    Text(stringResource(R.string.acao_cancelar))
                }
            },
        )
    }
}

@Composable
private fun DespesaFormUiState.erroDe(campo: CampoDespesa): String? = erros[campo]?.let { stringResource(it.mensagem) }

@Composable
private fun CampoDescricao(estado: DespesaFormUiState, onValorChange: (String) -> Unit) {
    OutlinedTextField(
        value = estado.descricao,
        onValueChange = onValorChange,
        label = { Text(stringResource(R.string.despesa_campo_descricao)) },
        placeholder = { Text(stringResource(R.string.despesa_campo_descricao_dica)) },
        singleLine = true,
        isError = estado.erros.containsKey(CampoDespesa.DESCRICAO),
        supportingText = estado.erroDe(CampoDespesa.DESCRICAO)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CampoCategoria(
    estado: DespesaFormUiState,
    categorias: List<Categoria>,
    onSelecionar: (Categoria) -> Unit,
    onGerenciarCategorias: () -> Unit,
) {
    CampoSelecao(
        rotulo = stringResource(R.string.despesa_campo_categoria),
        opcoes = categorias,
        selecionada = categorias.firstOrNull { it.id == estado.categoriaId },
        textoDe = { it.nome },
        onSelecionar = onSelecionar,
        iconeDe = { PontoCor(it.cor) },
        erro = estado.erroDe(CampoDespesa.CATEGORIA),
        acaoExtra = AcaoMenu(stringResource(R.string.despesa_gerenciar_categorias), onGerenciarCategorias),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CampoParcelas(estado: DespesaFormUiState, onValorChange: (String) -> Unit, ajuda: String? = null) {
    val erro = estado.erroDe(CampoDespesa.PARCELAS)
    OutlinedTextField(
        value = estado.totalParcelas,
        onValueChange = onValorChange,
        label = { Text(stringResource(R.string.despesa_campo_parcelas)) },
        singleLine = true,
        isError = erro != null,
        supportingText = (erro ?: ajuda)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CamposDespesaFixa(
    estado: DespesaFormUiState,
    categorias: List<Categoria>,
    avisoVale: AvisoVale?,
    viewModel: DespesaFormViewModel,
    onGerenciarCategorias: () -> Unit,
) {
    CampoDescricao(estado, viewModel::alterarDescricao)

    CampoCategoria(estado, categorias, viewModel::alterarCategoria, onGerenciarCategorias)

    CampoMoeda(
        valorCentavos = estado.valorCentavos,
        onValorChange = viewModel::alterarValor,
        rotulo = stringResource(R.string.despesa_campo_valor),
        erro = estado.erroDe(CampoDespesa.VALOR),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = estado.dia,
        onValueChange = viewModel::alterarDia,
        label = { Text(stringResource(R.string.despesa_campo_dia)) },
        singleLine = true,
        isError = estado.erros.containsKey(CampoDespesa.DIA),
        supportingText = {
            Text(estado.erroDe(CampoDespesa.DIA) ?: stringResource(R.string.despesa_campo_dia_ajuda))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )

    CampoSelecao(
        rotulo = stringResource(R.string.despesa_campo_forma),
        opcoes = FormaPagamento.entries,
        selecionada = estado.formaPagamento,
        textoDe = { stringResource(it.rotulo) },
        onSelecionar = viewModel::alterarFormaPagamento,
        modifier = Modifier.fillMaxWidth(),
    )

    // Paga com VR/VA: sai da carteira do vale, então "Pagar com" (salário/vale) não se aplica.
    if (avisoVale != null) {
        AvisoSaldoVale(avisoVale)
    } else {
        Text(stringResource(R.string.despesa_campo_vinculo), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            VinculoPeriodo.entries.forEachIndexed { indice, vinculo ->
                SegmentedButton(
                    selected = estado.vinculo == vinculo,
                    onClick = { viewModel.alterarVinculo(vinculo) },
                    shape = SegmentedButtonDefaults.itemShape(indice, VinculoPeriodo.entries.size),
                ) { Text(stringResource(vinculo.rotulo)) }
            }
        }
        if (estado.vinculo == VinculoPeriodo.AUTOMATICO) {
            TextoAjuda(stringResource(R.string.despesa_vinculo_ajuda))
        }
    }

    Text(stringResource(R.string.despesa_campo_recorrencia), style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Recorrencia.entries.forEachIndexed { indice, recorrencia ->
            SegmentedButton(
                selected = estado.recorrencia == recorrencia,
                onClick = { viewModel.alterarRecorrencia(recorrencia) },
                shape = SegmentedButtonDefaults.itemShape(indice, Recorrencia.entries.size),
            ) { Text(stringResource(recorrencia.rotulo)) }
        }
    }
    if (estado.recorrencia == Recorrencia.PARCELADA) {
        CampoParcelas(estado, viewModel::alterarTotalParcelas)
    }

    Text(
        stringResource(
            when (estado.recorrencia) {
                Recorrencia.PARCELADA -> R.string.despesa_campo_mes_inicio_parcelada
                Recorrencia.UNICA -> R.string.despesa_campo_mes_unica
                Recorrencia.MENSAL -> R.string.despesa_campo_mes_inicio
            },
        ),
        style = MaterialTheme.typography.labelLarge,
    )
    SeletorMes(
        mes = estado.mesInicio,
        onMesChange = viewModel::alterarMesInicio,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.despesa_campo_ativa),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = estado.ativa, onCheckedChange = viewModel::alterarAtiva)
    }
}

@Composable
private fun CamposCompraCartao(
    estado: DespesaFormUiState,
    categorias: List<Categoria>,
    cartoes: List<Cartao>,
    previa: PreviaFatura?,
    viewModel: DespesaFormViewModel,
    onGerenciarCategorias: () -> Unit,
    onNovoCartao: () -> Unit,
) {
    if (cartoes.isEmpty()) {
        TextoAjuda(stringResource(R.string.despesa_sem_cartoes))
        OutlinedButton(onClick = onNovoCartao, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cartoes_novo))
        }
        return
    }

    CampoSelecao(
        rotulo = stringResource(R.string.despesa_campo_cartao),
        opcoes = cartoes,
        selecionada = cartoes.firstOrNull { it.id == estado.cartaoId },
        textoDe = { it.nome },
        onSelecionar = viewModel::alterarCartao,
        erro = estado.erroDe(CampoDespesa.CARTAO),
        acaoExtra = AcaoMenu(stringResource(R.string.despesa_novo_cartao), onNovoCartao),
        modifier = Modifier.fillMaxWidth(),
    )

    CampoDescricao(estado, viewModel::alterarDescricao)

    CampoCategoria(estado, categorias, viewModel::alterarCategoria, onGerenciarCategorias)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.despesa_estorno),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = estado.estorno, onCheckedChange = viewModel::alterarEstorno)
    }
    if (estado.estorno) {
        TextoAjuda(stringResource(R.string.despesa_estorno_ajuda))
    }

    // Estorno não tem recorrência fixa.
    val modalidades = if (estado.estorno) ModalidadeCompra.entries - ModalidadeCompra.FIXO else ModalidadeCompra.entries
    Text(stringResource(R.string.despesa_campo_modalidade), style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modalidades.forEachIndexed { indice, modalidade ->
            SegmentedButton(
                selected = estado.modalidade == modalidade,
                onClick = { viewModel.alterarModalidade(modalidade) },
                shape = SegmentedButtonDefaults.itemShape(indice, modalidades.size),
            ) { Text(stringResource(modalidade.rotulo)) }
        }
    }

    CampoMoeda(
        valorCentavos = estado.valorCentavos,
        onValorChange = viewModel::alterarValor,
        rotulo = stringResource(
            when {
                estado.estorno -> R.string.despesa_campo_valor_estorno
                estado.modalidade == ModalidadeCompra.PARCELADO -> R.string.despesa_campo_valor_total
                estado.modalidade == ModalidadeCompra.FIXO -> R.string.despesa_campo_valor_mensal
                else -> R.string.despesa_campo_valor
            },
        ),
        erro = estado.erroDe(CampoDespesa.VALOR),
        modifier = Modifier.fillMaxWidth(),
    )

    if (estado.modalidade == ModalidadeCompra.PARCELADO) {
        CampoParcelas(
            estado = estado,
            onValorChange = viewModel::alterarTotalParcelas,
            ajuda = previa?.parcelas?.takeIf { estado.valorCentavos > 0 }?.let { resumoParcelas(it) },
        )
    }

    CampoData(
        rotulo = stringResource(R.string.despesa_campo_data_compra),
        data = estado.dataCompra,
        onDataChange = viewModel::alterarDataCompra,
        modifier = Modifier.fillMaxWidth(),
    )

    previa?.let {
        TextoAjuda(
            if (it.jaLancada) {
                stringResource(R.string.despesa_entrou_na_fatura, Formatadores.mesAno(it.mes))
            } else {
                stringResource(R.string.despesa_entra_na_fatura, Formatadores.mesAno(it.mes), Formatadores.data(it.fechamento))
            },
        )
    }

    // Encerrar só faz sentido para uma assinatura já lançada.
    if (estado.editando && estado.modalidade == ModalidadeCompra.FIXO) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.despesa_fixa_ativa),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = estado.fixaAtiva, onCheckedChange = viewModel::alterarFixaAtiva)
        }
        TextoAjuda(stringResource(R.string.despesa_fixa_ativa_ajuda))
        val ultima = estado.mesUltimaFatura
        if (!estado.fixaAtiva && ultima != null) {
            Text(stringResource(R.string.despesa_campo_ultima_fatura), style = MaterialTheme.typography.labelLarge)
            SeletorMes(mes = ultima, onMesChange = viewModel::alterarMesUltimaFatura, modifier = Modifier.fillMaxWidth())
            estado.erroDe(CampoDespesa.ENCERRAMENTO)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Saldo do VR/VA do mês e aviso quando a despesa o deixa negativo (salvar continua permitido). */
@Composable
private fun AvisoSaldoVale(aviso: AvisoVale) {
    val mes = Formatadores.mesAnoCompacto(aviso.mes)
    when {
        !aviso.possuiCredito && aviso.disponivelCentavos >= 0 ->
            TextoAjuda(stringResource(R.string.despesa_vale_sem_credito, mes))
        aviso.ficaraNegativo -> Text(
            stringResource(R.string.despesa_vale_aviso_negativo, mes, Formatadores.moeda(-aviso.saldoAposCentavos)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        else -> TextoAjuda(stringResource(R.string.despesa_vale_ajuda, mes, Formatadores.moeda(aviso.disponivelCentavos)))
    }
}

/** "5× de R$ 100,00" ou "2× de R$ 33,33 + última de R$ 33,34". */
@Composable
private fun resumoParcelas(parcelas: List<Long>): String {
    val primeira = parcelas.first()
    val ultima = parcelas.last()
    return if (primeira == ultima) {
        stringResource(R.string.despesa_parcelas_resumo, parcelas.size, Formatadores.moeda(primeira))
    } else {
        stringResource(
            R.string.despesa_parcelas_resumo_ultima,
            parcelas.size - 1,
            Formatadores.moeda(primeira),
            Formatadores.moeda(ultima),
        )
    }
}

@Composable
private fun TextoAjuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
