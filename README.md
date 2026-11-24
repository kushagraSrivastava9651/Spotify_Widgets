<h1 align="center">Spotify Widget</h1>

<p align="center">A floating Android overlay that detects the song playing in Spotify and lets you save notes, star ratings and emoji reactions on it, stored locally with Room.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Room-Database-4285F4?style=flat-square" />
</p>

## Features

- **Song detection without the Spotify SDK**: a `NotificationListenerService` reads Spotify's media notification and gets the title, artist and album art.
- **Floating widget**: a foreground `FloatingWidgetService` draws a bubble over other apps (`SYSTEM_ALERT_WINDOW`), so you can comment without leaving Spotify.
- **Song journal**: tap the bubble to open a bottom sheet where you can write a note, give 1–5 stars and pick an emoji reaction.
- **Offline-first**: comments are saved in a Room `song_comments` table through a repository and coroutines.
- **Guided permissions**: Compose status cards show which permissions (overlay, notification access) are still missing and link to the right settings screen.

## How It Works

```
Spotify notification ──▶ SpotifyNotificationListener ──▶ SongInfoManager (current song state)
                                                              │
                         FloatingWidgetService (overlay) ◀────┘
                                   │ tap
                                   ▼
                         CommentBottomSheetDialog ──▶ CommentRepository ──▶ Room (SongCommentDao)
```

| Package | Contents |
| --- | --- |
| `service/` | `SpotifyNotificationListener`, `FloatingWidgetService` |
| `data/` | `SongComment` entity, `SongCommentDao`, `AppDatabase`, `CommentRepository` |
| `ui/component/` | `CurrentSongCard`, `PermissionStatusCard`, `FloatingWidgetControlCard` |
| `ui/dialog/` | `CommentBottomSheetDialog` |
| `utils/` | `PermissionHelper`, `SongInfoManager` |

## Getting Started

1. Open the project in Android Studio and run it on a device (Spotify must be installed).
2. In the app, grant **Display over other apps** and **Notification access**.
3. Play a song in Spotify, start the floating widget, and tap the bubble to add a note.

**Tech:** Kotlin · Jetpack Compose · Material 3 · Room · Coroutines · LiveData · Coil · Accompanist Permissions

> Not affiliated with Spotify. This app only reads the media notification that Spotify already shows.
