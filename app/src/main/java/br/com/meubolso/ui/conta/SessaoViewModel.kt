package br.com.meubolso.ui.conta

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.data.remoto.ContaRepository
import br.com.meubolso.data.remoto.EstadoSessao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SessaoViewModel @Inject constructor(conta: ContaRepository) : ViewModel() {
    val estado: StateFlow<EstadoSessao> = conta.estado
}

/** Só libera o app com conta e orçamento definidos; antes disso, login e escolha do orçamento. */
@Composable
fun PortaoSessao(viewModel: SessaoViewModel = hiltViewModel(), conteudo: @Composable () -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    when (val atual = estado) {
        EstadoSessao.Carregando -> Surface(Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        EstadoSessao.Deslogado -> EntrarScreen()
        is EstadoSessao.SemOrcamento -> OrcamentoScreen(atual.usuario)
        is EstadoSessao.Pronto -> conteudo()
    }
}
