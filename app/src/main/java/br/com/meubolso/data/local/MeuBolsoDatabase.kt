package br.com.meubolso.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.meubolso.data.local.cartao.CartaoDao
import br.com.meubolso.data.local.cartao.CartaoEntity
import br.com.meubolso.data.local.cartao.CompraCartaoEntity
import br.com.meubolso.data.local.categoria.CategoriaDao
import br.com.meubolso.data.local.categoria.CategoriaEntity
import br.com.meubolso.data.local.categoria.CategoriasPadrao
import br.com.meubolso.data.local.despesa.DespesaDao
import br.com.meubolso.data.local.despesa.DespesaFixaEntity
import br.com.meubolso.data.local.despesa.PagamentoDespesaEntity
import br.com.meubolso.data.local.listacompra.ItemListaCompraEntity
import br.com.meubolso.data.local.listacompra.ListaCompraDao
import br.com.meubolso.data.local.listacompra.ListaCompraEntity
import br.com.meubolso.data.local.notificacao.NotificacaoBancoDao
import br.com.meubolso.data.local.notificacao.NotificacaoBancoEntity
import br.com.meubolso.data.local.objetivo.ObjetivoDao
import br.com.meubolso.data.local.objetivo.ObjetivoEntity
import br.com.meubolso.data.local.objetivo.PosicaoInvestimentoEntity
import br.com.meubolso.data.local.renda.RendaDao
import br.com.meubolso.data.local.renda.RendaEntity
import br.com.meubolso.data.local.sincronizacao.ControleSincronizacaoEntity
import br.com.meubolso.data.local.sincronizacao.CursorSincronizacaoEntity
import br.com.meubolso.data.local.sincronizacao.GatilhosSincronizacao
import br.com.meubolso.data.local.sincronizacao.RegistroSincronizacaoEntity
import br.com.meubolso.data.local.sincronizacao.SincronizacaoDao

/**
 * Toda alteração de esquema exige incrementar [version] e adicionar uma migração
 * (automática ou em [MIGRACOES]). Nunca usar fallbackToDestructiveMigration.
 *
 * Histórico:
 * - v1: rendas
 * - v2: categorias (com as padrão), despesas_fixas e pagamentos_despesa
 * - v3: cartoes e compras_cartao
 * - v4: rendas.forma_calculo_vale (VR/VA), listas_compra e itens_lista_compra
 * - v5: notificacoes_banco (transações lidas das notificações, para confirmar)
 * - v6: sync_registro, sync_cursor e sync_controle (sincronização com a nuvem) + gatilhos
 * - v7: objetivos e posicoes_investimento (patrimônio investido), já sincronizados
 */
@Database(
    entities = [
        RendaEntity::class,
        CategoriaEntity::class,
        DespesaFixaEntity::class,
        PagamentoDespesaEntity::class,
        CartaoEntity::class,
        CompraCartaoEntity::class,
        ListaCompraEntity::class,
        ItemListaCompraEntity::class,
        NotificacaoBancoEntity::class,
        ObjetivoEntity::class,
        PosicaoInvestimentoEntity::class,
        RegistroSincronizacaoEntity::class,
        CursorSincronizacaoEntity::class,
        ControleSincronizacaoEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = MeuBolsoDatabase.Migracao1Para2::class),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = MeuBolsoDatabase.Migracao5Para6::class),
        AutoMigration(from = 6, to = 7, spec = MeuBolsoDatabase.Migracao6Para7::class),
    ],
)
@TypeConverters(Conversores::class)
abstract class MeuBolsoDatabase : RoomDatabase() {

    abstract fun rendaDao(): RendaDao

    abstract fun categoriaDao(): CategoriaDao

    abstract fun despesaDao(): DespesaDao

    abstract fun cartaoDao(): CartaoDao

    abstract fun listaCompraDao(): ListaCompraDao

    abstract fun notificacaoBancoDao(): NotificacaoBancoDao

    abstract fun objetivoDao(): ObjetivoDao

    abstract fun sincronizacaoDao(): SincronizacaoDao

    /** Tabelas novas são criadas pelo Room; aqui só populamos as categorias padrão. */
    class Migracao1Para2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) = CategoriasPadrao.inserir(db)
    }

    /** Tabelas de sincronização criadas pelo Room; aqui os gatilhos e o registro do que já existe (para subir). */
    class Migracao5Para6 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            GatilhosSincronizacao.instalar(db)
            GatilhosSincronizacao.registrarExistentes(db)
        }
    }

    /** Tabelas novas nascem vazias: só faltam os gatilhos delas (instalar ignora os que já existem). */
    class Migracao6Para7 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) = GatilhosSincronizacao.instalar(db)
    }

    /** Instalação nova: o banco já nasce na versão atual, sem passar pelas migrações. */
    object AoCriar : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            GatilhosSincronizacao.instalar(db)
            CategoriasPadrao.inserir(db)
            GatilhosSincronizacao.fixarUuidsCategoriasPadrao(db)
        }
    }

    companion object {
        const val NOME = "meu_bolso.db"

        val MIGRACOES: Array<Migration> = arrayOf()
    }
}
