package com.example.spotifywidget.ui.component

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spotifywidget.service.FloatingWidgetService
import com.example.spotifywidget.utils.SongInfoManager

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatingWidgetControlCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isWidgetRunning by remember { mutableStateOf(false) }

    // Observe current song to show widget status
    val currentSong by SongInfoManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by SongInfoManager.isPlaying.collectAsStateWithLifecycle()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎵 Floating Music Circle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Current status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = if (isPlaying) "🟢 Music Playing" else "⚫ No Music",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )

                    if (currentSong != null) {
                        Text(
                            text = "${currentSong!!.title} - ${currentSong!!.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = if (isWidgetRunning)
                    "✨ Widget is active! Circle appears when music plays. Drag to move, tap to expand."
                else
                    "🚀 Start the widget to see a floating circle when Spotify plays music!",
                style = MaterialTheme.typography.bodyMedium
            )

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, FloatingWidgetService::class.java).apply {
                            action = FloatingWidgetService.ACTION_START
                        }
                        context.startForegroundService(intent)
                        isWidgetRunning = true
                    },
                    enabled = !isWidgetRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🚀 Start Widget")
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, FloatingWidgetService::class.java).apply {
                            action = FloatingWidgetService.ACTION_STOP
                        }
                        context.startService(intent)
                        isWidgetRunning = false
                    },
                    enabled = isWidgetRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🛑 Stop Widget")
                }
            }

            // Features info
            if (isWidgetRunning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "💡 How it works:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• Circle only appears when Spotify is playing\n• Tap circle to see song info and add comments\n• Drag circle to move it around\n• Auto-snaps to screen edges\n• Tap ✕ to close expanded panel",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}