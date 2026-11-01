package com.rifsxd.ksunext.ui.screen

import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Security
import com.rifsxd.ksunext.ui.util.toggleModule
import com.rifsxd.ksunext.ui.util.RootXPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Switch
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ui.util.RootXPin
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel

/** RootX extras that live at the bottom of Settings: PIN lock and the debug report. */
@Composable
fun RootXSettingsSection() {
    val context = LocalContext.current
    val modVm: ModuleViewModel = viewModel()
    var hasPin by remember { mutableStateOf(RootXPin.has(context)) }
    var showPin by remember { mutableStateOf(false) }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var safeOn by remember { mutableStateOf(RootXPresets.safeModeOn(context)) }
    var showReboot by remember { mutableStateOf(false) }
    var secureOn by remember {
        mutableStateOf(context.getSharedPreferences("rootx_security", android.content.Context.MODE_PRIVATE).getBoolean("secure_screen", true))
    }

    LaunchedEffect(Unit) { if (Natives.isManager) modVm.fetchModuleList() }

    Text(
        "RootX",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp)
    )

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { pin1 = ""; pin2 = ""; pinError = ""; showPin = true },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Filled.Lock, null) },
        headlineContent = {
            Text("PIN lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = { Text(if (hasPin) "On. Tap to change or remove" else "Off. Tap to set a PIN") }
    )

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Filled.Security, null) },
        headlineContent = {
            Text("Safe mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                if (!Natives.isManager) "Needs RootX to be active first"
                else if (safeOn) "On. All modules are switched off"
                else "Off. Turn on to switch every module off"
            )
        },
        trailingContent = {
            Switch(
                checked = safeOn,
                enabled = Natives.isManager,
                onCheckedChange = { want ->
                    scope.launch {
                        if (want) {
                            RootXPresets.saveSnapshot(context, modVm.moduleList.filter { it.enabled }.map { it.id })
                            withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, false) } }
                            RootXPresets.setSafeMode(context, true)
                            safeOn = true
                        } else {
                            val snap = RootXPresets.loadSnapshot(context) ?: emptyList()
                            withContext(Dispatchers.IO) { modVm.moduleList.forEach { toggleModule(it.id, it.id in snap) } }
                            RootXPresets.setSafeMode(context, false)
                            safeOn = false
                        }
                        showReboot = true
                    }
                }
            )
        }
    )
    RebootDialog(showReboot) { showReboot = false }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Filled.VisibilityOff, null) },
        headlineContent = {
            Text("Block screenshots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = { Text("Hides RootX from screenshots, screen recording and the recent apps view") },
        trailingContent = {
            Switch(
                checked = secureOn,
                onCheckedChange = {
                    secureOn = it
                    (context as? com.rifsxd.ksunext.ui.MainActivity)?.setSecureScreen(it)
                }
            )
        }
    )

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
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
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Filled.Share, null) },
        headlineContent = {
            Text("Debug report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = { Text("Share device and module info when something breaks") }
    )

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false },
            title = { Text(if (hasPin) "Change PIN" else "Set a PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { pin1 = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("PIN (4 to 8 digits)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { pin2 = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("Repeat PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (pinError.isNotEmpty()) {
                        Text(pinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pin1.length < 4 -> pinError = "Use at least 4 digits"
                        pin1 != pin2 -> pinError = "The PINs do not match"
                        else -> { RootXPin.set(context, pin1); hasPin = true; showPin = false }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row2(hasPin, onRemove = { RootXPin.clear(context); hasPin = false; showPin = false }, onCancel = { showPin = false })
            }
        )
    }
}

@Composable
private fun Row2(hasPin: Boolean, onRemove: () -> Unit, onCancel: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        if (hasPin) TextButton(onClick = onRemove) { Text("Remove PIN") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
