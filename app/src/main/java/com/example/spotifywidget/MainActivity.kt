package com.example.spotifywidget

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spotifywidget.service.FloatingWidgetService
import com.example.spotifywidget.ui.component.PermissionStatusCard
import com.example.spotifywidget.ui.component.CurrentSongCard
import com.example.spotifywidget.ui.component.DatabaseTestCard
import com.example.spotifywidget.ui.component.FloatingWidgetControlCard
import com.example.spotifywidget.ui.theme.SpotifyWidgetTheme
import com.example.spotifywidget.utils.PermissionHelper


class MainActivity : ComponentActivity() {


     @RequiresApi(Build.VERSION_CODES.O)
     private fun startFloatingWidget() {
        val intent = Intent(this, FloatingWidgetService::class.java).apply {
            action = FloatingWidgetService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopFloatingWidget() {
        val intent = Intent(this, FloatingWidgetService::class.java).apply {
            action = FloatingWidgetService.ACTION_STOP
        }
        startService(intent)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpotifyWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(modifier: Modifier = Modifier)  {
    val context = LocalContext.current
    val permissionHelper = remember { PermissionHelper(context) }

    // Track permission status
    var allPermissionsGranted by remember {
        mutableStateOf(permissionHelper.allPermissionsGranted())
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎵 Spotify Widget",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Step 4: Room Database ✅",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Permission Status Card
            PermissionStatusCard(
                onPermissionsChanged = {
                    allPermissionsGranted = permissionHelper.allPermissionsGranted()
                }
            )

            // Current Song Card (only show if permissions are granted)
            if (allPermissionsGranted) {
                CurrentSongCard()

                // Database Test Card (NEW!)
                DatabaseTestCard()
                FloatingWidgetControlCard()
            }

            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (allPermissionsGranted) "🗄️ Database Testing" else "📖 Setup Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (allPermissionsGranted) {
                        Text(
                            text = """
                            ✅ Database is now set up! Here's what we added:
                            
                            📊 **Room Database Components:**
                            • SongComment entity (data structure)
                            • SongCommentDao (database operations)
                            • AppDatabase (database configuration)
                            • CommentRepository (data management)
                            
                            🧪 **Test the database:**
                            1️⃣ Use the "Database Test" card above
                            2️⃣ Add test comments and see them saved
                            3️⃣ Check Android Studio's Database Inspector
                            
                            🎯 **What's working:**
                            • Comments can be saved to Room database
                            • Data persists between app restarts
                            • Clean repository pattern for data access
                            
                            📱 **Next steps:**
                            • Step 5: Floating widget service
                            • Step 6: Widget UI with Compose
                            • Step 7: Connect everything together!
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = """
                            ⚠️ **Please grant all permissions first!**
                            
                            The database is set up, but you need permissions to test the song detection:
                            • Draw Over Apps: For the floating widget
                            • Notification Access: To detect Spotify songs
                            
                            Grant the permissions above to test the database with real song data!
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SpotifyWidgetTheme {
        MainScreen()
    }
}