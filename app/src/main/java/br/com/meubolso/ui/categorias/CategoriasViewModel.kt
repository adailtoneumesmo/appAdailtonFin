package br.com.meubolso.ui.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.validacao.ErroValidacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Cores oferecidas para categorias personalizadas. */
val PALETA_CATEGORIAS: List<Int> = listOf(
    0xFF127A45, 0xFFE8A317, 0xFF2F6FB0, 0xFFC2477A, 0xFF7B5EA7, 0xFFE07B39,
    0xFF2BA3A3, 0xFF0E3B24, 0xFF7D8A80, 0xFF8D6E63, 0xFF5C6BC0, 0xFF9E9D24,
).map { it.toInt() }

data class DialogoCategoria(
    val id: Long? = null,
    val nome: String = "",
    val cor: Int = PALETA_CATEGORIAS.first(),
    val erro: ErroValidacao? = null,
)

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val repository: CategoriaRepository,
) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> = repository.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dialogo = MutableStateFlow<DialogoCategoria?>(null)
    val dialogo: StateFlow<DialogoCategoria?> = _dialogo.asStateFlow()

    /** Quantidade de despesas que impediram a última exclusão (null = sem aviso pendente). */
    private val _exclusaoBloqueada = MutableStateFlow<Int?>(null)
    val exclusaoBloqueada: StateFlow<Int?> = _exclusaoBloqueada.asStateFlow()

    fun novaCategoria() {
        _dialogo.value = DialogoCategoria()
    }

    fun editar(categoria: Categoria) {
        if (categoria.padrao) return
        _dialogo.value = DialogoCategoria(id = categoria.id, nome = categoria.nome, cor = categoria.cor)
    }

    fun alterarNome(nome: String) = _dialogo.update { it?.copy(nome = nome, erro = null) }

    fun alterarCor(cor: Int) = _dialogo.update { it?.copy(cor = cor) }

    fun fecharDialogo() {
        _dialogo.value = null
    }

    fun salvar() {
        val dialogo = _dialogo.value ?: return
        val nome = dialogo.nome.trim()
        val erro = when {
            nome.isEmpty() -> ErroValidacao.OBRIGATORIO
            categorias.value.any { it.id != dialogo.id && it.nome.equals(nome, ignoreCase = true) } ->
                ErroValidacao.NOME_DUPLICADO
            else -> null
        }
        if (erro != null) {
            _dialogo.value = dialogo.copy(erro = erro)
            return
        }
        viewModelScope.launch {
            repository.salvar(Categoria(id = dialogo.id ?: 0, nome = nome, cor = dialogo.cor))
            _dialogo.value = null
        }
    }

    fun excluir(categoria: Categoria) {
        if (categoria.padrao) return
        viewModelScope.launch {
            val emUso = repository.excluir(categoria.id)
            if (emUso > 0) _exclusaoBloqueada.value = emUso
        }
    }

    fun avisoExibido() {
        _exclusaoBloqueada.value = null
    }
}
