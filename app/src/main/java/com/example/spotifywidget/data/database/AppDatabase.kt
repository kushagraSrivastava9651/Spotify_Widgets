package com.example.spotifywidget.data.database


import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import java.util.Date

/**
 * Type converters for Room database
 * Room can't store Date objects directly, so we convert them to Long (timestamp)
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * Main Room database class
 * This is the central database configuration
 */
@Database(
    entities = [SongComment::class],
    version = 1,
    exportSchema = false // Set to true in production for schema versioning
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Abstract function to get the DAO
    abstract fun songCommentDao(): SongCommentDao

    companion object {
        // Singleton pattern - only one database instance
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance (singleton)
         * Thread-safe creation of database
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spotify_widget_database"
                )
                    .fallbackToDestructiveMigration() // For development - recreates DB on schema changes
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Close the database (useful for testing)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

    /**
     * Callback to populate database with sample data (optional)
     * Uncomment if you want to add some test data
     */
    /*
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // You could add some initial data here if needed
        }
    }
    */
}