package com.example.spotifywidget.data.repository

import android.content.Context
import android.util.Log
import com.example.spotifywidget.data.database.AppDatabase
import com.example.spotifywidget.data.database.SongComment
import com.example.spotifywidget.data.database.SongCommentDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Date

/**
 * Repository class that provides a clean API for data operations
 * This is the single source of truth for comment data
 */
class CommentRepository private constructor(context: Context) {

    private val database: AppDatabase
    private val songCommentDao: SongCommentDao

    init {
        try {
            database = AppDatabase.getDatabase(context)
            songCommentDao = database.songCommentDao()
            Log.d(TAG, "CommentRepository initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing CommentRepository", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "CommentRepository"

        // Singleton instance
        @Volatile
        private var INSTANCE: CommentRepository? = null

        fun getInstance(context: Context): CommentRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    CommentRepository(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating CommentRepository instance", e)
                    throw e
                }
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Data class to represent song info (avoiding circular dependency)
     * This is a simplified version for the repository layer
     */
    data class SongInfo(
        val title: String,
        val artist: String,
        val packageName: String = "com.spotify.music"
    )

    /**
     * Add a comment for the currently playing song
     */
    suspend fun addCommentForCurrentSong(
        songInfo: SongInfo,
        commentText: String,
        emoji: String? = null,
        rating: Int? = null
    ): Long {
        return try {
            val comment = SongComment(
                songTitle = songInfo.title,
                songArtist = songInfo.artist,
                comment = commentText,
                emoji = emoji,
                rating = rating,
                timestamp = Date(),
                packageName = songInfo.packageName
            )

            val id = songCommentDao.insertComment(comment)
            Log.d(TAG, "Comment added with ID: $id for song: ${songInfo.title}")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment for current song", e)
            -1L
        }
    }

    /**
     * Add a comment using song details directly (alternative method)
     */
    suspend fun addCommentForSong(
        title: String,
        artist: String,
        commentText: String,
        emoji: String? = null,
        rating: Int? = null
    ): Long {
        return try {
            val comment = SongComment(
                songTitle = title,
                songArtist = artist,
                comment = commentText,
                emoji = emoji,
                rating = rating,
                timestamp = Date()
            )

            val id = songCommentDao.insertComment(comment)
            Log.d(TAG, "Comment added with ID: $id for song: $title")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment for song: $title", e)
            -1L
        }
    }

    /**
     * Add a simple text comment
     */
    suspend fun addComment(
        title: String,
        artist: String,
        commentText: String
    ): Long {
        return try {
            val comment = SongComment(
                songTitle = title,
                songArtist = artist,
                comment = commentText,
                timestamp = Date()
            )

            val id = songCommentDao.insertComment(comment)
            Log.d(TAG, "Simple comment added with ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding simple comment", e)
            -1L
        }
    }

    /**
     * Get all comments (Flow for reactive UI)
     */
    fun getAllComments(): Flow<List<SongComment>> {
        return try {
            songCommentDao.getAllComments()
                .catch { e ->
                    Log.e(TAG, "Error getting all comments", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating getAllComments flow", e)
            flow { emit(emptyList<SongComment>()) }
        }
    }

    /**
     * Get comments for a specific song
     */
    fun getCommentsForSong(title: String, artist: String): Flow<List<SongComment>> {
        return try {
            songCommentDao.getCommentsForSong(title, artist)
                .catch { e ->
                    Log.e(TAG, "Error getting comments for song: $title", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating getCommentsForSong flow", e)
            flow { emit(emptyList<SongComment>()) }
        }
    }

    /**
     * Get the latest comment (for debugging)
     */
    suspend fun getLatestComment(): SongComment? {
        return try {
            songCommentDao.getLatestComment()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest comment", e)
            null
        }
    }

    /**
     * Delete a comment
     */
    suspend fun deleteComment(comment: SongComment) {
        try {
            songCommentDao.deleteComment(comment)
            Log.d(TAG, "Comment deleted: ${comment.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment: ${comment.id}", e)
        }
    }

    /**
     * Get total number of comments
     */
    suspend fun getCommentCount(): Int {
        return try {
            songCommentDao.getCommentCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comment count", e)
            0
        }
    }

    /**
     * Search comments
     */
    fun searchComments(searchText: String): Flow<List<SongComment>> {
        return try {
            songCommentDao.searchComments(searchText)
                .catch { e ->
                    Log.e(TAG, "Error searching comments", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating searchComments flow", e)
            flow { emit(emptyList<SongComment>()) }
        }
    }

    /**
     * Clear all comments (useful for testing)
     */
    suspend fun clearAllComments() {
        try {
            songCommentDao.deleteAllComments()
            Log.d(TAG, "All comments cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all comments", e)
        }
    }

    /**
     * Get comments with emojis only
     */
    fun getCommentsWithEmojis(): Flow<List<SongComment>> {
        return try {
            songCommentDao.getCommentsWithEmojis()
                .catch { e ->
                    Log.e(TAG, "Error getting comments with emojis", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating getCommentsWithEmojis flow", e)
            flow { emit(emptyList<SongComment>()) }
        }
    }

    /**
     * Get formatted comment text for display
     */

    suspend fun songHasComments(title: String, artist: String): Boolean {
        return try {
            songCommentDao.getCommentsForSong(title, artist).first().isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if song has comments", e)
            false
        }
    }
    /**
     * Check if a song has any comments
     */

    /**
     * Get comments for a song synchronously (for quick checks)
     */
    suspend fun getCommentsForSongSync(title: String, artist: String): List<SongComment> {
        return try {
            songCommentDao.getCommentsForSong(title, artist).first()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comments for song synchronously", e)
            emptyList()
        }
    }
    /**
     * Validate database connection
     */
    fun isDatabaseHealthy(): Boolean {
        return try {
            database.isOpen
        } catch (e: Exception) {
            Log.e(TAG, "Database health check failed", e)
            false
        }
    }
}