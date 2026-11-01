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
import com.rifsxd.ksunext.ui.util.RootPreset
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
    var preset by remember { mutableStateOf(RootPreset.Full) }

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
            Text("Profile:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RootPreset.values().forEach { pr ->
                    FilterChip(selected = preset == pr, onClick = { preset = pr }, label = { Text(pr.label) })
                }
            }
            Text("Apply it for:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10 min" to 10, "1 hour" to 60, "Today" to RootTimer.minutesUntilMidnight()).forEach { (t, m) ->
                    OutlinedButton(enabled = app != null, onClick = {
                        if (app != null) {
                            val ok = RootTimer.grantTemporary(context, app.packageName, app.uid, m, preset)
                            message = if (ok) "${preset.label} set for ${app.label} for $t" else "Could not change the profile"
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
    var on by remember { mutableStateOf(RootXPresets.safeModeOn(context)) }
    var showReboot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (Natives.isManager) modVm.fetchModuleList() }

    ToolCard("Safe mode", "Switch every module off until you turn it back on") {
        if (!Natives.isManager) {
            Text("Needs RootX to be active first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (on) "Safe mode is ON" else "Safe mode is off", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (on) "All modules stay off. Turn this off to bring them back." else "Turning it on switches all modules off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = on, onCheckedChange = { want ->
                    scope.launch {
                        if (want) {
                            RootXPresets.saveSnapshot(context, modVm.moduleList.filter { it.enabled }.map { it.id })
                            withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, false) } }
                            RootXPresets.setSafeMode(context, true)
                            on = true
                        } else {
                            val snap = RootXPresets.loadSnapshot(context) ?: emptyList()
                            withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, it.id in snap) } }
                            RootXPresets.setSafeMode(context, false)
                            on = false
                        }
                        showReboot = true
                    }
                })
            }
        }
    }
    RebootDialog(showReboot) { showReboot = false }
}
