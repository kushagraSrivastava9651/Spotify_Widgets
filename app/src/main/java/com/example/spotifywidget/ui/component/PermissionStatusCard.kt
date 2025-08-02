package com.example.spotifywidget.ui.component


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spotifywidget.utils.PermissionHelper


@Composable
fun PermissionStatusCard(
    modifier: Modifier = Modifier,
    onPermissionsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionHelper = remember { PermissionHelper(context) }

    // State to track permission status
    var canDrawOverlays by remember { mutableStateOf(permissionHelper.canDrawOverlays()) }
    var hasNotificationAccess by remember { mutableStateOf(permissionHelper.hasNotificationListenerPermission()) }

    // Function to refresh permission status
    val refreshPermissions = {
        canDrawOverlays = permissionHelper.canDrawOverlays()
        hasNotificationAccess = permissionHelper.hasNotificationListenerPermission()
        onPermissionsChanged()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📋 Permission Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )


            PermissionItem(
                title = "Draw Over Apps",
                description = "Allows floating widget to appear over Spotify",
                isGranted = canDrawOverlays,
                onRequestClick = {
                    permissionHelper.requestOverlayPermission()
                }
            )


            PermissionItem(
                title = "Notification Access",
                description = "Allows reading Spotify song information",
                isGranted = hasNotificationAccess,
                onRequestClick = {
                    permissionHelper.requestNotificationListenerPermission()
                }
            )

            Divider()


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (permissionHelper.allPermissionsGranted())
                        "🎉 All permissions granted!"
                    else
                        "⚠️ Permissions needed",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (permissionHelper.allPermissionsGranted())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = refreshPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("🔄 Refresh")
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGranted) "✅" else "❌",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        if (!isGranted) {
            Button(
                onClick = onRequestClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Grant")
            }
        }
    }
}