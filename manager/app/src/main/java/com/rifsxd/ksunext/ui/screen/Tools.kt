package com.rifsxd.ksunext.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ui.component.XBoom
import com.rifsxd.ksunext.ui.component.XBoomMood
import com.rifsxd.ksunext.ui.component.glass
import com.rifsxd.ksunext.ui.util.CheckResult
import com.rifsxd.ksunext.ui.util.RootTimer
import com.rifsxd.ksunext.ui.util.RootXIcon
import com.rifsxd.ksunext.ui.util.RootXPin
import com.rifsxd.ksunext.ui.util.RootXPresets
import com.rifsxd.ksunext.ui.util.SelfCheck
import com.rifsxd.ksunext.ui.util.reboot
import com.rifsxd.ksunext.ui.util.toggleModule
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import com.rifsxd.ksunext.ui.viewmodel.SuperUserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Destination<RootGraph>
@Composable
fun ToolsScreen(navigator: DestinationsNavigator) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                XBoom(Modifier.height(58.dp), mood = XBoomMood.Happy, bob = true)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("RootX extras", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SelfCheckSection()
            RootTimerSection()
            PresetsSection()
            SafeModeSection()
            LogSection()
            SecuritySection()
            ReportSection()
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp))) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(open) {
            Column(
                Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun RebootDialog(show: Boolean, onDismiss: () -> Unit) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reboot needed") },
        text = { Text("Module changes apply after a reboot. Reboot now?") },
        confirmButton = { TextButton(onClick = { onDismiss(); reboot() }) { Text("Reboot") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } }
    )
}

// ---------------------------------------------------------------------------

@Composable
private fun SelfCheckSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<CheckResult>>(emptyList()) }
    var running by remember { mutableStateOf(false) }
    ToolCard("Self check", "Looks for root traces that apps can detect") {
        Button(onClick = {
            scope.launch {
                running = true
                results = withContext(Dispatchers.IO) { SelfCheck.run(context) }
                running = false
            }
        }) { Text(if (running) "Checking..." else "Run self check") }
        if (results.isNotEmpty()) {
            Text(
                "Passed ${results.count { it.ok }} of ${results.size}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            results.forEach { r ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(if (r.ok) "OK" else "!!", color = if (r.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                    Column {
                        Text(r.title, style = MaterialTheme.typography.bodyMedium)
                        Text(r.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(52.dp))
        OutlinedButton(onClick = { onChange((hour + 23) % 24) }) { Text("-") }
        Text("%02d:00".format(hour), fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = { onChange((hour + 1) % 24) }) { Text("+") }
    }
}

@Composable
private fun RootTimerSection() {
    val context = LocalContext.current
    val suVm: SuperUserViewModel = viewModel()
    var tick by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<SuperUserViewModel.AppInfo?>(null) }
    var menu by remember { mutableStateOf(false) }
    var startH by remember { mutableIntStateOf(22) }
    var endH by remember { mutableIntStateOf(6) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (Natives.isManager && suVm.appList.isEmpty()) suVm.fetchAppList()
    }

    ToolCard("Temporary and scheduled root", "Root that switches off by itself") {
        if (!Natives.isManager) {
            Text("Needs RootX to be active first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(selected?.label ?: "Choose an app") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    suVm.appList.take(200).forEach { app ->
                        DropdownMenuItem(text = { Text(app.label) }, onClick = { selected = app; menu = false })
                    }
                }
            }
            val app = selected
            Text("Give root for:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10 min" to 10, "1 hour" to 60, "Today" to RootTimer.minutesUntilMidnight()).forEach { (t, m) ->
                    OutlinedButton(enabled = app != null, onClick = {
                        if (app != null) {
                            val ok = RootTimer.grantTemporary(context, app.packageName, app.uid, m)
                            message = if (ok) "Root given to ${app.label} for $t" else "Could not change root"
                            tick++
                        }
                    }) { Text(t) }
                }
            }
            Text("Or only during these hours, every day:", style = MaterialTheme.typography.labelLarge)
            HourStepper("From", startH) { startH = it }
            HourStepper("To", endH) { endH = it }
            Button(enabled = app != null, onClick = {
                if (app != null) {
                    RootTimer.addWindow(context, app.packageName, app.uid, startH, endH)
                    message = "Daily window saved for ${app.label}"
                    tick++
                }
            }) { Text("Save daily window") }
            if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)

            val temps = remember(tick) { RootTimer.listTemps(context) }
            val wins = remember(tick) { RootTimer.listWindows(context) }
            temps.forEach { t ->
                val left = ((t.until - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${t.pkg}\n${left} min left", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { RootTimer.revokeNow(context, t.pkg); tick++ }) { Text("Revoke") }
                }
            }
            wins.forEach { w ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${w.pkg}\n%02d:00 to %02d:00 daily".format(w.startHour, w.endHour), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { RootTimer.removeWindow(context, w.pkg); tick++ }) { Text("Remove") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun PresetsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modVm: ModuleViewModel = viewModel()
    var presets by remember { mutableStateOf(RootXPresets.load(context)) }
    var name by remember { mutableStateOf("") }
    var showReboot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (Natives.isManager) modVm.fetchModuleList() }

    ToolCard("Module presets", "Switch whole sets of modules in one tap") {
        if (!Natives.isManager) {
            Text("Needs RootX to be active first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Preset name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = name.isNotBlank(), onClick = {
                RootXPresets.save(context, name.trim(), modVm.moduleList.filter { it.enabled }.map { it.id })
                presets = RootXPresets.load(context)
                name = ""
            }) { Text("Save enabled modules as preset") }
            presets.forEach { (pn, ids) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$pn (${ids.size})", Modifier.weight(1f))
                    TextButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, it.id in ids) } }
                            showReboot = true
                        }
                    }) { Text("Apply") }
                    TextButton(onClick = { RootXPresets.delete(context, pn); presets = RootXPresets.load(context) }) { Text("Delete") }
                }
            }
        }
    }
    RebootDialog(showReboot) { showReboot = false }
}

