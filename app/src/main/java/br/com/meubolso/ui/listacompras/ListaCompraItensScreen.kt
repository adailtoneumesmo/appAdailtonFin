package br.com.meubolso.ui.listacompras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.listacompra.CalculadoraListaCompra
import br.com.meubolso.domain.listacompra.CampoListaCompra
import br.com.meubolso.domain.listacompra.ResumoListaCompra
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.ItemListaCompra
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.ui.components.CampoSelecao
import br.com.meubolso.ui.components.PontoCor
import br.com.meubolso.ui.components.mensagem
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

/** Até 99.999.999,99 por item. */
private const val MAX_DIGITOS_VALOR = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCompraItensScreen(
    onVoltar: () -> Unit,
    onEditarLista: (Long) -> Unit,
    viewModel: ListaCompraItensViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val novoItem by viewModel.novoItem.collectAsStateWithLifecycle()
    var itemAExcluir by remember { mutableStateOf<ItemListaCompra?>(null) }
    var finalizacao by remember { mutableStateOf<FinalizacaoUi?>(null) }

    // A lista foi excluída enquanto a tela estava aberta.
    LaunchedEffect(estado.carregando, estado.lista) {
        if (!estado.carregando && estado.lista == null) onVoltar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(estado.lista?.descricao.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        estado.lista?.let {
                            Text(
                                Formatadores.data(it.data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
                actions = {
                    estado.lista?.let { lista ->
                        IconButton(onClick = { onEditarLista(lista.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.listas_editar))
                        }
                    }
                },
            )
        },
        bottomBar = {
            RodapeItens(
                resumo = estado.resumo,
                finalizada = estado.lista?.finalizada == true,
                onFinalizar = { finalizacao = viewModel.finalizacaoInicial() },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(padding)
                .imePadding(),
        ) {
            item {
                CardNovoItem(
                    estado = novoItem,
                    onNome = viewModel::alterarNovoNome,
                    onValor = viewModel::alterarNovoValor,
                    onQuantidade = viewModel::alterarNovaQuantidade,
                    onAdicionar = viewModel::adicionarItem,
                )
            }
            item { CabecalhoTabela() }
            if (!estado.carregando && estado.itens.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.itens_vazio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(estado.itens, key = { it.id }) { item ->
                LinhaItem(
                    item = item,
                    onAlterar = viewModel::alterarItem,
                    onAlternarCarrinho = { viewModel.alternarCarrinho(item) },
                    onPedirExclusao = { itemAExcluir = item },
                )
                HorizontalDivider()
            }
            if (estado.itens.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.itens_dica_excluir),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    itemAExcluir?.let { item ->
        AlertDialog(
            onDismissRequest = { itemAExcluir = null },
            title = { Text(stringResource(R.string.itens_excluir_titulo)) },
            text = { Text(stringResource(R.string.itens_excluir_mensagem, item.nome)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirItem(item)
                    itemAExcluir = null
                }) { Text(stringResource(R.string.acao_excluir)) }
            },
            dismissButton = {
                TextButton(onClick = { itemAExcluir = null }) { Text(stringResource(R.string.acao_cancelar)) }
            },
        )
    }

    finalizacao?.let { escolha ->
        val categorias by viewModel.categorias.collectAsStateWithLifecycle()
        val cartoes by viewModel.cartoes.collectAsStateWithLifecycle()
        val saldoVale by viewModel.saldoVale.collectAsStateWithLifecycle()
        DialogoFinalizar(
            escolha = escolha,
            resumo = estado.resumo,
            valor = viewModel.valorFinalizacao(escolha),
            dataLista = estado.lista?.data?.let(Formatadores::data).orEmpty(),
            categorias = categorias,
            cartoes = cartoes,
            saldoVale = saldoVale,
            onAlterar = { finalizacao = it },
            onConfirmar = {
                viewModel.finalizar(escolha)
                finalizacao = null
            },
            onCancelar = { finalizacao = null },
        )
    }
}

@Composable
private fun CardNovoItem(
    estado: NovoItemUiState,
    onNome: (String) -> Unit,
    onValor: (Long) -> Unit,
    onQuantidade: (String) -> Unit,
    onAdicionar: () -> Boolean,
) {
    val focoNome = remember { FocusRequester() }
    // Depois de incluir, o cursor volta para o nome do próximo item.
    val adicionar = { if (onAdicionar()) focoNome.requestFocus() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val erroNome = estado.erros[CampoListaCompra.NOME_ITEM]?.let { stringResource(it.mensagem) }
            OutlinedTextField(
                value = estado.nome,
                onValueChange = onNome,
                label = { Text(stringResource(R.string.itens_campo_nome)) },
                singleLine = true,
                isError = erroNome != null,
                supportingText = erroNome?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focoNome),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoMoeda(
                    valorCentavos = estado.valorCentavos,
                    onValorChange = onValor,
                    rotulo = stringResource(R.string.itens_campo_valor),
                    modifier = Modifier.weight(1f),
                )
                val erroQtd = estado.erros[CampoListaCompra.QUANTIDADE]?.let { stringResource(it.mensagem) }
                OutlinedTextField(
                    value = estado.quantidade,
                    onValueChange = onQuantidade,
                    label = { Text(stringResource(R.string.itens_campo_qtd)) },
                    placeholder = { Text("1") },
                    singleLine = true,
                    isError = erroQtd != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { adicionar() }),
                    modifier = Modifier.width(88.dp),
                )
                FilledIconButton(onClick = adicionar) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.itens_adicionar))
                }
            }
            estado.erros[CampoListaCompra.QUANTIDADE]?.let {
                Text(stringResource(it.mensagem), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Larguras das colunas: checkbox fixa; demais proporcionais. */
private object Colunas {
    const val NOME = 3f
    const val VALOR = 2f
    const val QTD = 1.3f
    const val TOTAL = 2.2f
    val CHECKBOX = 40.dp
}

@Composable
private fun CabecalhoTabela() {
    val estilo = MaterialTheme.typography.labelMedium
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    ) {
        Box(Modifier.width(Colunas.CHECKBOX))
        Text(stringResource(R.string.itens_coluna_nome), style = estilo, modifier = Modifier.weight(Colunas.NOME))
        Text(stringResource(R.string.itens_coluna_valor), style = estilo, textAlign = TextAlign.End, modifier = Modifier.weight(Colunas.VALOR))
        Text(stringResource(R.string.itens_coluna_qtd), style = estilo, textAlign = TextAlign.Center, modifier = Modifier.weight(Colunas.QTD))
        Text(stringResource(R.string.itens_coluna_total), style = estilo, textAlign = TextAlign.End, modifier = Modifier.weight(Colunas.TOTAL))
    }
}

/**
 * Linha editável. Os campos guardam o texto localmente (o cursor não pula enquanto o banco
 * devolve o valor) e cada alteração válida é gravada com todos os campos da linha.
 * Deslizar para o lado pede confirmação para excluir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinhaItem(
    item: ItemListaCompra,
    onAlterar: (ItemListaCompra) -> Unit,
    onAlternarCarrinho: () -> Unit,
    onPedirExclusao: () -> Unit,
) {
    var nome by remember(item.id) { mutableStateOf(item.nome) }
    var valorCentavos by remember(item.id) { mutableLongStateOf(item.valorCentavos) }
    var quantidade by remember(item.id) {
        mutableStateOf(CalculadoraListaCompra.quantidadeParaTexto(item.quantidadeMilesimos))
    }
    val quantidadeMilesimos = CalculadoraListaCompra.quantidadeDeTexto(quantidade)
    val totalCentavos = CalculadoraListaCompra.totalItem(valorCentavos, quantidadeMilesimos ?: item.quantidadeMilesimos)

    /** Lê o estado atual (não o da última composição); nome vazio ou quantidade inválida não são gravados. */
    fun gravar() {
        val qtd = CalculadoraListaCompra.quantidadeDeTexto(quantidade) ?: return
        if (nome.isBlank()) return
        onAlterar(item.copy(nome = nome.trim(), valorCentavos = valorCentavos, quantidadeMilesimos = qtd))
    }

    val swipe = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            onPedirExclusao()
            swipe.reset()
        }
    }

    SwipeToDismissBox(
        state = swipe,
        backgroundContent = {
            Box(
                contentAlignment = if (swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.acao_excluir),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        val riscado = if (item.noCarrinho) TextDecoration.LineThrough else TextDecoration.None
        val estilo = MaterialTheme.typography.bodyMedium.copy(
            color = if (item.noCarrinho) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = riscado,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 4.dp),
        ) {
            Checkbox(
                checked = item.noCarrinho,
                onCheckedChange = { onAlternarCarrinho() },
                modifier = Modifier.width(Colunas.CHECKBOX),
            )
            CelulaTexto(
                valor = nome,
                onValorChange = {
                    nome = it
                    gravar()
                },
                estilo = estilo,
                erro = nome.isBlank(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.weight(Colunas.NOME),
            )
            CelulaMoeda(
                valorCentavos = valorCentavos,
                onValorChange = {
                    valorCentavos = it
                    gravar()
                },
                estilo = estilo.copy(textAlign = TextAlign.End),
                modifier = Modifier.weight(Colunas.VALOR),
            )
            CelulaTexto(
                valor = quantidade,
                onValorChange = {
                    quantidade = ListaCompraItensViewModel.filtrarQuantidade(it)
                    gravar()
                },
                estilo = estilo.copy(textAlign = TextAlign.Center),
                erro = quantidadeMilesimos == null,
                placeholder = "1",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.weight(Colunas.QTD),
            )
            Text(
                Formatadores.decimal(totalCentavos),
                style = estilo.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.weight(Colunas.TOTAL),
            )
        }
    }
}

@Composable
private fun CelulaTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    estilo: TextStyle,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    erro: Boolean = false,
    placeholder: String? = null,
) {
    val corFundo = if (erro) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    BasicTextField(
        value = valor,
        onValueChange = onValorChange,
        singleLine = true,
        textStyle = estilo,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { campo ->
            Box {
                if (valor.isEmpty() && placeholder != null) {
                    Text(placeholder, style = estilo.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                campo()
            }
        },
        modifier = modifier
            .background(corFundo, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/** Mesmo comportamento do CampoMoeda (dígitos entram como centavos), sem o "R$" e sem borda. */
@Composable
private fun CelulaMoeda(
    valorCentavos: Long,
    onValorChange: (Long) -> Unit,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
) {
    val texto = if (valorCentavos == 0L) "" else Formatadores.decimal(valorCentavos)
    BasicTextField(
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { novo ->
            val digitos = novo.text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITOS_VALOR)
            onValorChange(digitos.toLongOrNull() ?: 0L)
        },
        singleLine = true,
        textStyle = estilo,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { campo ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (texto.isEmpty()) {
                    Text(Formatadores.decimal(0), style = estilo.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                campo()
            }
        },
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/** Fixo no rodapé e atualizado a cada alteração de item. */
@Composable
private fun RodapeItens(resumo: ResumoListaCompra, finalizada: Boolean, onFinalizar: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        // Sem a barra de abas nesta tela, o rodapé desvia sozinho dos botões de navegação do aparelho.
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LinhaRodape(stringResource(R.string.itens_preco_total), Formatadores.moeda(resumo.totalCentavos), destaque = true)
            LinhaRodape(stringResource(R.string.itens_preco_atual), Formatadores.moeda(resumo.carrinhoCentavos))
            resumo.restanteCentavos?.let { restante ->
                if (resumo.acimaDoLimite) {
                    Text(
                        stringResource(R.string.itens_acima_limite, Formatadores.moeda(-restante)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    LinhaRodape(stringResource(R.string.itens_ainda_tem), Formatadores.moeda(restante))
                }
            }
            if (finalizada) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.itens_finalizada),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                Button(
                    onClick = onFinalizar,
                    enabled = resumo.totalCentavos > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) { Text(stringResource(R.string.itens_finalizar)) }
            }
        }
    }
}

@Composable
private fun LinhaRodape(rotulo: String, valor: String, destaque: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DialogoFinalizar(
    escolha: FinalizacaoUi,
    resumo: ResumoListaCompra,
    valor: Long,
    dataLista: String,
    categorias: List<Categoria>,
    cartoes: List<Cartao>,
    saldoVale: SugestaoVale?,
    onAlterar: (FinalizacaoUi) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val cartao = escolha.formaPagamento == FormaPagamento.CARTAO_CREDITO
    val podeConfirmar = valor > 0 && escolha.categoriaId != null && (!cartao || escolha.cartaoId != null)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.finalizar_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.finalizar_valor), style = MaterialTheme.typography.labelLarge)
                OpcaoValor(
                    texto = stringResource(R.string.finalizar_valor_carrinho, Formatadores.moeda(resumo.carrinhoCentavos)),
                    selecionada = escolha.usarCarrinho,
                    onSelecionar = { onAlterar(escolha.copy(usarCarrinho = true)) },
                )
                OpcaoValor(
                    texto = stringResource(R.string.finalizar_valor_total, Formatadores.moeda(resumo.totalCentavos)),
                    selecionada = !escolha.usarCarrinho,
                    onSelecionar = { onAlterar(escolha.copy(usarCarrinho = false)) },
                )
                CampoSelecao(
                    rotulo = stringResource(R.string.finalizar_categoria),
                    opcoes = categorias,
                    selecionada = categorias.firstOrNull { it.id == escolha.categoriaId },
                    textoDe = { it.nome },
                    onSelecionar = { onAlterar(escolha.copy(categoriaId = it.id)) },
                    iconeDe = { PontoCor(it.cor) },
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoSelecao(
                    rotulo = stringResource(R.string.finalizar_forma),
                    opcoes = FormaPagamento.entries,
                    selecionada = escolha.formaPagamento,
                    textoDe = { stringResource(it.rotulo) },
                    onSelecionar = { onAlterar(escolha.copy(formaPagamento = it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (cartao) {
                    if (cartoes.isEmpty()) {
                        Text(stringResource(R.string.finalizar_sem_cartao), color = MaterialTheme.colorScheme.error)
                    } else {
                        CampoSelecao(
                            rotulo = stringResource(R.string.finalizar_cartao),
                            opcoes = cartoes,
                            selecionada = cartoes.firstOrNull { it.id == escolha.cartaoId },
                            textoDe = { it.nome },
                            onSelecionar = { onAlterar(escolha.copy(cartaoId = it.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (escolha.formaPagamento == FormaPagamento.VALE_ALIMENTACAO && saldoVale != null) {
                    val mes = Formatadores.mesAnoCompacto(saldoVale.mes)
                    val disponivel = saldoVale.saldo.saldoCentavos
                    val apos = disponivel - valor
                    Text(
                        if (apos < 0) {
                            stringResource(R.string.despesa_vale_aviso_negativo, mes, Formatadores.moeda(-apos))
                        } else {
                            stringResource(R.string.despesa_vale_ajuda, mes, Formatadores.moeda(disponivel))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (apos < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    when {
                        valor <= 0 -> stringResource(R.string.finalizar_valor_zero)
                        cartao -> stringResource(R.string.finalizar_ajuda_cartao, dataLista)
                        else -> stringResource(R.string.finalizar_ajuda, dataLista)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valor <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = podeConfirmar) {
                Text(stringResource(R.string.finalizar_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.acao_cancelar)) }
        },
    )
}

@Composable
private fun OpcaoValor(texto: String, selecionada: Boolean, onSelecionar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selecionada, onClick = onSelecionar, role = Role.RadioButton),
    ) {
        RadioButton(selected = selecionada, onClick = null)
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}
