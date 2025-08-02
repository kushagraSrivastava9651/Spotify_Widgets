package com.example.spotifywidget.utils


import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

/**
 * Helper class to manage all app permissions
 * This handles both overlay permission and notification listener permission
 */
class PermissionHelper(private val context: Context) {

    /**
     * Check if the app can draw over other apps (overlay permission)
     * This is needed for our floating widget
     */
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }



    /**
     * Check if the app has notification listener permission
     * This is needed to read Spotify notifications
     */
    fun hasNotificationListenerPermission(): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(context.packageName)
    }

    /**
     * Open the overlay permission settings screen
     * User needs to manually enable this permission
     */
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Open the notification listener settings screen
     * User needs to manually enable our app in the list
     */
    fun requestNotificationListenerPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Check if all required permissions are granted
     * Returns true only if both permissions are available
     */
    fun allPermissionsGranted(): Boolean {
        return canDrawOverlays() && hasNotificationListenerPermission()
    }

    /**
     * Get a human-readable status of permissions
     * Useful for debugging and showing to user
     */
    fun getPermissionStatus(): String {
        val overlayStatus = if (canDrawOverlays()) "✅ Granted" else "❌ Not Granted"
        val notificationStatus = if (hasNotificationListenerPermission()) "✅ Granted" else "❌ Not Granted"

        return """
            Overlay Permission: $overlayStatus
            Notification Listener: $notificationStatus
            All Permissions: ${if (allPermissionsGranted()) "✅ Ready" else "❌ Incomplete"}
        """.trimIndent()
    }
}