// ---------------------------------------------------------------------------

@Composable
private fun SafeModeSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modVm: ModuleViewModel = viewModel()
    var showReboot by remember { mutableStateOf(false) }
    var confirmDisable by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { if (Natives.isManager) modVm.fetchModuleList() }

    ToolCard("Safe mode", "Get out of a bad module quickly") {
        if (!Natives.isManager) {
            Text("Needs RootX to be active first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Button(onClick = {
                RootXPresets.saveSnapshot(context, modVm.moduleList.filter { it.enabled }.map { it.id })
                info = "Snapshot saved"
            }) { Text("Save a safe snapshot now") }
            OutlinedButton(onClick = {
                val snap = RootXPresets.loadSnapshot(context)
                if (snap == null) {
                    info = "No snapshot saved yet"
                } else {
                    scope.launch {
                        withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, it.id in snap) } }
                        showReboot = true
                    }
                }
            }) { Text("Restore the snapshot") }
            OutlinedButton(onClick = { confirmDisable = true }) { Text("Disable all modules") }
            if (info.isNotEmpty()) Text(info, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmDisable) {
        AlertDialog(
            onDismissRequest = { confirmDisable = false },
            title = { Text("Disable all modules?") },
            text = { Text("Every module will be switched off after the next reboot.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisable = false
                    scope.launch {
                        withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, false) } }
                        showReboot = true
                    }
                }) { Text("Disable all") }
            },
            dismissButton = { TextButton(onClick = { confirmDisable = false }) { Text("Cancel") } }
        )
    }
    RebootDialog(showReboot) { showReboot = false }
}

// ---------------------------------------------------------------------------

