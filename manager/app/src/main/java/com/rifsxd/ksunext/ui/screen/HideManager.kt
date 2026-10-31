package com.rifsxd.ksunext.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.ElevatedCard
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.ManagerHider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Destination<RootGraph>
@Composable
fun HideManagerScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current

    var hasKey by remember { mutableStateOf(ManagerHider.hasKey(context)) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var newPkg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            ManagerHider.keyFile(context).outputStream().use { out -> input.copyTo(out) }
                        }
                    }.isSuccess
                }
                hasKey = ManagerHider.hasKey(context)
                snackBarHost.showSnackbar(if (ok) "Key imported" else "Could not import key")
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { NovaTopBar("Hide manager") { navigator.popBackStack() } },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This copies the app, gives it a random package name, signs it with YOUR key and installs it next to this one. " +
                            "The kernel recognises the manager by that signature, so your key must be the one added to the Nova kernel.\n\n" +
                            "Your key file stays private inside this app. The password is never saved.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current package: ${context.packageName}", style = MaterialTheme.typography.labelMedium)
                }
            }

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("1. Signing key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (hasKey) "Key imported" else "Pick your keystore file (.p12 / .jks made by keytool)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy, onClick = { picker.launch(arrayOf("*/*")) }) {
                            Text(if (hasKey) "Replace key" else "Choose key file")
                        }
                        if (hasKey) {
                            OutlinedButton(enabled = !busy, onClick = {
                                ManagerHider.deleteKey(context)
                                hasKey = false
                            }) { Text("Delete key") }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("2. Key password", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        enabled = !busy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        enabled = hasKey && password.isNotEmpty() && !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                error = null
                                newPkg = null
                                val r = ManagerHider.hide(context, password) { step = it }
                                password = ""
                                busy = false
                                r.onSuccess { newPkg = it }
                                r.onFailure { error = it.message ?: it.toString() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("3. Hide now") }
                    if (busy) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(step, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            error?.let {
                Text("Failed: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            newPkg?.let { pkg ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Installed as", style = MaterialTheme.typography.labelMedium)
                        Text(pkg, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Open the hidden copy and check that root access works there. Only after that, remove this visible app.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { ManagerHider.launch(pkg) } }) { Text("Open hidden copy") }
                            OutlinedButton(onClick = {
                                scope.launch { ManagerHider.uninstall(context.packageName) }
                            }) { Text("Remove this app") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
