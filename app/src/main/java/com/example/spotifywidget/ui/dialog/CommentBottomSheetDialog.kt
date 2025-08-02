package com.example.spotifywidget.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.spotifywidget.R
import com.example.spotifywidget.data.repository.CommentRepository
import com.example.spotifywidget.service.SpotifyNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fixed Comment Dialog that works from Service context
 */
class CommentBottomSheetDialog(
    context: Context,
    private val songInfo: SpotifyNotificationListener.SongInfo?,
    private val onCommentAdded: () -> Unit = {}
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    companion object {
        private const val TAG = "CommentBottomSheet"
    }

    private lateinit var commentRepository: CommentRepository
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // UI Elements
    private lateinit var dialogCard: CardView
    private lateinit var songTitleText: TextView
    private lateinit var songArtistText: TextView
    private lateinit var albumArtImage: ImageView
    private lateinit var commentEditText: EditText
    private lateinit var emojiButtonsLayout: LinearLayout
    private lateinit var ratingBar: RatingBar
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    private var selectedEmoji: String? = null
    private val emojiOptions = listOf("😍", "🔥", "💖", "🎵", "👌", "🙌", "😊", "🤩")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Creating comment dialog...")

            // Remove default dialog styling
            requestWindowFeature(Window.FEATURE_NO_TITLE)

            // Set the layout
            setContentView(R.layout.dialog_add_comment)

            // Configure window for overlay display
            configureWindow()

            // Initialize repository
            commentRepository = CommentRepository.getInstance(context)

            setupViews()
            setupClickListeners()
            populateSongInfo()
            setupEmojiButtons() // Call this after views are set up

            Log.d(TAG, "Comment dialog created successfully for song: ${songInfo?.title}")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating comment dialog", e)
            dismiss()
        }
    }

    private fun configureWindow() {
        window?.let { window ->
            try {
                // Set window type for overlay
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                } else {
                    @Suppress("DEPRECATION")
                    window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                }

                // Make background semi-transparent
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
                )
                window.setDimAmount(0.5f)

                // Set layout parameters
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )

                // Position at bottom
                window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)

                Log.d(TAG, "Window configured for overlay display")

            } catch (e: Exception) {
                Log.e(TAG, "Error configuring window", e)
            }
        }
    }

    private fun setupViews() {
        try {
            // Find views
            songTitleText = findViewById(R.id.dialogSongTitle)
            songArtistText = findViewById(R.id.dialogSongArtist)
            albumArtImage = findViewById(R.id.dialogAlbumArt)
            commentEditText = findViewById(R.id.commentEditText)
            emojiButtonsLayout = findViewById(R.id.emojiButtonsLayout)
            ratingBar = findViewById(R.id.ratingBar)
            submitButton = findViewById(R.id.submitButton)
            cancelButton = findViewById(R.id.cancelButton)

            Log.d(TAG, "Views setup successfully")
            Log.d(TAG, "EmojiButtonsLayout found: ${emojiButtonsLayout != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views", e)
        }
    }

    private fun setupEmojiButtons() {
        try {
            Log.d(TAG, "Setting up emoji buttons...")

            // Clear any existing buttons
            emojiButtonsLayout.removeAllViews()

            emojiOptions.forEachIndexed { index, emoji ->
                Log.d(TAG, "Creating button for emoji: $emoji")

                val button = Button(context).apply {
                    text = emoji
                    textSize = 24f

                    // Create layout params
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(12, 8, 12, 8)
                    }

                    // Set background to transparent initially
                    setBackgroundColor(Color.TRANSPARENT)

                    // Set padding
                    setPadding(20, 16, 20, 16)

                    // Set minimum width
                    minWidth = 80

                    setOnClickListener {
                        Log.d(TAG, "Emoji button clicked: $emoji")
                        selectEmoji(emoji, this)
                    }
                }

                emojiButtonsLayout.addView(button)
                Log.d(TAG, "Added emoji button: $emoji")
            }

            Log.d(TAG, "Emoji buttons setup complete. Total buttons: ${emojiButtonsLayout.childCount}")

            // Force a layout update
            emojiButtonsLayout.requestLayout()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up emoji buttons", e)

            // Fallback: Add a simple test button
            try {
                val testButton = Button(context).apply {
                    text = "😍"
                    textSize = 24f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                emojiButtonsLayout.addView(testButton)
                Log.d(TAG, "Added fallback test button")
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Even fallback button failed", fallbackError)
            }
        }
    }

    private fun selectEmoji(emoji: String, button: Button) {
        try {
            Log.d(TAG, "Selecting emoji: $emoji")

            // Reset all emoji buttons to transparent
            for (i in 0 until emojiButtonsLayout.childCount) {
                val child = emojiButtonsLayout.getChildAt(i) as? Button
                child?.setBackgroundColor(Color.TRANSPARENT)
            }

            // Toggle selection
            if (selectedEmoji == emoji) {
                // Deselect if clicking same emoji
                selectedEmoji = null
                button.setBackgroundColor(Color.TRANSPARENT)
                Log.d(TAG, "Deselected emoji: $emoji")
            } else {
                // Select new emoji
                selectedEmoji = emoji
                button.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
                Log.d(TAG, "Selected emoji: $emoji")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error selecting emoji", e)
        }
    }

    private fun setupClickListeners() {
        try {
            submitButton.setOnClickListener {
                Log.d(TAG, "Submit button clicked")
                submitComment()
            }

            cancelButton.setOnClickListener {
                Log.d(TAG, "Cancel button clicked")
                dismiss()
            }

            // Close dialog when clicking outside the card
            findViewById<View>(android.R.id.content).setOnClickListener {
                Log.d(TAG, "Clicked outside dialog")
                dismiss()
            }

            setCanceledOnTouchOutside(true)

            Log.d(TAG, "Click listeners setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners", e)
        }
    }

    private fun populateSongInfo() {
        try {
            if (songInfo != null) {
                songTitleText.text = songInfo.title
                songArtistText.text = songInfo.artist

                if (songInfo.albumArt != null) {
                    albumArtImage.setImageBitmap(songInfo.albumArt)
                } else {
                    albumArtImage.setImageResource(R.drawable.ic_music_note)
                }
                Log.d(TAG, "Populated song info: ${songInfo.title} - ${songInfo.artist}")
            } else {
                songTitleText.text = "Test Song"
                songArtistText.text = "Test Artist"
                albumArtImage.setImageResource(R.drawable.ic_music_note)
                Log.d(TAG, "No song info available, using test values")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error populating song info", e)
        }
    }

    private fun submitComment() {
        try {
            val commentText = commentEditText.text.toString().trim()

            if (commentText.isEmpty()) {
                Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show()
                return
            }

            // Use songInfo or create dummy data
            val title = songInfo?.title ?: "Test Song"
            val artist = songInfo?.artist ?: "Test Artist"

            // Disable submit button to prevent double submission
            submitButton.isEnabled = false
            submitButton.text = "Adding..."

            val rating = if (ratingBar.rating > 0) ratingBar.rating.toInt() else null

            Log.d(TAG, "Submitting comment: '$commentText' for song: $title")

            coroutineScope.launch {
                try {
                    val commentId = withContext(Dispatchers.IO) {
                        commentRepository.addCommentForSong(
                            title = title,
                            artist = artist,
                            commentText = commentText,
                            emoji = selectedEmoji,
                            rating = rating
                        )
                    }

                    Log.d(TAG, "Comment added successfully with ID: $commentId")

                    // Show success message
                    Toast.makeText(context, "Comment added! 🎵", Toast.LENGTH_SHORT).show()

                    // Notify that comment was added
                    onCommentAdded()

                    // Close dialog
                    dismiss()

                } catch (e: Exception) {
                    Log.e(TAG, "Error adding comment", e)
                    Toast.makeText(context, "Failed to add comment: ${e.message}", Toast.LENGTH_LONG).show()

                    // Re-enable submit button
                    submitButton.isEnabled = true
                    submitButton.text = "Add Comment"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in submitComment", e)
            Toast.makeText(context, "Error submitting comment", Toast.LENGTH_SHORT).show()
            submitButton.isEnabled = true
            submitButton.text = "Add Comment"
        }
    }

    override fun show() {
        try {
            Log.d(TAG, "Attempting to show dialog...")
            super.show()
            Log.d(TAG, "Dialog show() completed successfully")

            // Debug the emoji buttons after show
            emojiButtonsLayout.post {
                Log.d(TAG, "After show - emoji buttons count: ${emojiButtonsLayout.childCount}")
                for (i in 0 until emojiButtonsLayout.childCount) {
                    val child = emojiButtonsLayout.getChildAt(i)
                    Log.d(TAG, "Button $i: ${(child as? Button)?.text}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog", e)
            Toast.makeText(context, "Cannot open comment dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun dismiss() {
        try {
            super.dismiss()
            Log.d(TAG, "Dialog dismissed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing dialog", e)
        }
    }
}