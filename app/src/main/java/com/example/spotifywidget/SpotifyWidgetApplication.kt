package com.example.spotifywidget


import android.app.Application

/**
 * Application class for our Spotify Widget app
 * This is the entry point of our application
 */
class SpotifyWidgetApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // App initialization code will go here
        // For now, we just log that the app started
        android.util.Log.d("SpotifyWidget", "App started successfully!")
    }
}


