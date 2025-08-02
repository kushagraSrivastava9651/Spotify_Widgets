package com.example.spotifywidget.ui.component


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spotifywidget.data.repository.CommentRepository
import com.example.spotifywidget.utils.SongInfoManager
import kotlinx.coroutines.launch

/**
 * Component for testing the Room database
 * Allows adding comments and viewing the count
 */
@Composable
fun DatabaseTestCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { CommentRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // State for the test comment input
    var testComment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var lastAddedCommentId by remember { mutableStateOf<Long?>(null) }
    var commentCount by remember { mutableIntStateOf(0) }

    // Get current song info
    val currentSong by SongInfoManager.currentSong.collectAsState()

    // Get all comments to show count
    val allComments by repository.getAllComments().collectAsStateWithLifecycle(initialValue = emptyList())

    // Update comment count when comments change
    LaunchedEffect(allComments) {
        commentCount = allComments.size
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🗄️ Database Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Database stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Comments: $commentCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (lastAddedCommentId != null) {
                    Text(
                        text = "✅ Last ID: $lastAddedCommentId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider()

            // Current song info for context
            if (currentSong != null) {
                Text(
                    text = "🎵 Current: ${currentSong!!.title} by ${currentSong!!.artist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "🎵 No song detected - will use test data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Comment input
            OutlinedTextField(
                value = testComment,
                onValueChange = { testComment = it },
                label = { Text("Test Comment") },
                placeholder = { Text("Enter a test comment...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                enabled = !isLoading,
                maxLines = 3
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add comment button
                Button(
                    onClick = {
                        if (testComment.isNotBlank()) {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val id = if (currentSong != null) {
                                        // Add comment for current song
                                        repository.addCommentForSong(
                                            title = currentSong!!.title,
                                            artist = currentSong!!.artist,
                                            commentText = testComment,
                                            emoji = "🧪" // Test emoji
                                        )
                                    } else {
                                        // Add test comment with dummy data
                                        repository.addComment(
                                            title = "Test Song",
                                            artist = "Test Artist",
                                            commentText = testComment
                                        )
                                    }
                                    lastAddedCommentId = id
                                    testComment = ""
                                } catch (e: Exception) {
                                    android.util.Log.e("DatabaseTest", "Error adding comment", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = testComment.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("💾 Add Comment")
                    }
                }

                // Clear all button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                repository.clearAllComments()
                                lastAddedCommentId = null
                            } catch (e: Exception) {
                                android.util.Log.e("DatabaseTest", "Error clearing comments", e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && commentCount > 0
                ) {
                    Text("🗑️ Clear")
                }
            }

            // Recent comments preview
            if (allComments.isNotEmpty()) {
                Divider()
                Text(
                    text = "📝 Recent Comments:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Show last 3 comments
                allComments.take(3).forEach { comment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "${comment.emoji ?: "💬"} ${comment.comment}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "♪ ${comment.songTitle} - ${comment.songArtist}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (allComments.size > 3) {
                    Text(
                        text = "... and ${allComments.size - 3} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}