package com.rifsxd.ksunext.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.ElevatedCard
import com.rifsxd.ksunext.ui.util.*
import kotlinx.coroutines.launch

@Destination<RootGraph>
@Composable
fun BootGuardScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current
    var status by remember { mutableStateOf(BootGuardStatus(false, null, 0)) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        status = runCatching { readBootGuardStatus() }.getOrDefault(BootGuardStatus(false, null, 0))
    }

    fun doAction(okMsg: String, failMsg: String, block: suspend () -> Boolean) {
        scope.launch {
            busy = true
            val ok = runCatching { block() }.getOrDefault(false)
            busy = false
            reload++
            snackBarHost.showSnackbar(if (ok) okMsg else failMsg)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { NovaTopBar("Bootloop rescue") { navigator.popBackStack() } },
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
                    Text("Nova Boot Guard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Installs a small module that counts failed boots. If the phone does not finish booting 3 times in a row, " +
                            "it disables every other module so you can get back in. A reboot is needed after install.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (status.installed) "Status: installed (failed boots counted: ${status.bootCount})" else "Status: not installed",
                        style = MaterialTheme.typography.labelLarge
                    )
                    status.triggered?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Rescue ran on: $it. All other modules were disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!status.installed) {
                            Button(enabled = !busy, onClick = {
                                doAction("Installed. Reboot to activate.", "Install failed") { installBootGuard(context) }
                            }) { Text("Install") }
                        } else {
                            OutlinedButton(enabled = !busy, onClick = {
                                doAction("Removed", "Remove failed") { removeBootGuard() }
                            }) { Text("Remove") }
                        }
                        if (status.triggered != null) {
                            TextButton(enabled = !busy, onClick = {
                                doAction("Cleared", "Failed") { clearBootGuardFlag() }
                            }) { Text("Clear notice") }
                        }
                    }
                }
            }

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Manual panic buttons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Disable every module right now (they stay installed). Re-enable turns on all modules, including ones you disabled yourself. Changes apply after a reboot.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy, onClick = {
                            doAction("All modules disabled. Reboot to apply.", "Failed") { disableAllModulesNow() }
                        }) { Text("Disable all") }
                        OutlinedButton(enabled = !busy, onClick = {
                            doAction("All modules enabled. Reboot to apply.", "Failed") { enableAllModulesNow() }
                        }) { Text("Re-enable all") }
                    }
                }
            }

            Text(
                "Tip: also use the Module backup in Settings > Backup & restore before big changes.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
