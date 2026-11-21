package com.qwiksave.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.qwiksave.app.MainActivity
import com.qwiksave.app.R
import com.qwiksave.app.ui.main.SongTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android Foreground Service for Continuous Background Audio Playback
 * Allows music to play seamlessly when the app is minimized or phone screen is locked.
 */
class MusicPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSessionCompat? = null
    private var currentCoverBitmap: Bitmap? = null
    private var lastLoadedCoverUrl: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QwikSave:MusicWakeLock")
        wakeLock?.setReferenceCounted(false)

        mediaSession = MediaSessionCompat(this, "QwikMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { resumeMedia() }
                override fun onPause() { pauseMedia() }
                override fun onSkipToNext() { MusicController.nextTrack(this@MusicPlaybackService) }
                override fun onSkipToPrevious() { MusicController.prevTrack(this@MusicPlaybackService) }
                override fun onSeekTo(pos: Long) {
                    val sec = (pos / 1000).toInt()
                    mediaPlayer?.seekTo(pos.toInt())
                    MusicController._progressSec.value = sec
                }
                override fun onStop() { stopPlayback() }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val track = MusicController.currentTrack.value
                if (track != null) {
                    startForegroundWithNotification(track, isPlaying = true)
                    playMedia(track)
                }
            }
            ACTION_TOGGLE_PLAY_PAUSE -> {
                if (mediaPlayer?.isPlaying == true) {
                    pauseMedia()
                } else {
                    resumeMedia()
                }
            }
            ACTION_NEXT -> {
                MusicController.nextTrack(this)
            }
            ACTION_PREV -> {
                MusicController.prevTrack(this)
            }
            ACTION_STOP -> {
                stopPlayback()
            }
            ACTION_SEEK -> {
                val seekSec = intent.getIntExtra(EXTRA_SEEK_SEC, 0)
                mediaPlayer?.seekTo(seekSec * 1000)
                MusicController._progressSec.value = seekSec
            }
        }
        return START_NOT_STICKY
    }

    private fun playMedia(track: SongTrack) {
        try {
            currentCoverBitmap = null
            lastLoadedCoverUrl = null
            wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // Check local cached file first
                val cacheDir = File(cacheDir, "music_offline")
                val localFile = File(cacheDir, "${track.id}.mp3")
                if (localFile.exists() && localFile.length() > 1024) {
                    setDataSource(this@MusicPlaybackService, Uri.fromFile(localFile))
                } else {
                    setDataSource(track.streamUrl)
                }

                setOnPreparedListener { mp ->
                    mp.start()
                    MusicController._isPlaying.value = true
                    val dur = (mp.duration / 1000).coerceAtLeast(track.durationSec)
                    MusicController._durationSec.value = dur
                    startProgressTracker()
                    updateNotification(track, isPlaying = true)
                }

                setOnCompletionListener {
                    MusicController.nextTrack(this@MusicPlaybackService)
                }

                setOnErrorListener { _, _, _ ->
                    MusicController._isPlaying.value = false
                    stopProgressTracker()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            MusicController._isPlaying.value = false
        }
    }

    private fun pauseMedia() {
        mediaPlayer?.pause()
        MusicController._isPlaying.value = false
        stopProgressTracker()
        MusicController.currentTrack.value?.let { updateNotification(it, isPlaying = false) }
        wakeLock?.release()
    }

    private fun resumeMedia() {
        mediaPlayer?.start()
        MusicController._isPlaying.value = true
        startProgressTracker()
        MusicController.currentTrack.value?.let { updateNotification(it, isPlaying = true) }
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    private fun stopPlayback() {
        stopProgressTracker()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        MusicController._isPlaying.value = false
        MusicController._progressSec.value = 0
        wakeLock?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val posSec = (mediaPlayer?.currentPosition ?: 0) / 1000
                MusicController._progressSec.value = posSec
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QwikSave Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows now playing controls and notifications for QwikSave Music Hub"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun getDefaultArtBitmap(): Bitmap {
        val width = 256
        val height = 256
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Spotify Green to Deep Forest gradient background
        val shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(0xFF1DB954.toInt(), 0xFF064E3B.toInt(), 0xFF121212.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Vinyl grooves & glowing music aura
        paint.shader = null
        paint.color = 0xFF1DB954.toInt()
        paint.alpha = 140
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(width / 2f, height / 2f, 75f, paint)
        canvas.drawCircle(width / 2f, height / 2f, 50f, paint)

        // Center vinyl disc
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xFF18181B.toInt()
        paint.alpha = 230
        canvas.drawCircle(width / 2f, height / 2f, 30f, paint)

        paint.color = 0xFF1DB954.toInt()
        paint.alpha = 255
        canvas.drawCircle(width / 2f, height / 2f, 12f, paint)

        return bmp
    }

    private fun updateMediaSession(track: SongTrack, isPlaying: Boolean, artBitmap: Bitmap?) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val pos = (mediaPlayer?.currentPosition ?: 0).toLong()
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, pos, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.albumOrMovie)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (track.durationSec * 1000).toLong())

        if (track.coverUrl.isNotBlank()) {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, track.coverUrl)
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.coverUrl)
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, track.coverUrl)
        }

        val effectiveBitmap = artBitmap ?: getDefaultArtBitmap()
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, effectiveBitmap)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, effectiveBitmap)

        // Set official colored app logo for system media notification player source icon
        val appLogoBitmap = try {
            BitmapFactory.decodeResource(resources, R.drawable.app_logo)
        } catch (_: Exception) {
            effectiveBitmap
        }
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, appLogoBitmap ?: effectiveBitmap)

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun buildNotification(track: SongTrack, isPlaying: Boolean, artBitmap: Bitmap? = currentCoverBitmap): Notification {
        val effectiveBitmap = artBitmap ?: getDefaultArtBitmap()
        updateMediaSession(track, isPlaying, effectiveBitmap)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PAGE", "music_hub")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Toggle action
        val toggleIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Prev action
        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            this,
            2,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next action
        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { mediaStyle.setMediaSession(it) }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_qwiksave)
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.albumOrMovie}")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(0xFF1DB954.toInt()) // Vibrant Spotify Green
            .setColorized(true)
            .setLargeIcon(effectiveBitmap)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(mediaStyle)

        return builder.build()
    }

    private fun startForegroundWithNotification(track: SongTrack, isPlaying: Boolean) {
        val notification = buildNotification(track, isPlaying, currentCoverBitmap)
        startForeground(NOTIFICATION_ID, notification)
        loadCoverArtAsync(track)
    }

    private fun updateNotification(track: SongTrack, isPlaying: Boolean) {
        val notification = buildNotification(track, isPlaying, currentCoverBitmap)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
        loadCoverArtAsync(track)
    }

    private fun loadCoverArtAsync(track: SongTrack) {
        if (track.coverUrl.isBlank()) return
        if (track.coverUrl == lastLoadedCoverUrl && currentCoverBitmap != null) return

        serviceScope.launch(Dispatchers.IO) {
            try {
                val conn = (URL(track.coverUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    connectTimeout = 6000
                    readTimeout = 8000
                }
                if (conn.responseCode == 200) {
                    val rawBmp = BitmapFactory.decodeStream(conn.inputStream)
                    if (rawBmp != null) {
                        // Scale to 280x280 to comfortably fit IPC binder limits and guarantee crisp rendering
                        val scaledBmp = Bitmap.createScaledBitmap(rawBmp, 280, 280, true)
                        currentCoverBitmap = scaledBmp
                        lastLoadedCoverUrl = track.coverUrl
                        withContext(Dispatchers.Main) {
                            val notifWithArt = buildNotification(track, MusicController.isPlaying.value, scaledBmp)
                            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            nm.notify(NOTIFICATION_ID, notifWithArt)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        stopPlayback()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "qwik_music_channel"
        const val NOTIFICATION_ID = 4099

        const val ACTION_PLAY = "com.qwiksave.app.action.PLAY"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.qwiksave.app.action.TOGGLE"
        const val ACTION_NEXT = "com.qwiksave.app.action.NEXT"
        const val ACTION_PREV = "com.qwiksave.app.action.PREV"
        const val ACTION_STOP = "com.qwiksave.app.action.STOP"
        const val ACTION_SEEK = "com.qwiksave.app.action.SEEK"
        const val EXTRA_SEEK_SEC = "extra_seek_sec"
    }
}

/**
 * Global Singleton Music Controller
 * Accessible anywhere in Jetpack Compose UI to observe state and send commands to the service.
 */
object MusicController {
    val _currentTrack = MutableStateFlow<SongTrack?>(null)
    val currentTrack: StateFlow<SongTrack?> = _currentTrack.asStateFlow()

    val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val _progressSec = MutableStateFlow(0)
    val progressSec: StateFlow<Int> = _progressSec.asStateFlow()

    val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    val _playlist = MutableStateFlow<List<SongTrack>>(emptyList())
    val playlist: StateFlow<List<SongTrack>> = _playlist.asStateFlow()

    fun playTrack(context: Context, track: SongTrack, list: List<SongTrack> = emptyList()) {
        _currentTrack.value = track
        if (list.isNotEmpty()) {
            _playlist.value = list
        }
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PLAY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun togglePlayPause(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_TOGGLE_PLAY_PAUSE
        }
        context.startService(intent)
    }

    fun nextTrack(context: Context) {
        val current = _currentTrack.value ?: return
        val list = _playlist.value
        if (list.isNotEmpty()) {
            val idx = list.indexOfFirst { it.id == current.id }
            val nextIdx = if (idx >= 0 && idx + 1 < list.size) idx + 1 else 0
            playTrack(context, list[nextIdx], list)
        }
    }

    fun prevTrack(context: Context) {
        val current = _currentTrack.value ?: return
        val list = _playlist.value
        if (list.isNotEmpty()) {
            val idx = list.indexOfFirst { it.id == current.id }
            val prevIdx = if (idx > 0) idx - 1 else list.size - 1
            playTrack(context, list[prevIdx], list)
        }
    }

    fun seekTo(context: Context, seconds: Int) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SEEK
            putExtra(MusicPlaybackService.EXTRA_SEEK_SEC, seconds)
        }
        context.startService(intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_STOP
        }
        context.startService(intent)
    }
}
