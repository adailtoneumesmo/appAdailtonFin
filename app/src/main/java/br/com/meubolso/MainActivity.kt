package br.com.meubolso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.meubolso.ui.navigation.MeuBolsoNavHost
import br.com.meubolso.ui.theme.MeuBolsoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeuBolsoTheme {
                MeuBolsoNavHost()
            }
        }
    }
}
