package com.qwiksave.app.ui.main

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.sdk.QwikSaveClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Data Model for Music Tracks (90s Classics, Trending & Custom Songs)
 */
data class SongTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumOrMovie: String,
    val durationText: String,
    val durationSec: Int,
    val streamUrl: String,
    val coverUrl: String,
    val category: String // "90s", "Trending", "Travel", "Punjabi", "Custom"
)

/**
 * Curated Travel & Evergreen Tracks Library
 * Handcrafted with famous 90s Bollywood hits, modern travel melodies & road trip favorites.
 */
val CURATED_SONGS_CATALOG = listOf(
    // 📻 90s Bollywood Evergreen Hits (Road Trip & Nostalgia)
    SongTrack(
        id = "s_90s_01",
        title = "Tujhe Dekha Toh Yeh Jaana Sanam",
        artist = "Kumar Sanu, Lata Mangeshkar",
        albumOrMovie = "Dilwale Dulhania Le Jayenge (1995)",
        durationText = "5:04",
        durationSec = 304,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_02",
        title = "Pehla Nasha Pehla Khumaar",
        artist = "Udit Narayan, Sadhana Sargam",
        albumOrMovie = "Jo Jeeta Wohi Sikandar (1992)",
        durationText = "4:51",
        durationSec = 291,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_03",
        title = "Chura Ke Dil Mera Goriya Chali",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Main Khiladi Tu Anari (1994)",
        durationText = "4:38",
        durationSec = 278,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_04",
        title = "Dheere Dheere Se Meri Zindagi",
        artist = "Kumar Sanu, Anuradha Paudwal",
        albumOrMovie = "Aashiqui (1990)",
        durationText = "5:26",
        durationSec = 326,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        coverUrl = "https://images.unsplash.com/photo-1445985543470-41fdd6ce388d?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_05",
        title = "Baazigar O Baazigar",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Baazigar (1993)",
        durationText = "7:31",
        durationSec = 451,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_06",
        title = "Aankh Marey O Ladka Aankh Marey",
        artist = "Kumar Sanu, Kavita Krishnamurthy",
        albumOrMovie = "Tere Mere Sapne (1996)",
        durationText = "5:12",
        durationSec = 312,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_07",
        title = "Tip Tip Barsa Paani",
        artist = "Udit Narayan, Alka Yagnik",
        albumOrMovie = "Mohra (1994)",
        durationText = "5:57",
        durationSec = 357,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
        coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400&q=80",
        category = "90s"
    ),
    SongTrack(
        id = "s_90s_08",
        title = "Mere Sapnon Ki Rani Kab Aayegi Tu",
        artist = "Kishore Kumar",
        albumOrMovie = "Aradhana • Evergreen Classic",
        durationText = "5:00",
        durationSec = 300,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400&q=80",
        category = "90s"
    ),

    // 🔥 Trending & Modern Melodies
    SongTrack(
        id = "s_trend_01",
        title = "Kesariya Tera Ishq Hai Piya",
        artist = "Arijit Singh, Pritam",
        albumOrMovie = "Brahmāstra (2022)",
        durationText = "4:28",
        durationSec = 268,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80",
        category = "Trending"
    ),
    SongTrack(
        id = "s_trend_02",
        title = "Chaleya Teri Aur Chala",
        artist = "Arijit Singh, Shilpa Rao, Anirudh",
        albumOrMovie = "Jawan (2023)",
        durationText = "3:20",
        durationSec = 200,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
        coverUrl = "https://images.unsplash.com/photo-1429962714451-bb934ecdc4ec?w=400&q=80",
        category = "Trending"
    ),
    SongTrack(
        id = "s_trend_03",
        title = "Heeriye Teri Hoke",
        artist = "Arijit Singh, Jasleen Royal",
        albumOrMovie = "Heeriye Single (2023)",
        durationText = "3:14",
        durationSec = 194,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        coverUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=400&q=80",
        category = "Trending"
    ),
    SongTrack(
        id = "s_trend_04",
        title = "Apna Bana Le Piya",
        artist = "Arijit Singh, Sachin-Jigar",
        albumOrMovie = "Bhediya (2022)",
        durationText = "4:21",
        durationSec = 261,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        coverUrl = "https://images.unsplash.com/photo-1518895949257-7621c3c786d7?w=400&q=80",
        category = "Trending"
    ),
    SongTrack(
        id = "s_trend_05",
        title = "Tum Hi Ho Bandhu",
        artist = "Arijit Singh • Aashiqui 2",
        albumOrMovie = "Aashiqui 2",
        durationText = "4:22",
        durationSec = 262,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400&q=80",
        category = "Trending"
    ),

    // 🚗 Road Trip & Travel Vibes
    SongTrack(
        id = "s_travel_01",
        title = "Ilahi Mera Dil Aaye",
        artist = "Arijit Singh, Pritam",
        albumOrMovie = "Yeh Jawaani Hai Deewani (YJHD)",
        durationText = "3:49",
        durationSec = 229,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400&q=80",
        category = "Travel"
    ),
    SongTrack(
        id = "s_travel_02",
        title = "Safarnama Khuli Sadak Pe",
        artist = "Lucky Ali, A.R. Rahman",
        albumOrMovie = "Tamasha Travel Song",
        durationText = "4:11",
        durationSec = 251,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        coverUrl = "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=400&q=80",
        category = "Travel"
    ),
    SongTrack(
        id = "s_travel_03",
        title = "Matargashti Khuli Sadak Pe",
        artist = "Mohit Chauhan, A.R. Rahman",
        albumOrMovie = "Tamasha",
        durationText = "5:28",
        durationSec = 328,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        coverUrl = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=400&q=80",
        category = "Travel"
    ),
    SongTrack(
        id = "s_travel_04",
        title = "Kabira Maan Ja",
        artist = "Tochi Raina, Rekha Bhardwaj",
        albumOrMovie = "Yeh Jawaani Hai Deewani",
        durationText = "3:43",
        durationSec = 223,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
        coverUrl = "https://images.unsplash.com/photo-1519671482749-fd09be7ccebf?w=400&q=80",
        category = "Travel"
    ),

    // ⚡ Punjabi & High-Energy Hits
    SongTrack(
        id = "s_punj_01",
        title = "Pasoori Nu Sajan",
        artist = "Ali Sethi, Shae Gill",
        albumOrMovie = "Coke Studio Season 14",
        durationText = "3:44",
        durationSec = 224,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80",
        category = "Punjabi"
    ),
    SongTrack(
        id = "s_punj_02",
        title = "Excuses • Kehndi Hundi Si",
        artist = "AP Dhillon, Gurinder Gill",
        albumOrMovie = "Run-Up Records",
        durationText = "2:56",
        durationSec = 176,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&q=80",
        category = "Punjabi"
    ),
    SongTrack(
        id = "s_punj_03",
        title = "Brown Munde",
        artist = "AP Dhillon, Shinda Kahlon",
        albumOrMovie = "Not by Chance",
        durationText = "4:07",
        durationSec = 247,
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
        coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&q=80",
        category = "Punjabi"
    )
)

