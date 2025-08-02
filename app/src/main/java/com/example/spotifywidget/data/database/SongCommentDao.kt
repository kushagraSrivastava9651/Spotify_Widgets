package com.example.spotifywidget.data.database


import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface SongCommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: SongComment): Long



    @Query("SELECT * FROM song_comments ORDER BY timestamp DESC")
    fun getAllComments(): Flow<List<SongComment>>


    @Query("SELECT * FROM song_comments WHERE songTitle = :title AND songArtist = :artist ORDER BY timestamp DESC")
    fun getCommentsForSong(title: String, artist: String): Flow<List<SongComment>>


    @Query("SELECT * FROM song_comments ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestComment(): SongComment?


    @Delete
    suspend fun deleteComment(comment: SongComment)


    @Query("DELETE FROM song_comments WHERE songTitle = :title AND songArtist = :artist")
    suspend fun deleteCommentsForSong(title: String, artist: String)


    @Query("DELETE FROM song_comments")
    suspend fun deleteAllComments()


    @Query("SELECT COUNT(*) FROM song_comments")
    suspend fun getCommentCount(): Int


    @Query("SELECT * FROM song_comments WHERE comment LIKE '%' || :searchText || '%' ORDER BY timestamp DESC")
    fun searchComments(searchText: String): Flow<List<SongComment>>


    @Query("SELECT * FROM song_comments WHERE emoji IS NOT NULL ORDER BY timestamp DESC")
    fun getCommentsWithEmojis(): Flow<List<SongComment>>


    @Query("SELECT * FROM song_comments WHERE rating IS NOT NULL ORDER BY timestamp DESC")
    fun getCommentsWithRatings(): Flow<List<SongComment>>
}