@Composable
private fun LogSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    ToolCard("Log viewer", "Read system and kernel logs") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Logcat", "Kernel").forEachIndexed { i, s ->
                FilterChip(selected = source == i, onClick = { source = i }, label = { Text(s) })
            }
        }
        OutlinedTextField(value = filter, onValueChange = { filter = it }, label = { Text("Filter (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                loading = true
                text = withContext(Dispatchers.IO) {
                    val cmd = if (source == 0) "logcat -d -t 800" else "dmesg | tail -n 600"
                    val lines = runCatching { com.topjohnwu.superuser.Shell.cmd(cmd).exec().out }.getOrDefault(emptyList())
                    val f = filter.trim()
                    (if (f.isEmpty()) lines else lines.filter { it.contains(f, ignoreCase = true) }).takeLast(300).joinToString("\n")
                }
                loading = false
            }
        }) { Text(if (loading) "Reading..." else "Read log") }
        if (text.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .background(Color(0x66000000), RoundedCornerShape(12.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            TextButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("RootX log", text))
            }) { Text("Copy log") }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun SecuritySection() {
    val context = LocalContext.current
    var hasPin by remember { mutableStateOf(RootXPin.has(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(RootXIcon.isHidden(context)) }
    var confirmHide by remember { mutableStateOf(false) }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    ToolCard("Security", "PIN lock and a hidden app icon") {
        Text(if (hasPin) "PIN lock is on" else "PIN lock is off", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pin1 = ""; pin2 = ""; pinError = ""; showPinDialog = true }) { Text(if (hasPin) "Change PIN" else "Set PIN") }
            if (hasPin) OutlinedButton(onClick = { RootXPin.clear(context); hasPin = false }) { Text("Remove PIN") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hide app icon", fontWeight = FontWeight.SemiBold)
                Text("Open it later by dialing *#*#7668#*#*", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = hidden, onCheckedChange = { want ->
                if (want) confirmHide = true else { RootXIcon.setHidden(context, false); hidden = false }
            })
        }
    }
    if (confirmHide) {
        AlertDialog(
            onDismissRequest = { confirmHide = false },
            title = { Text("Hide the app icon?") },
            text = { Text("The icon disappears from the launcher. To open RootX, dial *#*#7668#*#* in the phone app. This code may not work on every phone, so test it before relying on it.") },
            confirmButton = { TextButton(onClick = { confirmHide = false; RootXIcon.setHidden(context, true); hidden = true }) { Text("Hide") } },
            dismissButton = { TextButton(onClick = { confirmHide = false }) { Text("Cancel") } }
        )
    }
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set a PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pin1, onValueChange = { pin1 = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("PIN (4 to 8 digits)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    OutlinedTextField(value = pin2, onValueChange = { pin2 = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("Repeat PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    if (pinError.isNotEmpty()) Text(pinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pin1.length < 4 -> pinError = "Use at least 4 digits"
                        pin1 != pin2 -> pinError = "The PINs do not match"
                        else -> { RootXPin.set(context, pin1); hasPin = true; showPinDialog = false }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
        )
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun ReportSection() {
    val context = LocalContext.current
    val modVm: ModuleViewModel = viewModel()
    LaunchedEffect(Unit) { if (Natives.isManager) modVm.fetchModuleList() }
    ToolCard("Debug report", "Share device and module info when something breaks") {
        Button(onClick = {
            val sb = StringBuilder()
            sb.appendLine("RootX debug report")
            sb.appendLine("Manager: ${getManagerVersion(context).first}")
            sb.appendLine("Kernel: ${System.getProperty("os.version")}")
            sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (${android.os.Build.VERSION.SDK_INT})")
            sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            sb.appendLine("Recognized by kernel: ${Natives.isManager}")
            sb.appendLine("Modules:")
            if (Natives.isManager) {
                modVm.moduleList.forEach { sb.appendLine("  ${it.id} ${it.version} enabled=${it.enabled}") }
            }
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, sb.toString())
            context.startActivity(Intent.createChooser(send, "Share report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text("Share debug report") }
    }
}
