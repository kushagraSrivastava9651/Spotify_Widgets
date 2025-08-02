package com.example.spotifywidget.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifywidget.MainActivity
import com.example.spotifywidget.R
import com.example.spotifywidget.data.repository.CommentRepository
import com.example.spotifywidget.adapter.CommentAdapter
import com.example.spotifywidget.ui.dialog.CommentBottomSheetDialog
import com.example.spotifywidget.utils.SongInfoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * FIXED FloatingWidgetService with proper click detection
 */
class FloatingWidgetService : Service() {

    companion object {
        private const val TAG = "FloatingWidgetService"
        private const val NOTIFICATION_ID = 100
        private const val CHANNEL_ID = "floating_widget_channel"

        const val ACTION_START = "com.example.spotifywidget.START_FLOATING_WIDGET"
        const val ACTION_STOP = "com.example.spotifywidget.STOP_FLOATING_WIDGET"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // UI Elements
    private var circleContainer: CardView? = null
    private var albumArt: ImageView? = null
    private var playIndicator: ImageView? = null
    private var expandedPanel: CardView? = null
    private var songTitle: TextView? = null
    private var songArtist: TextView? = null
    private var largeAlbumArt: ImageView? = null
    private var closeButton: ImageView? = null
    private var addCommentButton: Button? = null
    private var commentsRecyclerView: RecyclerView? = null
    private var noCommentsText: TextView? = null

    // Comment functionality
    private lateinit var commentRepository: CommentRepository
    private lateinit var commentAdapter: CommentAdapter
    private var currentSongInfo: SpotifyNotificationListener.SongInfo? = null

    private var isPanelExpanded = false
    private var isServiceRunning = false

    // FIXED: Touch handling variables
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val dragThreshold = 20f // Reduced threshold
    private var clickStartTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingWidgetService created")

        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Initialize comment repository
        commentRepository = CommentRepository.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Start floating widget requested")
                if (!isServiceRunning) {
                    startForegroundServiceProperly()
                    createFloatingWidget()
                    observeSongChanges()
                    isServiceRunning = true

                    // DEBUG: Test after creation
                    floatingView?.postDelayed({
                        debugWidgetState()
                        // Set test data for debugging
                        setTestSongData()
                    }, 1000)
                }
            }

