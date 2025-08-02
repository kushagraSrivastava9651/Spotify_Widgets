package com.example.spotifywidget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifywidget.R
import com.example.spotifywidget.data.database.SongComment
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView Adapter for displaying song comments in the floating widget
 */
class CommentAdapter : ListAdapter<SongComment, CommentAdapter.CommentViewHolder>(
    CommentDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val commentText: TextView = itemView.findViewById(R.id.commentText)
        private val commentTime: TextView = itemView.findViewById(R.id.commentTime)
        private val commentEmoji: TextView = itemView.findViewById(R.id.commentEmoji)
        private val commentRating: TextView = itemView.findViewById(R.id.commentRating)

        private val timeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

        fun bind(comment: SongComment) {
            commentText.text = comment.comment
            commentTime.text = timeFormat.format(comment.timestamp)

            // Handle emoji
            if (comment.emoji != null) {
                commentEmoji.text = comment.emoji
                commentEmoji.visibility = View.VISIBLE
            } else {
                commentEmoji.visibility = View.GONE
            }

            // Handle rating
            if (comment.rating != null) {
                commentRating.text = "⭐".repeat(comment.rating)
                commentRating.visibility = View.VISIBLE
            } else {
                commentRating.visibility = View.GONE
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<SongComment>() {
        override fun areItemsTheSame(oldItem: SongComment, newItem: SongComment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SongComment, newItem: SongComment): Boolean {
            return oldItem == newItem
        }
    }
}