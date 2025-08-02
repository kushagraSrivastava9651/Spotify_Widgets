package com.example.spotifywidget.service


import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.example.spotifywidget.utils.SongInfoManager

/**
 * Service that listens to all notifications and extracts Spotify song information
 * This runs in the background and detects when Spotify shows music notifications
 */
class SpotifyNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SpotifyListener"
        private const val SPOTIFY_PACKAGE = "com.spotify.music"

        // Callback to notify other parts of the app when a song changes
        var onSongChanged: ((SongInfo) -> Unit)? = null
    }

    /**
     * Data class to hold song information
     */
    data class SongInfo(
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,
        val isPlaying: Boolean = true,
        val packageName: String = SPOTIFY_PACKAGE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn?.packageName != SPOTIFY_PACKAGE) {
            return
        }

        Log.d(TAG, "Spotify notification detected!")

        try {
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: "Unknown Title"
            val text = extras.getCharSequence("android.text")?.toString() ?: "Unknown Artist"

            val albumArt = try {
                val largeIcon = notification.getLargeIcon()
                largeIcon?.loadDrawable(this)?.toBitmap()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract album art: ${e.message}")
                null
            }

            val songInfo = SongInfo(
                title = title,
                artist = text,
                albumArt = albumArt,
                isPlaying = true
            )

            Log.d(TAG, "Song detected: $title by $text")

            // Update the SongInfoManager
            SongInfoManager.updateCurrentSong(songInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing Spotify notification", e)
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        if (sbn?.packageName == SPOTIFY_PACKAGE) {
            Log.d(TAG, "Spotify notification removed - music stopped")
            SongInfoManager.clearCurrentSong()
        }
    }


    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener Service connected!")

        try {
            val activeNotifications = activeNotifications
            activeNotifications?.forEach { sbn ->
                if (sbn.packageName == SPOTIFY_PACKAGE) {
                    Log.d(TAG, "Processing existing Spotify notification")
                    onNotificationPosted(sbn)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing existing notifications", e)
        }
    }


    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification Listener Service disconnected!")
    }
}