            ACTION_STOP -> {
                Log.d(TAG, "Stop floating widget requested")
                stopWidget()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Music Widget",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating music widget over other apps"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceProperly() {
        try {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎵 Music Widget Active")
                .setContentText("Tap the floating circle to add comments!")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(
                    R.drawable.ic_close,
                    "Stop Widget",
                    PendingIntent.getService(
                        this,
                        0,
                        Intent(this, FloatingWidgetService::class.java).apply {
                            action = ACTION_STOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "Foreground service started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            stopSelf()
        }
    }

    private fun createFloatingWidget() {
        try {
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget_circle, null)

            // Get UI references
            circleContainer = floatingView?.findViewById(R.id.circleContainer)
            albumArt = floatingView?.findViewById(R.id.albumArt)
            playIndicator = floatingView?.findViewById(R.id.playIndicator)
            expandedPanel = floatingView?.findViewById(R.id.expandedPanel)
            songTitle = floatingView?.findViewById(R.id.songTitle)
            songArtist = floatingView?.findViewById(R.id.songArtist)
            largeAlbumArt = floatingView?.findViewById(R.id.largeAlbumArt)
            closeButton = floatingView?.findViewById(R.id.closeButton)
            addCommentButton = floatingView?.findViewById(R.id.addCommentButton)
            commentsRecyclerView = floatingView?.findViewById(R.id.commentsRecyclerView)
            noCommentsText = floatingView?.findViewById(R.id.noCommentsText)

            Log.d(TAG, "UI references obtained successfully")

            setupRecyclerView()
            setupClickListeners()
            setupTouchListener() // FIXED: New method name
            showFloatingWidget()

            Log.d(TAG, "Floating widget created successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating widget", e)
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter()
        commentsRecyclerView?.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(this@FloatingWidgetService)
        }
        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")

        // Close button - collapse panel only
        closeButton?.setOnClickListener {
            Log.d(TAG, "Close button clicked")
            collapsePanel()
        }

        // Add comment button - show comment dialog
        addCommentButton?.setOnClickListener {
            Log.d(TAG, "Add comment button clicked")
            showCommentDialog()
        }

        Log.d(TAG, "Click listeners setup complete")
    }

    // COMPLETELY REWRITTEN: Fixed touch handling
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        Log.d(TAG, "Setting up FIXED touch listener")

        circleContainer?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "🔴 ACTION_DOWN detected")

                    val params = floatingView?.layoutParams as WindowManager.LayoutParams
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    clickStartTime = System.currentTimeMillis()

                    Log.d(TAG, "Touch down at: (${event.rawX}, ${event.rawY})")
                    true // Consume the event
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)

                    if (!isDragging && (deltaX > dragThreshold || deltaY > dragThreshold)) {
                        isDragging = true
                        Log.d(TAG, "🟡 DRAG STARTED - Movement: ($deltaX, $deltaY)")

                        // Close panel when dragging starts
                        if (isPanelExpanded) {
                            collapsePanel()
                        }
                    }

                    if (isDragging) {
                        val params = floatingView?.layoutParams as WindowManager.LayoutParams
                        params.x = initialX + (initialTouchX - event.rawX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()

                        // Keep within screen bounds
                        val displayMetrics = resources.displayMetrics
                        params.x = params.x.coerceIn(-50, displayMetrics.widthPixels - 70)
                        params.y = params.y.coerceIn(0, displayMetrics.heightPixels - 120)

                        windowManager?.updateViewLayout(floatingView, params)
                    }

                    true // Consume the event
                }

                MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - clickStartTime
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)

                    Log.d(TAG, "🟢 ACTION_UP - Duration: ${touchDuration}ms, Movement: ($deltaX, $deltaY)")

                    if (isDragging) {
                        Log.d(TAG, "Finishing drag operation")
                        snapToEdge()
                        isDragging = false
                    } else {
                        // This was a click!
                        if (touchDuration < 300 && deltaX < dragThreshold && deltaY < dragThreshold) {
                            Log.d(TAG, "🎯 VALID CLICK DETECTED!")

                            // Immediate feedback
                            Toast.makeText(
                                this@FloatingWidgetService,
                                "Circle clicked! Opening...",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Handle the click
                            handleCircleClick()
                        } else {
                            Log.d(TAG, "Invalid click - too long or too much movement")
                        }
                    }

                    true // Consume the event
                }

                MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "🔴 ACTION_CANCEL")
                    isDragging = false
                    true
                }

                else -> false
            }
        }

        // Make sure the circle container is properly configured
        circleContainer?.apply {
            isClickable = true
            isFocusable = true
            Log.d(TAG, "Circle container configured as clickable")
        }

        Log.d(TAG, "Touch listener setup complete")
    }

    // NEW: Separated click handling logic
    private fun handleCircleClick() {
        Log.d(TAG, "=== HANDLING CIRCLE CLICK ===")

        if (isPanelExpanded) {
            Log.d(TAG, "Panel is expanded - showing comment dialog directly")
            showCommentDialog()
        } else {
            Log.d(TAG, "Panel is collapsed - expanding panel")
            expandPanel()
        }
    }

    private fun showCommentDialog() {
        Log.d(TAG, "=== SHOWING COMMENT DIALOG ===")

        try {
            // Create song info if none exists (for testing)
            val songToUse = currentSongInfo ?: SpotifyNotificationListener.SongInfo(
                title = "Test Song",
                artist = "Test Artist",
                albumArt = null
            )

            Log.d(TAG, "Creating dialog for song: ${songToUse.title}")

            val dialog = CommentBottomSheetDialog(
                context = this@FloatingWidgetService,
                songInfo = songToUse,
                onCommentAdded = {
                    Log.d(TAG, "Comment added successfully!")
                    loadCommentsForCurrentSong()
                    Toast.makeText(
                        this@FloatingWidgetService,
                        "Comment saved! 🎵",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            Log.d(TAG, "Calling dialog.show()...")
            dialog.show()

            // Verify dialog is showing
            floatingView?.postDelayed({
                Log.d(TAG, "Dialog status check: ${if (dialog.isShowing) "SHOWING ✅" else "NOT SHOWING ❌"}")
            }, 100)

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR showing comment dialog", e)
            Toast.makeText(this, "Error opening dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun expandPanel() {
        try {
            Log.d(TAG, "=== EXPANDING PANEL ===")

            expandedPanel?.visibility = View.VISIBLE
            isPanelExpanded = true

            updateExpandedPanelContent()
            loadCommentsForCurrentSong()
            updateWindowLayout()

            Log.d(TAG, "Panel expanded successfully")

            // Auto-show comment dialog after panel expansion
            floatingView?.postDelayed({
                Log.d(TAG, "Auto-showing comment dialog after panel expansion")
                showCommentDialog()
            }, 200)

        } catch (e: Exception) {
            Log.e(TAG, "Error expanding panel", e)
        }
    }

    private fun collapsePanel() {
        try {
            Log.d(TAG, "Collapsing panel")
            expandedPanel?.visibility = View.GONE
            isPanelExpanded = false
            updateWindowLayout()
            Log.d(TAG, "Panel collapsed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error collapsing panel", e)
        }
    }

    private fun updateWindowLayout() {
        try {
            val params = floatingView?.layoutParams as? WindowManager.LayoutParams ?: return

            // Keep window focusable for touch events
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT

            windowManager?.updateViewLayout(floatingView, params)
            Log.d(TAG, "Window layout updated - expanded: $isPanelExpanded")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout", e)
        }
    }

    private fun updateExpandedPanelContent() {
        try {
            currentSongInfo?.let { songInfo ->
                songTitle?.text = songInfo.title
                songArtist?.text = songInfo.artist

                if (songInfo.albumArt != null) {
                    largeAlbumArt?.setImageBitmap(songInfo.albumArt)
                } else {
                    largeAlbumArt?.setImageResource(R.drawable.ic_music_note)
                }
                Log.d(TAG, "Updated panel content for: ${songInfo.title}")
            } ?: run {
                songTitle?.text = "No song playing"
                songArtist?.text = "Tap to add comment"
                largeAlbumArt?.setImageResource(R.drawable.ic_music_note)
                Log.d(TAG, "Updated panel content with default values")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating expanded panel content", e)
        }
    }

    private fun loadCommentsForCurrentSong() {
        currentSongInfo?.let { songInfo ->
            serviceScope.launch {
                try {
                    commentRepository.getCommentsForSong(songInfo.title, songInfo.artist)
                        .collect { comments ->
                            commentAdapter.submitList(comments)

                            if (comments.isNotEmpty()) {
                                commentsRecyclerView?.visibility = View.VISIBLE
                                noCommentsText?.visibility = View.GONE
                            } else {
                                commentsRecyclerView?.visibility = View.GONE
                                noCommentsText?.visibility = View.VISIBLE
                            }

                            Log.d(TAG, "Loaded ${comments.size} comments for: ${songInfo.title}")
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading comments", e)
                }
            }
        }
    }

    // FIXED: Proper window creation
    private fun showFloatingWidget() {
        if (floatingView == null || windowManager == null) {
            Log.e(TAG, "Cannot show widget - view or window manager is null")
            return
        }

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // CRITICAL: Proper flags for touch handling
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 50
                y = 200
            }

            windowManager?.addView(floatingView, params)
            Log.d(TAG, "✅ Floating widget shown with proper touch handling")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating widget", e)
        }
    }

    private fun snapToEdge() {
        val params = floatingView?.layoutParams as? WindowManager.LayoutParams ?: return
        val displayMetrics = resources.displayMetrics

        val screenCenterX = displayMetrics.widthPixels / 2
        val currentX = displayMetrics.widthPixels - params.x

        params.x = if (currentX > screenCenterX) {
            50 // Right edge
        } else {
            displayMetrics.widthPixels - 110 // Left edge
        }

        windowManager?.updateViewLayout(floatingView, params)
        Log.d(TAG, "Snapped to edge at x: ${params.x}")
    }

    private fun observeSongChanges() {
        serviceScope.launch {
            SongInfoManager.currentSong.collect { songInfo ->
                Log.d(TAG, "Song changed: ${songInfo?.title}")
                currentSongInfo = songInfo
                updateCircleUI(songInfo)

                if (isPanelExpanded) {
                    updateExpandedPanelContent()
                    loadCommentsForCurrentSong()
                }
            }
        }

        serviceScope.launch {
            SongInfoManager.isPlaying.collect { isPlaying ->
                updatePlayIndicator(isPlaying)
            }
        }
    }

    private fun updateCircleUI(songInfo: SpotifyNotificationListener.SongInfo?) {
        try {
            // Always show circle
            circleContainer?.visibility = View.VISIBLE

            if (songInfo != null) {
                if (songInfo.albumArt != null) {
                    albumArt?.setImageBitmap(songInfo.albumArt)
                } else {
                    albumArt?.setImageResource(R.drawable.ic_music_note)
                }
                Log.d(TAG, "Circle updated with song: ${songInfo.title}")
            } else {
                albumArt?.setImageResource(R.drawable.ic_music_note)
                Log.d(TAG, "Circle updated with default icon")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating circle UI", e)
        }
    }

    private fun updatePlayIndicator(isPlaying: Boolean) {
        try {
            playIndicator?.setImageResource(
                if (isPlaying) R.drawable.baseline_play_arrow_24
                else R.drawable.baseline_motion_photos_pause_24
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating play indicator", e)
        }
    }

    // NEW: Set test data for debugging
    private fun setTestSongData() {
        Log.d(TAG, "Setting test song data for debugging")

        currentSongInfo = SpotifyNotificationListener.SongInfo(
            title = "Test Song",
            artist = "Test Artist",
            albumArt = null,
            isPlaying = true
        )

        updateCircleUI(currentSongInfo)

        Toast.makeText(
            this,
            "Test mode: Click the circle to test comment dialog!",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun debugWidgetState() {
        Log.d(TAG, "=== WIDGET DEBUG STATE ===")
        Log.d(TAG, "floatingView: ${floatingView != null}")
        Log.d(TAG, "circleContainer: ${circleContainer != null}")
        Log.d(TAG, "expandedPanel: ${expandedPanel != null}")
        Log.d(TAG, "isPanelExpanded: $isPanelExpanded")
        Log.d(TAG, "currentSongInfo: ${currentSongInfo?.title}")

        circleContainer?.let {
            Log.d(TAG, "Circle - Visibility: ${it.visibility}, Clickable: ${it.isClickable}")
        }
    }

    private fun stopWidget() {
        try {
            hideFloatingWidget()
            stopForeground(true)
            serviceScope.cancel()
            isServiceRunning = false
            stopSelf()
            Log.d(TAG, "Widget stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping widget", e)
        }
    }

    private fun hideFloatingWidget() {
        try {
            if (floatingView != null && windowManager != null) {
                windowManager?.removeView(floatingView)
                Log.d(TAG, "Floating widget hidden")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating widget", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingWidgetService destroyed")
        hideFloatingWidget()
        serviceScope.cancel()
    }
}