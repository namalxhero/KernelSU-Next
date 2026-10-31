package com.rifsxd.ksunext.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.ElevatedCard
import com.rifsxd.ksunext.ui.util.AppRootStat
import com.rifsxd.ksunext.ui.util.loadRootStats

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun RootStatsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var stats by remember { mutableStateOf<List<AppRootStat>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        val r = runCatching { loadRootStats(context) }.getOrNull()
        stats = r?.first ?: emptyList()
        total = r?.second ?: 0
        loading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Root usage stats", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { reload++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (stats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No root activity found yet.\n\nTurn on \"sulog\" in Settings so root requests get recorded, then come back here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val max = (stats.maxOf { it.count }).coerceAtLeast(1)
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "$total root events from ${stats.size} apps",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(stats, key = { it.uid }) { s ->
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (s.pkg.isNotEmpty()) s.pkg else "uid ${s.uid}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text("${s.count}x", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { s.count.toFloat() / max.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            s.last?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Last: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
