package br.com.meubolso.ui.conta

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.ui.components.LogoMeuBolso
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun EntrarScreen(viewModel: EntrarViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    fun entrarComGoogle() {
        val idCliente = idClienteWeb(context) ?: return viewModel.falhaGoogle()
        escopo.launch {
            try {
                val pedido = GetCredentialRequest.Builder()
                    .addCredentialOption(GetSignInWithGoogleOption.Builder(idCliente).build())
                    .build()
                val credencial = credentialManager.getCredential(context, pedido).credential
                if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    viewModel.entrarComGoogle(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
                } else {
                    viewModel.falhaGoogle()
                }
            } catch (_: GetCredentialCancellationException) {
                // Usuário fechou a escolha de conta.
            } catch (_: NoCredentialException) {
                // Nenhuma conta Google no aparelho.
                viewModel.falhaGoogle()
            } catch (_: GetCredentialException) {
                viewModel.falhaGoogle()
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            LogoMeuBolso(altura = 44.dp, modifier = Modifier.padding(top = 32.dp, bottom = 8.dp))
            Text(
                stringResource(if (estado.criandoConta) R.string.conta_titulo_criar else R.string.conta_titulo_entrar),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.conta_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            OutlinedButton(
                onClick = ::entrarComGoogle,
                enabled = !estado.carregando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.conta_google)) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    stringResource(R.string.conta_ou),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(Modifier.weight(1f))
            }

            if (estado.criandoConta) {
                OutlinedTextField(
                    value = estado.nome,
                    onValueChange = viewModel::alterarNome,
                    label = { Text(stringResource(R.string.conta_nome)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = estado.email,
                onValueChange = viewModel::alterarEmail,
                label = { Text(stringResource(R.string.conta_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.senha,
                onValueChange = viewModel::alterarSenha,
                label = { Text(stringResource(R.string.conta_senha)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            estado.erro?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            estado.aviso?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::confirmar,
                enabled = !estado.carregando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (estado.criandoConta) R.string.conta_criar else R.string.conta_entrar))
            }
            if (!estado.criandoConta) {
                TextButton(onClick = viewModel::redefinirSenha, enabled = !estado.carregando) {
                    Text(stringResource(R.string.conta_esqueci_senha))
                }
            }
            TextButton(onClick = { viewModel.alterarModo(!estado.criandoConta) }, enabled = !estado.carregando) {
                Text(stringResource(if (estado.criandoConta) R.string.conta_ja_tenho else R.string.conta_nao_tenho))
            }
        }
    }
}

/**
 * Gerado pelo plugin google-services a partir do google-services.json (só existe depois de
 * ativar o login com Google no console). Buscado em tempo de execução para o app compilar sem ele.
 */
@SuppressLint("DiscouragedApi")
private fun idClienteWeb(context: Context): String? =
    context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        .takeIf { it != 0 }
        ?.let(context::getString)
