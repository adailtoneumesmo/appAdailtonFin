package br.com.meubolso.ui.cartoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.domain.cartao.CalculadoraFatura
import br.com.meubolso.domain.model.Fatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class CartoesUiState(
    val carregando: Boolean = true,
    /** Fatura aberta hoje de cada cartão (a que recebe uma compra feita agora). */
    val faturasAtuais: List<Fatura> = emptyList(),
)

@HiltViewModel
class CartoesViewModel @Inject constructor(
    cartaoRepository: CartaoRepository,
    private val calculadora: CalculadoraFatura,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<CartoesUiState> = combine(
        cartaoRepository.observarCartoes(),
        cartaoRepository.observarCompras(),
    ) { cartoes, compras ->
        val hoje = LocalDate.now(clock)
        CartoesUiState(
            carregando = false,
            faturasAtuais = cartoes.map { cartao ->
                calculadora.fatura(cartao, compras, calculadora.faturaDaCompra(cartao, hoje))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartoesUiState())
}