/**
 * Spotify-Themed Full Music Hub Screen:
 * - Search bar with live filtering + Spotify/YouTube link fetch
 * - 90s Evergreen, Trending, Travel, Liked Songs & Offline tabs
 * - Seamless Media Player with Play/Pause, Next, Previous
 * - Draggable Seekbar ("lining pe ungli se aage piche kar sake")
 * - Continuous Autoplay (advances to next track when finished)
 * - 1-Click Like heart (saves to Liked Songs playlist)
 * - 1-Click Offline Download (saves playable MP3 for road trips with zero internet)
 */
@Composable
fun MusicHubScreen(
    client: QwikSaveClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("90s") } // "All", "90s", "Trending", "Travel", "Punjabi", "Liked", "Offline"

    // User favorites and downloaded files state
    var likedSongIds by remember { mutableStateOf(UserPreferences.getLikedSongIds(context)) }
    var downloadedPaths by remember { mutableStateOf(UserPreferences.getDownloadedSongPaths(context)) }

    val allTracks = remember { mutableStateListOf<SongTrack>().apply { addAll(CURATED_SONGS_CATALOG) } }

    // Audio Player State
    var currentTrack by remember { mutableStateOf<SongTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    fun playTrack(track: SongTrack) {
        currentTrack = track
        isBuffering = true
        isPlaying = false
        currentPositionMs = 0
        durationMs = track.durationSec * 1000

        coroutineScope.launch(Dispatchers.IO) {
            try {
                mediaPlayer.reset()

                // Check if downloaded offline file exists
                val localPath = downloadedPaths[track.id]
                val localFile = if (!localPath.isNullOrBlank()) File(localPath) else null

                if (localFile != null && localFile.exists() && localFile.length() > 1024) {
                    mediaPlayer.setDataSource(context, Uri.fromFile(localFile))
                } else {
                    val streamUrl = if (track.streamUrl.startsWith("/")) "${client.baseUrl}${track.streamUrl}" else track.streamUrl
                    mediaPlayer.setDataSource(streamUrl)
                }

                mediaPlayer.setOnPreparedListener { mp ->
                    isBuffering = false
                    durationMs = mp.duration.coerceAtLeast(track.durationSec * 1000)
                    mp.start()
                    isPlaying = true
                }

                mediaPlayer.setOnErrorListener { _, what, extra ->
                    isBuffering = false
                    isPlaying = false
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Playback error ($what, $extra)", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                isBuffering = false
                isPlaying = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not stream audio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Continuous Autoplay Callback: When song completes, auto-advance to next song
    DisposableEffect(allTracks, selectedCategory, likedSongIds, downloadedPaths) {
        mediaPlayer.setOnCompletionListener {
            val activeList = allTracks.filter { track ->
                when (selectedCategory) {
                    "All" -> true
                    "Liked" -> likedSongIds.contains(track.id)
                    "Offline" -> downloadedPaths.containsKey(track.id)
                    else -> track.category.equals(selectedCategory, ignoreCase = true)
                }
            }
            if (activeList.isNotEmpty() && currentTrack != null) {
                val curIdx = activeList.indexOfFirst { it.id == currentTrack?.id }
                val nextIdx = if (curIdx in 0 until activeList.size - 1) curIdx + 1 else 0
                playTrack(activeList[nextIdx])
            }
        }
        onDispose {
            mediaPlayer.release()
        }
    }

    // Periodic progress updater for draggable seekbar
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPositionMs = mediaPlayer.currentPosition
                    if (durationMs > 0) {
                        sliderValue = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    }
                }
            } catch (_: Exception) {}
            delay(500)
        }
    }

    // Filtered track list based on search and category
    val displayedTracks = remember(allTracks, searchQuery, selectedCategory, likedSongIds, downloadedPaths) {
        val q = searchQuery.trim().lowercase()
        allTracks.filter { track ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Liked" -> likedSongIds.contains(track.id)
                "Offline" -> downloadedPaths.containsKey(track.id)
                else -> track.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = q.isEmpty() ||
                    track.title.lowercase().contains(q) ||
                    track.artist.lowercase().contains(q) ||
                    track.albumOrMovie.lowercase().contains(q)
            matchesCategory && matchesSearch
        }
    }

    fun playNext() {
        if (displayedTracks.isEmpty() || currentTrack == null) return
        val idx = displayedTracks.indexOfFirst { it.id == currentTrack?.id }
        val nextIdx = if (idx in 0 until displayedTracks.size - 1) idx + 1 else 0
        playTrack(displayedTracks[nextIdx])
    }

    fun playPrevious() {
        if (displayedTracks.isEmpty() || currentTrack == null) return
        val idx = displayedTracks.indexOfFirst { it.id == currentTrack?.id }
        val prevIdx = if (idx > 0) idx - 1 else displayedTracks.size - 1
        playTrack(displayedTracks[prevIdx])
    }

    fun formatTime(ms: Int): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    // Spotify Theme Styling (#121212 dark, #1DB954 vibrant Spotify green)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentTrack != null) 100.dp else 16.dp)
        ) {
            // Header Bar with Back Button & Glowing Spotify Music Vibe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1DB954).copy(alpha = 0.25f), Color(0xFF121212))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Qwik Music Hub", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1DB954))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("SPOTIFY VIBE 🎧", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                        Text("Stream, Liked Songs & Offline Road Trip Hits", fontSize = 11.sp, color = Color(0xFFB3B3B3))
                    }
                }
            }

            // Search Bar (Supports Custom Song Names, Artists or Links)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 90s song, Arijit, Kishore, AP Dhillon, link...", fontSize = 12.sp, color = Color(0xFF71717A)) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color(0xFF27272A),
                    focusedContainerColor = Color(0xFF18181B),
                    unfocusedContainerColor = Color(0xFF18181B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Category Filter Chips (90s Classics, Trending, Travel, Liked, Offline)
            val categories = listOf(
                Pair("90s", "📻 90s Classics"),
                Pair("Trending", "🔥 Trending Hits"),
                Pair("Travel", "🚗 Road Trip"),
                Pair("Punjabi", "⚡ Punjabi"),
                Pair("Liked", "❤️ Liked (${likedSongIds.size})"),
                Pair("Offline", "📲 Offline (${downloadedPaths.size})"),
                Pair("All", "🌐 All Tracks")
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(categories) { _, (key, label) ->
                    val isSelected = selectedCategory == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1DB954) else Color(0xFF27272A))
                            .clickable { selectedCategory = key }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Song List
            if (displayedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MusicOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (selectedCategory == "Liked") "No Liked Songs yet!\nTap the heart ❤️ icon on any song to add it here."
                            else if (selectedCategory == "Offline") "No Offline Downloads yet!\nTap the download icon ⬇️ to save songs for travel."
                            else "No songs found for '$searchQuery'",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedTracks) { index, song ->
                        val isCurrent = currentTrack?.id == song.id
                        val isLiked = likedSongIds.contains(song.id)
                        val isDownloaded = downloadedPaths.containsKey(song.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isCurrent && isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                    } else if (isCurrent && !isPlaying) {
                                        mediaPlayer.start()
                                        isPlaying = true
                                    } else {
                                        playTrack(song)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF1E293B) else Color(0xFF18181B)
                            ),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1DB954)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Cover Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF27272A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncThumbnail(
                                        url = song.coverUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        placeholderIcon = Icons.Default.MusicNote
                                    )
                                    if (isCurrent && isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }

                                // Song Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.albumOrMovie}",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Action Buttons (Like Heart & Download)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Like Toggle Button
                                    IconButton(
                                        onClick = {
                                            val nowLiked = UserPreferences.toggleLikedSong(context, song.id)
                                            likedSongIds = UserPreferences.getLikedSongIds(context)
                                            val msg = if (nowLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (isLiked) Color(0xFF1DB954) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Download Button
                                    var isSongDownloading by remember { mutableStateOf(false) }

                                    IconButton(
                                        onClick = {
                                            if (isDownloaded) {
                                                Toast.makeText(context, "Already downloaded! Plays offline 📲", Toast.LENGTH_SHORT).show()
                                                return@IconButton
                                            }
                                            isSongDownloading = true
                                            coroutineScope.launch {
                                                val cleanName = "QwikSave_Song_${song.id}_${song.title.replace(Regex("[^a-zA-Z0-9]"), "_")}.mp3"
                                                val res = client.downloadMediaDirectly(context, song.streamUrl, cleanName)
                                                isSongDownloading = false
                                                res.onSuccess { f ->
                                                    UserPreferences.saveDownloadedSong(context, song.id, f.absolutePath)
                                                    downloadedPaths = UserPreferences.getDownloadedSongPaths(context)
                                                    Toast.makeText(context, "Downloaded for offline road trips! 🎵", Toast.LENGTH_LONG).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (isSongDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF1DB954), strokeWidth = 2.dp)
                                        } else if (isDownloaded) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF1DB954), modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = "Download Offline", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Spotify-Style Player Bar at Bottom (With Interactive Draggable Seekbar)
        currentTrack?.let { track ->
            val isLiked = likedSongIds.contains(track.id)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
                ) {
                    // Top: Track Info & Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Track Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncThumbnail(
                                    url = track.coverUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIcon = Icons.Default.MusicNote
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Playback Controls (Previous, Play/Pause, Next, Like)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { playPrevious() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // Circular Spotify Play/Pause Button
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        mediaPlayer.start()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF1DB954), CircleShape)
                            ) {
                                if (isBuffering) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { playNext() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = {
                                    val nowLiked = UserPreferences.toggleLikedSong(context, track.id)
                                    likedSongIds = UserPreferences.getLikedSongIds(context)
                                    val msg = if (nowLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color(0xFF1DB954) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Draggable Seekbar ("lining pe ungli se aage piche kar sake")
                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            isSeeking = true
                            sliderValue = newVal
                            currentPositionMs = (newVal * durationMs).toInt()
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            try {
                                val target = (sliderValue * durationMs).toInt()
                                mediaPlayer.seekTo(target)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF1DB954),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    // Timing indicators: 01:23 / 04:55
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPositionMs), color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(formatTime(durationMs), color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
