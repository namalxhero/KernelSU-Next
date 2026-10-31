package com.rifsxd.ksunext.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BootGuardScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HideManagerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RootStatsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.ElevatedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun NovaToolCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Destination<RootGraph>
@Composable
fun NovaToolsScreen(navigator: DestinationsNavigator) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { NovaTopBar("Nova Tools") { navigator.popBackStack() } },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NovaToolCard(
                Icons.Filled.QueryStats,
                "Root usage stats",
                "Which apps used root, and how many times"
            ) { navigator.navigate(RootStatsScreenDestination) }
            NovaToolCard(
                Icons.Filled.Shield,
                "Bootloop rescue",
                "Auto-disable modules if the phone fails to boot 3 times"
            ) { navigator.navigate(BootGuardScreenDestination) }
            NovaToolCard(
                Icons.Filled.VisibilityOff,
                "Hide manager",
                "Reinstall this app under a random package name"
            ) { navigator.navigate(HideManagerScreenDestination) }
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
