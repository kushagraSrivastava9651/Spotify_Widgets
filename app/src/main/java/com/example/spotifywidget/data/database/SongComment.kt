package com.example.spotifywidget.data.database


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a comment on a song
 * This defines the structure of our database table
 */
@Entity(tableName = "song_comments")
data class SongComment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Song information
    val songTitle: String,
    val songArtist: String,
    val albumArt: String? = null, // We'll store as base64 string or file path

    // Comment data
    val comment: String,
    val timestamp: Date = Date(),

    // Optional: Rating or emoji reaction
    val rating: Int? = null, // 1-5 stars
    val emoji: String? = null, // Emoji reaction like 😍, 🔥, 💖

    // Metadata
    val packageName: String = "com.spotify.music"
)