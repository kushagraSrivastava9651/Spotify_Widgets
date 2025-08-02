package com.example.spotifywidget.utils


import android.graphics.Bitmap
import android.util.Log
import com.example.spotifywidget.service.SpotifyNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class that handles song information state throughout the app
 * This acts as a bridge between the notification service and UI components
 */
object SongInfoManager {

    private const val TAG = "SongInfoManager"

    // Current song information
    private val _currentSong = MutableStateFlow<SpotifyNotificationListener.SongInfo?>(null)
    val currentSong: StateFlow<SpotifyNotificationListener.SongInfo?> = _currentSong.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    // History of detected songs (for later use)
    private val _songHistory = MutableStateFlow<List<SpotifyNotificationListener.SongInfo>>(emptyList())
    val songHistory: StateFlow<List<SpotifyNotificationListener.SongInfo>> = _songHistory.asStateFlow()

    init {
        // Set up the callback to receive song changes from the notification service
        SpotifyNotificationListener.onSongChanged = { songInfo ->
            Log.d(TAG, "New song received: ${songInfo.title} by ${songInfo.artist}")
            updateCurrentSong(songInfo)
        }
    }

    /**
     * Update the current song and add it to history
     */
    fun updateCurrentSong(songInfo: SpotifyNotificationListener.SongInfo) {
        _currentSong.value = songInfo
        _isPlaying.value = true

        // Add to history (avoid duplicates)
        val currentHistory = _songHistory.value.toMutableList()

        // Only add if it's different from the last song
        if (currentHistory.isEmpty() ||
            currentHistory.last().title != songInfo.title ||
            currentHistory.last().artist != songInfo.artist) {

            currentHistory.add(songInfo)

            // Keep only last 50 songs to avoid memory issues
            if (currentHistory.size > 50) {
                currentHistory.removeAt(0)
            }

            _songHistory.value = currentHistory
            Log.d(TAG, "Song added to history. Total songs: ${currentHistory.size}")
        }
    }
    fun clearCurrentSong() {
        _currentSong.value = null
        _isPlaying.value = false
    }
    /**
     * Get the current song info (nullable)
     */
    fun getCurrentSong(): SpotifyNotificationListener.SongInfo? {
        return _currentSong.value
    }

    /**
     * Check if we have any song information
     */
    fun hasSongInfo(): Boolean {
        return _currentSong.value != null
    }


    /**
     * Clear all song information (useful for testing)
     */
    fun clearSongInfo() {
        _currentSong.value = null
        _songHistory.value = emptyList()
        Log.d(TAG, "Song info cleared")
    }

    /**
     * Get formatted display text for current song
     */
    fun getCurrentSongDisplayText(): String {
        val song = _currentSong.value
        return if (song != null) {
            "🎵 ${song.title}\n👤 ${song.artist}"
        } else {
            "🎵 No song detected yet\n🎧 Play something on Spotify!"
        }
    }
}