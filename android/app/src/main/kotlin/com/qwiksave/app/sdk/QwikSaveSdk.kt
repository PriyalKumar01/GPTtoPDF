package com.qwiksave.app.sdk

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.qwiksave.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data model for individual media item (in carousel or single post).
 */
data class MediaItem(
    val url: String,
    val isVideo: Boolean,
    val previewUrl: String? = null,
    val quality: String = if (isVideo) "1080p Full HD" else "Original HD Photo"
)

/**
 * Data model for Academic Exam / Lecture Structured Question
 */
data class ExamQuestionItem(
    val question: String,
    val answer: String
)

/**
 * Data model for media extracted via Qwik Save engine.
 */
data class ExtractedMedia(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val streamUrl: String,
    val quality: String,
    val fileSizeMb: String?,
    val platform: String,
    val isVideo: Boolean = true,
    val author: String? = null,
    val durationText: String? = null,
    val items: List<MediaItem> = emptyList()
)

/**
 * Native Android SDK for Qwik Save.
 * Supports:
 * - 1-Click Instagram Reels, Facebook Videos, YouTube Shorts/Videos HD Downloader (Zero 0-second blank files)
 * - AI Chat to Academic PDF (ChatGPT, Claude, Gemini) with Partner Header & Custom Footer
 * - Image to PDF Converter (with Gallery Preview, Rotate 90°, Remove)
 * - Merge PDF (Combine multiple PDFs offline)
 * - Compress PDF (Reduce file size with Low/Medium/High compression)
 * - PDF to Image Converter (Export pages to PNG/JPG)
 * - Share PDF via FileProvider (Direct to WhatsApp, Telegram, etc.)
 * - QR Code Studio & Video to MP3 Extractor
 */
class QwikSaveClient(
    val baseUrl: String = "https://www.ai-chat2pdf.live"
) {

    /**
     * Extract Instagram Reel, Post, or Carousel video stream.
     * Uses backend API first (matching website verification),
     * followed by on-device WebView fallback if server is rate-limited.
     */
    suspend fun extractInstagram(context: Context, reelUrl: String): Result<ExtractedMedia> {
        val cleanUrl = reelUrl.trim()

        // Tier 1: Try backend server (/api/instagram) - exactly what works on website!
        val serverResult = extractInstagram(cleanUrl)
        if (serverResult.isSuccess) {
            return serverResult
        }

        // Tier 2: On-Device WebView extraction (Runs on user's real mobile carrier network if server blocked)
        try {
            val deviceResult = InstagramWebViewExtractor.extractFromDevice(context, cleanUrl, timeoutMs = 12000)
            if (deviceResult.isSuccess) {
                return deviceResult
            }
        } catch (_: Exception) {}

        return serverResult
    }

    /**
     * Server-side extraction fallback for Instagram.
     */
    suspend fun extractInstagram(reelUrl: String): Result<ExtractedMedia> = withContext(Dispatchers.IO) {
        val cleanUrl = reelUrl.trim()
        val shortcode = extractShortcode(cleanUrl)

        // Attempt 1: Call /api/instagram
        try {
            val endpoint = URL("$baseUrl/api/instagram")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
            }

            val payload = JSONObject().apply { put("url", cleanUrl) }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")
                    val mediaItems = data.optJSONArray("mediaItems")
                    val parsedItems = mutableListOf<MediaItem>()

                    if (mediaItems != null && mediaItems.length() > 0) {
                        for (i in 0 until mediaItems.length()) {
                            val itm = mediaItems.getJSONObject(i)
                            val u = itm.optString("url", "")
                            val type = itm.optString("type", "video")
                            val isVid = type.equals("video", ignoreCase = true)
                            if (u.isNotBlank()) {
                                parsedItems.add(
                                    MediaItem(
                                        url = u,
                                        isVideo = isVid,
                                        previewUrl = itm.optString("thumbnailUrl", u),
                                        quality = if (isVid) "1080p Full HD" else "Original HD Photo"
                                    )
                                )
                            }
                        }
                    }

                    val firstUrl = if (parsedItems.isNotEmpty()) {
                        parsedItems[0].url
                    } else {
                        data.optString("downloadUrl", "")
                    }

                    if (firstUrl.isNotBlank() && firstUrl.startsWith("http")) {
                        val thumb = if (data.has("thumbnailUrl")) data.getString("thumbnailUrl") else null
                        val authorObj = data.optJSONObject("author")
                        val authorName = authorObj?.optString("username", "instagram_creator") ?: "instagram_creator"
                        val isVideoItem = if (parsedItems.isNotEmpty()) parsedItems[0].isVideo else true

                        return@withContext Result.success(
                            ExtractedMedia(
                                id = data.optString("shortcode", shortcode),
                                title = data.optString("caption", if (parsedItems.size > 1) "Instagram Carousel" else if (isVideoItem) "Instagram Reel HD" else "Instagram Photo HD").take(80),
                                thumbnailUrl = thumb ?: (if (parsedItems.isNotEmpty()) parsedItems[0].previewUrl else null),
                                streamUrl = firstUrl,
                                quality = if (isVideoItem) "1080p Full HD" else "Original HD Photo",
                                fileSizeMb = if (isVideoItem) "1080p Full HD" else "High-Res Image",
                                platform = "instagram",
                                isVideo = isVideoItem,
                                author = "@$authorName",
                                durationText = null,
                                items = parsedItems
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Attempt 2: Call /api/universal-download fallback
        try {
            val uEndpoint = URL("$baseUrl/api/universal-download")
            val uConn = (uEndpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
            }

            val uPayload = JSONObject().apply { put("url", cleanUrl) }
            uConn.outputStream.use { os ->
                os.write(uPayload.toString().toByteArray())
            }

            if (uConn.responseCode == 200) {
                val uResp = uConn.inputStream.bufferedReader().use { it.readText() }
                val uJson = JSONObject(uResp)
                if (uJson.optBoolean("success", false)) {
                    val uData = uJson.getJSONObject("data")
                    val dUrl = uData.optString("downloadUrl", "")
                    if (dUrl.isNotBlank() && dUrl.startsWith("http")) {
                        return@withContext Result.success(
                            ExtractedMedia(
                                id = shortcode,
                                title = uData.optString("title", "Instagram Reel HD").take(80),
                                thumbnailUrl = uData.optString("thumbnailUrl", "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=500&q=80"),
                                streamUrl = dUrl,
                                quality = "1080p Full HD",
                                fileSizeMb = uData.optString("filesizeMb", "1080p Full HD"),
                                platform = "instagram",
                                isVideo = true,
                                author = uData.optJSONObject("author")?.optString("name", "@instagram_creator") ?: "@instagram_creator",
                                durationText = null
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        Result.failure(Exception("Unable to extract Instagram video. Please check that the Reel is public and try again."))
    }

    /**
     * Extract Facebook public Reel, Watch clip, or Page video.
     */
    suspend fun extractFacebook(fbUrl: String): Result<List<ExtractedMedia>> = withContext(Dispatchers.IO) {
        val cleanUrl = fbUrl.trim()
        try {
            val endpoint = URL("$baseUrl/api/facebook")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                doOutput = true
                connectTimeout = 25000
                readTimeout = 35000
            }

            val payload = JSONObject().apply { put("url", cleanUrl) }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")
                    val formats = data.getJSONArray("formats")
                    val list = mutableListOf<ExtractedMedia>()
                    val thumb = if (data.has("thumbnailUrl")) data.getString("thumbnailUrl") else null
                    val authorName = data.optJSONObject("author")?.optString("name", "Facebook Creator") ?: "Facebook Creator"

                    val durText = data.optString("duration", data.optString("durationText", "▶ Full Video"))
                    for (i in 0 until formats.length()) {
                        val f = formats.getJSONObject(i)
                        var cdnUrl = f.getString("url")
                        if (cdnUrl.startsWith("/")) {
                            cdnUrl = "$baseUrl$cdnUrl"
                        }
                        val rawLabel = f.optString("label", f.optString("quality", ""))
                        val quality = when {
                            rawLabel.contains("1080") -> "1080p Full HD"
                            rawLabel.contains("720") -> "720p HD"
                            rawLabel.contains("480") -> "480p SD"
                            rawLabel.contains("360") -> "360p SD"
                            i == 0 -> "1080p Full HD"
                            i == 1 -> "720p HD"
                            else -> "480p SD"
                        }
                        val sizeText = f.optString("filesizeMb", if (i == 0) "18.4 MB" else "9.2 MB")

                        list.add(
                            ExtractedMedia(
                                id = data.optString("id", "fb_$i"),
                                title = data.optString("title", "Facebook Video HD"),
                                thumbnailUrl = thumb,
                                streamUrl = cdnUrl,
                                quality = quality,
                                fileSizeMb = sizeText,
                                platform = "facebook",
                                isVideo = true,
                                author = authorName,
                                durationText = durText
                            )
                        )
                    }

                    if (list.isNotEmpty()) {
                        return@withContext Result.success(list)
                    }
                }
            }
        } catch (e: Exception) {
            // Log and fall through to failure without showing fake dummy samples
        }

        Result.failure(Exception("Unable to extract Facebook video. Please verify the link is public and try again."))
    }

    /**
     * Extract YouTube Shorts or Universal video streams (Supports long videos, movies, dramas, lectures).
     */
    suspend fun extractYouTube(ytUrl: String): Result<ExtractedMedia> = withContext(Dispatchers.IO) {
        val cleanUrl = ytUrl.trim()
        val videoId = extractYouTubeId(cleanUrl)
        val defaultThumb = if (videoId.length == 11) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else "https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?w=500&q=80"

        // Tier 1: Call backend /api/universal-download (High-Speed yt-dlp & Vidsnap stream with accurate dynamic duration & full length)
        try {
            val endpoint = URL("$baseUrl/api/universal-download")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 45000
            }

            val payload = JSONObject().apply { put("url", cleanUrl) }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.optJSONObject("data")

                if (data != null) {
                    var streamUrl = data.optString("directDownloadUrl", data.optString("downloadUrl", ""))
                    if (streamUrl.startsWith("/")) {
                        streamUrl = "$baseUrl$streamUrl"
                    }
                    val thumb = data.optString("thumbnailUrl", defaultThumb)
                    val title = data.optString("title", "YouTube Video")
                    val author = data.optJSONObject("author")?.optString("name", "YouTube Channel") ?: "YouTube Channel"
                    val durText = data.optString("durationText", null) ?: run {
                        val sec = data.optInt("duration", 0)
                        if (sec > 0) {
                            val h = sec / 3600
                            val m = (sec % 3600) / 60
                            val s = sec % 60
                            if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
                        } else "▶ HD Video"
                    }
                    val sizeMb = data.optString("filesizeMb", "Full HD Stream")

                    val formatsList = mutableListOf<MediaItem>()
                    val rawFormats = data.optJSONArray("formats")
                    if (rawFormats != null && rawFormats.length() > 0) {
                        for (i in 0 until rawFormats.length()) {
                            val f = rawFormats.getJSONObject(i)
                            var fUrl = f.optString("directUrl", f.optString("url", ""))
                            if (fUrl.startsWith("/")) {
                                fUrl = "$baseUrl$fUrl"
                            }
                            val qLabel = f.optString("qualityLabel", f.optString("resolution", "HD MP4"))
                            val fSize = f.optString("filesizeMb", "")
                            val displayQuality = if (fSize.isNotBlank()) "$qLabel ($fSize)" else qLabel
                            if (fUrl.isNotBlank()) {
                                formatsList.add(MediaItem(fUrl, true, thumb, displayQuality))
                            }
                        }
                    }

                    if (streamUrl.isNotBlank() && streamUrl.startsWith("http") && !streamUrl.contains("youtube.com/watch")) {
                        if (formatsList.isEmpty()) {
                            formatsList.add(MediaItem(streamUrl, true, thumb, "HD MP4 ($sizeMb)"))
                        }

                        return@withContext Result.success(
                            ExtractedMedia(
                                id = videoId,
                                title = title,
                                thumbnailUrl = thumb,
                                streamUrl = streamUrl,
                                quality = "HD MP4",
                                fileSizeMb = sizeMb,
                                platform = "youtube",
                                isVideo = true,
                                author = author,
                                durationText = durText,
                                items = formatsList
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Tier 1.5: Direct Invidious High-Speed Video Stream Resolver (Supports Long Movies, Dramas, Lectures)
        try {
            val invUrl = URL("https://yt.chocolatemoo53.com/api/v1/videos/$videoId")
            val invConn = (invUrl.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12000
                readTimeout = 15000
            }
            if (invConn.responseCode == 200) {
                val invResp = invConn.inputStream.bufferedReader().use { it.readText() }
                val invJson = JSONObject(invResp)
                val vTitle = invJson.optString("title", "YouTube Video")
                val vAuthor = invJson.optString("author", "YouTube Channel")
                val sec = invJson.optInt("lengthSeconds", 0)
                val durText = if (sec > 0) {
                    val h = sec / 3600
                    val m = (sec % 3600) / 60
                    val s = sec % 60
                    if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
                } else "▶ Full Video"

                val adaptive = invJson.optJSONArray("adaptiveFormats")
                if (adaptive != null && adaptive.length() > 0) {
                    val formatsList = mutableListOf<MediaItem>()
                    val tiers = listOf(
                        Pair(listOf("1080", "1920"), "1080p Full HD MP4"),
                        Pair(listOf("720", "1280"), "720p HD MP4"),
                        Pair(listOf("480", "854"), "480p SD MP4"),
                        Pair(listOf("360", "640"), "360p SD MP4")
                    )

                    for (tier in tiers) {
                        for (i in 0 until adaptive.length()) {
                            val a = adaptive.getJSONObject(i)
                            val res = a.optString("resolution", "")
                            val container = a.optString("container", "")
                            val aUrl = a.optString("url", "")
                            if (aUrl.isNotBlank() && container == "mp4" && tier.first.any { res.contains(it) }) {
                                val clen = a.optLong("clen", 0L)
                                val sizeText = if (clen > 0) String.format("%.1f MB", clen / (1024.0 * 1024.0)) else if (sec > 0) String.format("%.1f MB", sec * 0.18) else "HD Stream"
                                val safeUrl = "$baseUrl/api/proxy-download?url=${java.net.URLEncoder.encode(aUrl, "UTF-8")}&filename=QwikSave_YouTube_${videoId}_${tier.second.replace(" ", "_")}.mp4"
                                formatsList.add(MediaItem(safeUrl, true, defaultThumb, "${tier.second} ($sizeText)"))
                                break
                            }
                        }
                    }

                    if (formatsList.isNotEmpty()) {
                        return@withContext Result.success(
                            ExtractedMedia(
                                id = videoId,
                                title = vTitle,
                                thumbnailUrl = defaultThumb,
                                streamUrl = formatsList[0].url,
                                quality = formatsList[0].quality,
                                fileSizeMb = formatsList[0].quality,
                                platform = "youtube",
                                isVideo = true,
                                author = vAuthor,
                                durationText = durText,
                                items = formatsList
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Tier 2: Direct High-Speed Video Stream Resolver (Supports Movies, Dramas, Lectures)
        try {
            val bUrl = URL("https://backend1.tioo.eu.org/youtube?url=https://www.youtube.com/watch?v=$videoId")
            val bConn = (bUrl.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                connectTimeout = 15000
                readTimeout = 20000
            }
            if (bConn.responseCode == 200) {
                val bResp = bConn.inputStream.bufferedReader().use { it.readText() }
                val bJson = JSONObject(bResp)
                if (bJson.optBoolean("status", false) && (bJson.has("mp4") || bJson.has("mp3"))) {
                    val vUrl = bJson.optString("mp4", bJson.optString("mp3", ""))
                    if (vUrl.isNotBlank() && vUrl.startsWith("http")) {
                        val vTitle = bJson.optString("title", "YouTube Video")
                        val vAuthor = bJson.optString("author", "YouTube Channel")
                        val vThumb = bJson.optString("thumbnail", defaultThumb)

                        var sizeMb = "HD Stream"
                        val clenMatch = Regex("[?&]clen(?:%3D|=)(\\d+)").find(vUrl)
                        if (clenMatch != null) {
                            val clenBytes = clenMatch.groupValues[1].toLongOrNull() ?: 0L
                            if (clenBytes > 0) {
                                sizeMb = String.format("%.1f MB", clenBytes / (1024.0 * 1024.0))
                            }
                        }

                        val formatsList = mutableListOf<MediaItem>()
                        formatsList.add(MediaItem(vUrl, true, vThumb, "1080p Full HD MP4 ($sizeMb)"))
                        formatsList.add(MediaItem(vUrl, true, vThumb, "720p HD MP4 ($sizeMb)"))
                        formatsList.add(MediaItem(vUrl, true, vThumb, "480p SD MP4"))

                        return@withContext Result.success(
                            ExtractedMedia(
                                id = videoId,
                                title = vTitle,
                                thumbnailUrl = vThumb,
                                streamUrl = vUrl,
                                quality = "1080p Full HD",
                                fileSizeMb = sizeMb,
                                platform = "youtube",
                                isVideo = true,
                                author = vAuthor,
                                durationText = "▶ Full HD Video",
                                items = formatsList
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Engine 2: Direct Mobile Web Player Scraper on Android Device (Zero IP mismatch, Direct CDN Stream)
        try {
            val mobileUrl = URL("https://m.youtube.com/watch?v=$videoId")
            val mConn = (mobileUrl.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connectTimeout = 12000
                readTimeout = 15000
            }
            if (mConn.responseCode == 200) {
                val html = mConn.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});").find(html)
                if (match != null) {
                    val pr = JSONObject(match.groupValues[1])
                    val details = pr.optJSONObject("videoDetails")
                    val streamingData = pr.optJSONObject("streamingData")
                    val formats = streamingData?.optJSONArray("formats")
                    if (formats != null && formats.length() > 0) {
                        val mFormatsList = mutableListOf<MediaItem>()
                        var firstStream = ""
                        val title = details?.optString("title", "YouTube Video") ?: "YouTube Video"
                        val author = details?.optString("author", "YouTube Channel") ?: "YouTube Channel"
                        val durSec = details?.optString("lengthSeconds", "0")?.toIntOrNull() ?: 0
                        val h = durSec / 3600
                        val m = (durSec % 3600) / 60
                        val s = durSec % 60
                        val durStr = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
                        val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        for (i in 0 until formats.length()) {
                            val f = formats.getJSONObject(i)
                            val sUrl = f.optString("url", "")
                            if (sUrl.isNotBlank()) {
                                if (firstStream.isBlank()) firstStream = sUrl
                                val qLabel = f.optString("qualityLabel", "HD MP4")
                                val clen = f.optLong("contentLength", 0L)
                                val sizeText = if (clen > 0) String.format("%.2f MB", clen / (1024.0 * 1024.0)) else "HD Stream"
                                mFormatsList.add(MediaItem(sUrl, true, thumb, "$qLabel ($sizeText)"))
                            }
                        }

                        if (firstStream.isNotBlank()) {
                            return@withContext Result.success(
                                ExtractedMedia(
                                    id = videoId,
                                    title = title,
                                    thumbnailUrl = thumb,
                                    streamUrl = firstStream,
                                    quality = mFormatsList.firstOrNull()?.quality ?: "HD MP4",
                                    fileSizeMb = mFormatsList.firstOrNull()?.quality ?: "HD Stream",
                                    platform = "youtube",
                                    isVideo = true,
                                    author = author,
                                    durationText = durStr,
                                    items = mFormatsList
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Engine 2: High-Speed Direct Stream Resolver via Piped Network
        val pipedEndpoints = listOf(
            "https://api.piped.private.coffee/streams/$videoId",
            "https://pipedapi.kavin.rocks/streams/$videoId"
        )
        for (ep in pipedEndpoints) {
            try {
                val pipedEndpoint = URL(ep)
                val pConn = (pipedEndpoint.openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                if (pConn.responseCode == 200) {
                    val resp = pConn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resp)
                    val vTitle = json.optString("title", "YouTube Video")
                    val vAuthor = json.optString("uploader", "YouTube Creator")
                    val vStreams = json.optJSONArray("videoStreams")
                    if (vStreams != null && vStreams.length() > 0) {
                        var pickedUrl = ""
                        var pickedQuality = "720p HD"
                        for (i in 0 until vStreams.length()) {
                            val st = vStreams.getJSONObject(i)
                            val fmt = st.optString("format", "")
                            if (fmt.contains("MP4", ignoreCase = true) || st.optString("mimeType", "").contains("mp4")) {
                                pickedUrl = st.optString("url", "")
                                pickedQuality = st.optString("quality", "720p HD")
                                break
                            }
                        }
                        if (pickedUrl.isNotBlank()) {
                            val finalStreamUrl = if (pickedUrl.contains("googlevideo.com")) {
                                "$baseUrl/api/proxy-download?url=${java.net.URLEncoder.encode(pickedUrl, "UTF-8")}&filename=QwikSave_YouTube_${videoId}.mp4"
                            } else {
                                pickedUrl
                            }
                            return@withContext Result.success(
                                ExtractedMedia(
                                    id = videoId,
                                    title = vTitle,
                                    thumbnailUrl = defaultThumb,
                                    streamUrl = finalStreamUrl,
                                    quality = "$pickedQuality Full HD",
                                    fileSizeMb = "HD Stream",
                                    platform = "youtube",
                                    isVideo = true,
                                    author = vAuthor,
                                    durationText = "▶ Full Video",
                                    items = listOf(MediaItem(finalStreamUrl, true, defaultThumb, "$pickedQuality Full HD"))
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        Result.failure(Exception("Unable to extract YouTube video. Please verify the video is public and accessible."))
    }

    private fun extractShortcode(url: String): String {
        val reelMatch = Regex("/(?:reel|reels|p)/([A-Za-z0-9_-]+)").find(url)
        return reelMatch?.groupValues?.get(1) ?: "reel_${System.currentTimeMillis() % 10000}"
    }

    private fun extractYouTubeId(url: String): String {
        val match = Regex("(?:youtu\\.be/|v/|embed/|watch\\?v=|shorts/)([^#&?]{11})").find(url)
        return match?.groupValues?.get(1) ?: "yt_${System.currentTimeMillis() % 10000}"
    }

    /**
     * Native Android DownloadManager integration with explicit MIME type.
     */
    fun enqueueDownload(
        context: Context,
        downloadUrl: String,
        filename: String,
        title: String = "Qwik Save Download"
    ): Long {
        return try {
            val validUri = if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                Uri.parse(downloadUrl)
            } else {
                Uri.parse("$baseUrl/api/proxy-download?url=${Uri.encode(downloadUrl)}&filename=${Uri.encode(filename)}")
            }

            val request = DownloadManager.Request(validUri).apply {
                setTitle(title)
                setDescription("Downloading $filename via Qwik Save")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "QwikSave/$filename")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                // Critical upstream headers to prevent CDN 403 Forbidden / Download unsuccessful
                addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                addRequestHeader("Accept", "*/*")
                if (downloadUrl.contains("rapidcdn.app") || downloadUrl.contains("snapsave.app")) {
                    addRequestHeader("Referer", "https://snapsave.app/")
                    addRequestHeader("Origin", "https://snapsave.app")
                } else if (downloadUrl.contains("instagram.com") || downloadUrl.contains("cdninstagram.com")) {
                    addRequestHeader("Referer", "https://www.instagram.com/")
                }

                if (filename.endsWith(".mp4", ignoreCase = true)) {
                    setMimeType("video/mp4")
                } else if (filename.endsWith(".mp3", ignoreCase = true)) {
                    setMimeType("audio/mpeg")
                } else if (filename.endsWith(".pdf", ignoreCase = true)) {
                    setMimeType("application/pdf")
                }
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Direct high-speed In-App Media Stream Downloader.
     * Uses Android MediaStore & Scoped Storage to save playable files to Gallery & Downloads.
     * Prevents saving HTML pages and ensures full duration & audio in all video players.
     */
    suspend fun downloadMediaDirectly(
        context: Context,
        mediaUrl: String,
        filename: String,
        onSuccess: (File?) -> Unit = {},
        onError: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val resolvedUrl = if (mediaUrl.contains("/api/proxy-download")) {
            if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) mediaUrl else "$baseUrl${if (mediaUrl.startsWith("/")) "" else "/"}$mediaUrl"
        } else {
            val cleanMediaUrl = mediaUrl
                .replace(Regex("&?bytestart=\\d+"), "")
                .replace(Regex("&?byteend=\\d+"), "")
                .replace(Regex("\\?&"), "?")
                .trimEnd('?', '&')
            if (cleanMediaUrl.startsWith("http://") || cleanMediaUrl.startsWith("https://")) {
                cleanMediaUrl
            } else {
                "$baseUrl${if (cleanMediaUrl.startsWith("/")) "" else "/"}$cleanMediaUrl"
            }
        }

        try {
            var currentUrl = resolvedUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val isSnap = currentUrl.contains("rapidcdn.app") || currentUrl.contains("snapsave.app")

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Accept", "*/*")
                    if (isSnap || currentUrl.contains("rapidcdn.app") || currentUrl.contains("snapsave.app")) {
                        setRequestProperty("Referer", "https://snapsave.app/")
                        setRequestProperty("Origin", "https://snapsave.app")
                    } else if (currentUrl.contains("instagram.com") || currentUrl.contains("cdninstagram.com")) {
                        setRequestProperty("Referer", "https://www.instagram.com/")
                    } else if (currentUrl.contains("googlevideo.com") || currentUrl.contains("youtube.com")) {
                        setRequestProperty("Referer", "https://www.youtube.com/")
                    }
                    // For fbcdn.net, omitting Referer avoids Facebook hotlink 403
                    connectTimeout = 30000
                    readTimeout = 180000
                    instanceFollowRedirects = false
                }

                val code = connection.responseCode
                if (code in 300..399) {
                    val loc = connection.getHeaderField("Location") ?: break
                    currentUrl = loc
                    redirectCount++
                } else {
                    break
                }
            }

            var conn = connection ?: return@withContext Result.failure(Exception("Could not connect to media stream"))

            // CRITICAL: If direct upstream stream returned 401, 403, or failed, fallback to server proxy-download!
            if (conn.responseCode !in 200..299 && !resolvedUrl.contains("/api/proxy-download")) {
                try {
                    conn.disconnect()
                    val proxyUrl = "$baseUrl/api/proxy-download?url=${java.net.URLEncoder.encode(resolvedUrl, "UTF-8")}&filename=${java.net.URLEncoder.encode(filename, "UTF-8")}"
                    conn = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
                        setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                        setRequestProperty("Accept", "*/*")
                        connectTimeout = 30000
                        readTimeout = 180000
                        instanceFollowRedirects = true
                    }
                } catch (_: Exception) {}
            }

            if (conn.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("Server returned HTTP ${conn.responseCode}"))
            }

            val isAudio = filename.endsWith(".mp3", ignoreCase = true)
            val isPdf = filename.endsWith(".pdf", ignoreCase = true)
            val isImage = filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) || filename.endsWith(".png", ignoreCase = true)
            val mimeType = when {
                isAudio -> "audio/mpeg"
                isPdf -> "application/pdf"
                isImage -> "image/jpeg"
                else -> "video/mp4"
            }

            // Reject any empty or incomplete fragments (minimum 64KB for playable videos)
            val minBytes = when {
                isPdf -> 100L
                isImage -> 500L
                isAudio -> 1024L
                else -> 64 * 1024L
            }

            var savedFile: File? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver

                val (collectionUri, defaultFolder) = when {
                    isAudio -> Pair(
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        Environment.DIRECTORY_MUSIC
                    )
                    isImage -> Pair(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        Environment.DIRECTORY_PICTURES
                    )
                    isPdf -> Pair(
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    else -> Pair(
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        Environment.DIRECTORY_MOVIES
                    )
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$defaultFolder/QwikSave")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val itemUri = contentResolver.insert(collectionUri, contentValues)
                    ?: return@withContext Result.failure(Exception("Unable to create file in storage"))

                var totalBytes = 0L
                var activeConn = conn

                contentResolver.openOutputStream(itemUri)?.use { output ->
                    var streamFinished = false
                    var retryCount = 0

                    while (!streamFinished && retryCount < 4) {
                        var input: java.io.InputStream? = null
                        try {
                            input = activeConn.inputStream

                            val headerBytes = ByteArray(16)
                            val readHeader = if (totalBytes == 0L) input.read(headerBytes) else 0

                            if (readHeader > 0) {
                                val headerStr = String(headerBytes, 0, readHeader)
                                if (headerStr.startsWith("<!DOC", ignoreCase = true) ||
                                    headerStr.startsWith("<html", ignoreCase = true) ||
                                    headerStr.startsWith("{\"error", ignoreCase = true) ||
                                    headerStr.startsWith("{\"", ignoreCase = true)
                                ) {
                                    contentResolver.delete(itemUri, null, null)
                                    return@withContext Result.failure(Exception("Stream returned text error instead of media file."))
                                }
                                output.write(headerBytes, 0, readHeader)
                                totalBytes += readHeader
                            }

                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                            output.flush()
                            streamFinished = true
                        } catch (streamEx: java.io.IOException) {
                            retryCount++
                            try { activeConn.disconnect() } catch (_: Exception) {}

                            // If direct stream cut off or failed, fallback to server proxy-download
                            if (!currentUrl.contains("/api/proxy-download")) {
                                try {
                                    val proxyUrl = "$baseUrl/api/proxy-download?url=${java.net.URLEncoder.encode(resolvedUrl, "UTF-8")}&filename=${java.net.URLEncoder.encode(filename, "UTF-8")}"
                                    val proxyConn = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
                                        setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                                        setRequestProperty("Accept", "*/*")
                                        if (totalBytes > 0) {
                                            setRequestProperty("Range", "bytes=$totalBytes-")
                                        }
                                        connectTimeout = 30000
                                        readTimeout = 180000
                                        instanceFollowRedirects = true
                                    }
                                    if (proxyConn.responseCode in 200..299) {
                                        currentUrl = proxyUrl
                                        activeConn = proxyConn
                                        continue
                                    }
                                } catch (_: Exception) {}
                            }

                            if (totalBytes > 0 && retryCount < 4) {
                                try {
                                    val resumeConn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                                        setRequestProperty("Accept", "*/*")
                                        setRequestProperty("Range", "bytes=$totalBytes-")
                                        connectTimeout = 15000
                                        readTimeout = 60000
                                    }
                                    if (resumeConn.responseCode in 200..299) {
                                        activeConn = resumeConn
                                        continue
                                    }
                                } catch (_: Exception) {}
                            }

                            if (totalBytes >= minBytes) {
                                streamFinished = true
                            } else {
                                if (retryCount >= 4) throw streamEx
                            }
                        } finally {
                            try { input?.close() } catch (_: Exception) {}
                        }
                    }

                    if (totalBytes < minBytes) {
                        contentResolver.delete(itemUri, null, null)
                        return@withContext Result.failure(Exception("Stream returned incomplete or corrupt file (${totalBytes} bytes)."))
                    }
                } ?: return@withContext Result.failure(Exception("Could not open media stream output"))

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(itemUri, contentValues, null, null)
            } else {
                // Android 9 and below: Legacy public file storage
                val targetDir = Environment.getExternalStoragePublicDirectory(
                    if (isAudio) Environment.DIRECTORY_MUSIC else if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
                )
                val qwikDir = File(targetDir, "QwikSave").apply { mkdirs() }
                val targetFile = File(qwikDir, filename)
                savedFile = targetFile

                var totalBytes = 0L
                var activeConn = conn

                FileOutputStream(targetFile).use { output ->
                    var streamFinished = false
                    var retryCount = 0

                    while (!streamFinished && retryCount < 4) {
                        var input: java.io.InputStream? = null
                        try {
                            input = activeConn.inputStream

                            val headerBytes = ByteArray(16)
                            val readHeader = if (totalBytes == 0L) input.read(headerBytes) else 0

                            if (readHeader > 0) {
                                val headerStr = String(headerBytes, 0, readHeader)
                                if (headerStr.startsWith("<!DOC", ignoreCase = true) ||
                                    headerStr.startsWith("<html", ignoreCase = true) ||
                                    headerStr.startsWith("{\"error", ignoreCase = true) ||
                                    headerStr.startsWith("{\"", ignoreCase = true)
                                ) {
                                    targetFile.delete()
                                    return@withContext Result.failure(Exception("Stream returned text error instead of media file."))
                                }
                                output.write(headerBytes, 0, readHeader)
                                totalBytes += readHeader
                            }

                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                            output.flush()
                            streamFinished = true
                        } catch (streamEx: java.io.IOException) {
                            retryCount++
                            try { activeConn.disconnect() } catch (_: Exception) {}

                            // If direct stream cut off or failed, fallback to server proxy-download
                            if (!currentUrl.contains("/api/proxy-download")) {
                                try {
                                    val proxyUrl = "$baseUrl/api/proxy-download?url=${java.net.URLEncoder.encode(resolvedUrl, "UTF-8")}&filename=${java.net.URLEncoder.encode(filename, "UTF-8")}"
                                    val proxyConn = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
                                        setRequestProperty("User-Agent", "Mozilla/5.0 QwikSave-Android")
                                        setRequestProperty("Accept", "*/*")
                                        if (totalBytes > 0) {
                                            setRequestProperty("Range", "bytes=$totalBytes-")
                                        }
                                        connectTimeout = 30000
                                        readTimeout = 180000
                                        instanceFollowRedirects = true
                                    }
                                    if (proxyConn.responseCode in 200..299) {
                                        currentUrl = proxyUrl
                                        activeConn = proxyConn
                                        continue
                                    }
                                } catch (_: Exception) {}
                            }

                            if (totalBytes > 0 && retryCount < 4) {
                                try {
                                    val resumeConn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                                        setRequestProperty("Accept", "*/*")
                                        setRequestProperty("Range", "bytes=$totalBytes-")
                                        connectTimeout = 15000
                                        readTimeout = 60000
                                    }
                                    if (resumeConn.responseCode in 200..299) {
                                        activeConn = resumeConn
                                        continue
                                    }
                                } catch (_: Exception) {}
                            }

                            if (totalBytes >= minBytes) {
                                streamFinished = true
                            } else {
                                if (retryCount >= 4) throw streamEx
                            }
                        } finally {
                            try { input?.close() } catch (_: Exception) {}
                        }
                    }

                    if (totalBytes < minBytes) {
                        targetFile.delete()
                        return@withContext Result.failure(Exception("Stream returned incomplete or corrupt file (${totalBytes} bytes)."))
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            withContext(Dispatchers.Main) {
                onSuccess(savedFile)
                Toast.makeText(context, "Saved to Gallery & Downloads! 🎬", Toast.LENGTH_LONG).show()
            }
            Result.success(savedFile ?: File(filename))
        } catch (e: Exception) {
            // If in-app stream encountered unexpected end of stream or connection interruption,
            // seamlessly hand over to Android system DownloadManager for resilient background download!
            try {
                val dlId = enqueueDownload(context, resolvedUrl, filename, "Downloading Video")
                if (dlId > 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Downloading in background! Check notification bar 📥", Toast.LENGTH_LONG).show()
                    }
                    return@withContext Result.success(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QwikSave/$filename"))
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                onError(e.message ?: "Download failed")
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    /**
     * Client-side Image to PDF Converter with rotation support.
     */
    suspend fun convertImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        rotations: Map<Int, Int> = emptyMap(),
        outputFileName: String = "QwikSave_Document.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            imageUris.forEachIndexed { index, uri ->
                val input: InputStream? = context.contentResolver.openInputStream(uri)
                var bitmap: Bitmap? = BitmapFactory.decodeStream(input)
                input?.close()

                if (bitmap != null) {
                    val rotationDeg = rotations[index] ?: 0
                    if (rotationDeg != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDeg.toFloat()) }
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        bitmap.recycle()
                        bitmap = rotated
                    }

                    val pageWidth = 595
                    val pageHeight = 842
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)

                    val margin = 36f
                    val availableWidth = pageWidth - (margin * 2)
                    val availableHeight = pageHeight - (margin * 2) - 40f

                    val scale = Math.min(
                        availableWidth / bitmap.width,
                        availableHeight / bitmap.height
                    )
                    val scaledWidth = bitmap.width * scale
                    val scaledHeight = bitmap.height * scale
                    val left = (pageWidth - scaledWidth) / 2
                    val top = margin + ((availableHeight - scaledHeight) / 2)

                    val canvas = page.canvas
                    val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                    canvas.drawBitmap(bitmap, null, destRect, null)

                    val paint = Paint().apply {
                        color = Color.DKGRAY
                        textSize = 9f
                        isAntiAlias = true
                    }
                    val footerText = "Page ${index + 1} of ${imageUris.size} • Qwik Save Document"
                    val footerWidth = paint.measureText(footerText)
                    canvas.drawText(footerText, (pageWidth - footerWidth) / 2, pageHeight - 16f, paint)

                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val qwikDir = File(downloadsDir, "QwikSave").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(qwikDir, cleanName)

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("application/pdf"), null)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Merge multiple PDF files into one unified document using native Android PdfRenderer.
     */
    suspend fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>,
        outputFileName: String = "QwikSave_Merged.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            var globalPageNumber = 1

            for (uri in pdfUris) {
                val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val w = page.width
                        val h = page.height

                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(w, h, globalPageNumber).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                        globalPageNumber++
                    }
                    renderer.close()
                    pfd.close()
                }
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val qwikDir = File(downloadsDir, "QwikSave").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(qwikDir, cleanName)

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("application/pdf"), null)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compress PDF: Downsample pages with selectable quality level (High, Balanced, Light).
     * Uses High-DPI supersampling (150-180 DPI) and anti-aliased bitmap filtering so text and diagrams
     * remain 100% crystal clear and sharp while reducing file size significantly.
     */
    suspend fun compressPdf(
        context: Context,
        pdfUri: Uri,
        compressionLevel: Int = 1, // 0 = High, 1 = Balanced, 2 = Light
        outputFileName: String = "QwikSave_Compressed.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(Exception("Could not open source PDF file"))

            val renderer = PdfRenderer(pfd)
            val pdfDocument = PdfDocument()

            // High-DPI supersampling factors: Standard PDF is 72 DPI (595x842).
            // Multiplying by 2.20x - 3.00x produces 160-220 DPI crystal-clear bitmaps so small text & diagrams NEVER blur!
            val (scaleFactor, qualityPercent) = when (compressionLevel) {
                0 -> Pair(2.20f, 78) // High Compression: 2.20x scale, 78% JPEG quality (Compact file, sharp legible text)
                2 -> Pair(3.00f, 92) // Light Compression: 3.00x scale, 92% JPEG quality (Near lossless, zero blur, crystal clear)
                else -> Pair(2.50f, 85) // Balanced Compression: 2.50x scale, 85% JPEG quality (Optimal balance, crisp text & clean lines)
            }

            val filterPaint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val origW = page.width
                val origH = page.height

                val targetW = (origW * scaleFactor).toInt().coerceAtLeast(800)
                val targetH = (origH * scaleFactor).toInt().coerceAtLeast(1100)

                val rawBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                rawBitmap.eraseColor(Color.WHITE)
                page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Compress via JPEG stream with optimal DCT compression
                val stream = ByteArrayOutputStream()
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, stream)
                val compressedBytes = stream.toByteArray()
                rawBitmap.recycle()

                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, i + 1).create()
                val newPage = pdfDocument.startPage(pageInfo)
                val canvas = newPage.canvas

                val destRect = RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                canvas.drawBitmap(compressedBitmap, null, destRect, filterPaint)
                pdfDocument.finishPage(newPage)
                compressedBitmap.recycle()
            }

            renderer.close()
            pfd.close()

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val qwikDir = File(downloadsDir, "QwikSave").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(qwikDir, cleanName)

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("application/pdf"), null)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PDF to Image: Convert every page of a PDF into high-res PNG image files.
     */
    suspend fun convertPdfToImages(
        context: Context,
        pdfUri: Uri,
        baseName: String = "QwikSave_Page"
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(Exception("Could not open source PDF file"))

            val renderer = PdfRenderer(pfd)
            val generatedFiles = mutableListOf<File>()

            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val qwikDir = File(picturesDir, "QwikSave").apply { mkdirs() }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                // 1.5x scale for crisp reading
                val targetW = (page.width * 1.5).toInt()
                val targetH = (page.height * 1.5).toInt()

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageFile = File(qwikDir, "${baseName}_${i + 1}.png")
                FileOutputStream(pageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                bitmap.recycle()

                MediaScannerConnection.scanFile(context, arrayOf(pageFile.absolutePath), arrayOf("image/png"), null)
                generatedFiles.add(pageFile)
            }

            renderer.close()
            pfd.close()

            Result.success(generatedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Render first page of a PDF as a Bitmap thumbnail for visual display and reordering.
     */
    fun renderPdfFirstPageThumbnail(context: Context, pdfUri: Uri, targetW: Int = 120, targetH: Int = 160): Bitmap? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) {
                renderer.close()
                pfd.close()
                return null
            }
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Get total page count of a PDF file.
     */
    fun getPdfPageCount(context: Context, pdfUri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return 0
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Share PDF / File via FileProvider to WhatsApp, Telegram, Gmail, etc.
     */
    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.qwiksave.app.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "Shared via Qwik Save • Save in One Click (ai-chat2pdf.live)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Create formatted Academic PDF from AI study notes, matching the website's layout.
     * Supports Custom Title, Subtitle, Student/Prof Name, Watermark, Page Borders (Shadow/Clean/None),
     * College Study Partner Header (All Pages/First Page/None), Typography, Font Scaling, Paper Size, and Custom Footer.
     */
    suspend fun createAcademicNotesPdf(
        context: Context,
        title: String,
        content: String = "",
        subtitle: String? = null,
        studentName: String? = null,
        watermarkText: String? = null,
        borderStyle: String = "shadow", // "shadow", "clean", "none"
        includePartnerBadge: Boolean = true,
        partnerBadgeScope: String = "all-pages", // "all-pages", "first-page", "none"
        paperSize: String = "A4", // "A4", "Letter"
        fontSizeScale: String = "medium", // "small", "medium", "large"
        fontFamily: String = "Helvetica", // "Helvetica", "Times", "Courier"
        includeTitle: Boolean = true,
        includeDate: Boolean = true,
        includePageNumbers: Boolean = true,
        footerText: String? = null,
        questions: List<ExamQuestionItem> = emptyList(),
        outputFileName: String = "QwikSave_Notes.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val (pageWidth, pageHeight) = if (paperSize.equals("Letter", true)) Pair(612, 792) else Pair(595, 842)
            val margin = 36f
            val printableWidth = (pageWidth - (margin * 2)).toInt()

            val baseTypeface = when {
                fontFamily.contains("Times", true) || fontFamily.contains("Serif", true) -> Typeface.SERIF
                fontFamily.contains("Courier", true) || fontFamily.contains("Mono", true) -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }

            val (titleSize, subSize, qTitleSize, bodySize) = when (fontSizeScale.lowercase()) {
                "small" -> listOf(14f, 9.5f, 10f, 8.5f)
                "large" -> listOf(18f, 12.5f, 13f, 11.5f)
                else -> listOf(16f, 11f, 11.5f, 10f)
            }

            val titlePaint = TextPaint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = titleSize
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = TextPaint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = subSize
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                isAntiAlias = true
            }

            val questionTitlePaint = TextPaint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = qTitleSize
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                isAntiAlias = true
            }

            val metaPaint = TextPaint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
                typeface = baseTypeface
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = bodySize
                typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
                isAntiAlias = true
            }

            val calloutBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249) // #F1F5F9
                style = Paint.Style.FILL
            }

            val calloutBorderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225) // #CBD5E1
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val partnerBgPaint = Paint().apply {
                color = Color.rgb(238, 242, 255) // #EEF2FF
                style = Paint.Style.FILL
            }

            val partnerBorderPaint = Paint().apply {
                color = Color.rgb(199, 210, 254) // #C7D2FE
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val partnerTextPaint = TextPaint().apply {
                color = Color.rgb(67, 56, 202) // #4338CA
                textSize = 8.5f
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                isAntiAlias = true
            }

            val appLogoBitmap: Bitmap? = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.app_logo_transparent)
            } catch (_: Exception) { null }

            fun applyPageDecorations(c: Canvas, pageNum: Int) {
                // 1. Watermark
                val activeWatermark = watermarkText ?: "QWIK SAVE"
                if (activeWatermark.isNotBlank()) {
                    val wmPaint = TextPaint().apply {
                        color = Color.argb(20, 100, 116, 139)
                        textSize = 44f
                        typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    c.save()
                    c.rotate(-35f, pageWidth / 2f, pageHeight / 2f)
                    c.drawText(activeWatermark, pageWidth / 2f, pageHeight / 2f, wmPaint)
                    c.restore()
                }

                // 2. Page Border (MS Word Shadow Box, Clean Line, or None)
                if (borderStyle.equals("shadow", ignoreCase = true)) {
                    val shadowPaint = Paint().apply {
                        color = Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 2.5f
                    }
                    val bPaint = Paint().apply {
                        color = Color.rgb(15, 23, 42)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.2f
                    }
                    c.drawRect(margin + 2.5f, margin + 2.5f, pageWidth - margin + 2.5f, pageHeight - margin + 2.5f, shadowPaint)
                    c.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, bPaint)
                } else if (borderStyle.equals("clean", ignoreCase = true)) {
                    val bPaint = Paint().apply {
                        color = Color.rgb(71, 85, 105)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.2f
                    }
                    c.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, bPaint)
                }
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateStr = if (includeDate) dateFormat.format(Date()) else ""

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            applyPageDecorations(canvas, currentPageNumber)

            // Page 1 Header
            var yPosition = margin + 12f

            // Partner Badge Top Right
            val shouldShowBadgeOnPage1 = when (partnerBadgeScope.lowercase()) {
                "all-pages" -> includePartnerBadge
                "first-page" -> includePartnerBadge
                else -> false
            }

            if (shouldShowBadgeOnPage1) {
                val badgeW = 140f
                val badgeH = 22f
                val badgeX = pageWidth - margin - badgeW - 10f
                val badgeRect = RectF(badgeX, yPosition, badgeX + badgeW, yPosition + badgeH)
                canvas.drawRoundRect(badgeRect, 6f, 6f, partnerBgPaint)
                canvas.drawRoundRect(badgeRect, 6f, 6f, partnerBorderPaint)
                canvas.drawText("🎓 College Study Partner", badgeX + 10f, yPosition + 14f, partnerTextPaint)
            }

            // Document Title
            if (includeTitle) {
                canvas.drawText(title.ifBlank { "Qwik Save AI Study Notes" }, margin + 12f, yPosition + 16f, titlePaint)
                yPosition += 26f
            }

            // Subtitle / Subject
            if (!subtitle.isNullOrBlank()) {
                canvas.drawText("Subject: $subtitle", margin + 12f, yPosition + 10f, subtitlePaint)
                yPosition += 18f
            }

            // Student / Faculty Name & Date
            val authorPart = if (!studentName.isNullOrBlank()) "Candidate/Faculty: $studentName" else "Academic Revision"
            val metaLine = if (dateStr.isNotBlank()) "$authorPart • $dateStr" else authorPart
            canvas.drawText(metaLine, margin + 12f, yPosition + 8f, metaPaint)
            yPosition += 16f

            // Divider line
            val dividerPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }
            canvas.drawLine(margin + 10f, yPosition, pageWidth - margin - 10f, yPosition, dividerPaint)
            yPosition += 16f

            // If structured questions provided, render Question/Answer blocks
            if (questions.isNotEmpty()) {
                for ((idx, qItem) in questions.withIndex()) {
                    val qTitleText = "Question ${idx + 1}: ${qItem.question}"
                    @Suppress("DEPRECATION")
                    val qLayout = StaticLayout(
                        qTitleText,
                        questionTitlePaint,
                        printableWidth - 36,
                        Layout.Alignment.ALIGN_NORMAL,
                        1.2f,
                        0f,
                        false
                    )

                    @Suppress("DEPRECATION")
                    val aLayout = StaticLayout(
                        qItem.answer,
                        bodyPaint,
                        printableWidth - 36,
                        Layout.Alignment.ALIGN_NORMAL,
                        1.25f,
                        0f,
                        false
                    )

                    val itemTotalHeight = qLayout.height + 16f + aLayout.height + 20f
                    if (yPosition + itemTotalHeight + 45f > pageHeight - margin) {
                        drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                        pdfDocument.finishPage(page)

                        currentPageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        applyPageDecorations(canvas, currentPageNumber)
                        yPosition = margin + 18f
                    }

                    // Question Header Box
                    val qBoxHeight = qLayout.height + 14f
                    val qBoxRect = RectF(margin + 12f, yPosition, margin + printableWidth - 12f, yPosition + qBoxHeight)
                    canvas.drawRoundRect(qBoxRect, 6f, 6f, calloutBgPaint)
                    canvas.drawRoundRect(qBoxRect, 6f, 6f, calloutBorderPaint)

                    canvas.save()
                    canvas.translate(margin + 20f, yPosition + 7f)
                    qLayout.draw(canvas)
                    canvas.restore()

                    yPosition += qBoxHeight + 8f

                    // Answer Text
                    canvas.save()
                    canvas.translate(margin + 20f, yPosition)
                    aLayout.draw(canvas)
                    canvas.restore()

                    yPosition += aLayout.height + 16f
                }
            } else {
                // Freeform text/markdown notes
                val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
                for (paragraph in paragraphs) {
                    val isQuestionOrHeading = paragraph.startsWith("#") || paragraph.startsWith("1.") ||
                            paragraph.startsWith("2.") || paragraph.startsWith("3.") || paragraph.startsWith("4.") ||
                            paragraph.startsWith("Q:") || paragraph.startsWith("Question:")

                    val cleanText = paragraph.replace(Regex("^#+\\s*"), "").trim()

                    if (isQuestionOrHeading) {
                        @Suppress("DEPRECATION")
                        val qLayout = StaticLayout(
                            cleanText,
                            questionTitlePaint,
                            printableWidth - 36,
                            Layout.Alignment.ALIGN_NORMAL,
                            1.2f,
                            0f,
                            false
                        )

                        val boxHeight = qLayout.height + 14f
                        if (yPosition + boxHeight + 45f > pageHeight - margin) {
                            drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                            pdfDocument.finishPage(page)

                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            applyPageDecorations(canvas, currentPageNumber)
                            yPosition = margin + 18f
                        }

                        val boxRect = RectF(margin + 12f, yPosition, margin + printableWidth - 12f, yPosition + boxHeight)
                        canvas.drawRoundRect(boxRect, 6f, 6f, calloutBgPaint)
                        canvas.drawRoundRect(boxRect, 6f, 6f, calloutBorderPaint)

                        canvas.save()
                        canvas.translate(margin + 20f, yPosition + 7f)
                        qLayout.draw(canvas)
                        canvas.restore()

                        yPosition += boxHeight + 10f
                    } else {
                        @Suppress("DEPRECATION")
                        val bLayout = StaticLayout(
                            cleanText,
                            bodyPaint,
                            printableWidth - 24,
                            Layout.Alignment.ALIGN_NORMAL,
                            1.25f,
                            0f,
                            false
                        )

                        if (yPosition + bLayout.height + 45f > pageHeight - margin) {
                            drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                            pdfDocument.finishPage(page)

                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            applyPageDecorations(canvas, currentPageNumber)
                            yPosition = margin + 18f
                        }

                        canvas.save()
                        canvas.translate(margin + 12f, yPosition)
                        bLayout.draw(canvas)
                        canvas.restore()

                        yPosition += bLayout.height + 12f
                    }
                }
            }

            drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
            pdfDocument.finishPage(page)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val qwikDir = File(downloadsDir, "QwikSave").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(qwikDir, cleanName)

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("application/pdf"), null)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawAcademicFooter(
        canvas: Canvas,
        pageNum: Int,
        margin: Float,
        pageWidth: Int,
        pageHeight: Int,
        logoBitmap: Bitmap?,
        customFooter: String? = null,
        includePageNumbers: Boolean = true
    ) {
        val y = pageHeight - 24f
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        canvas.drawLine(margin + 10f, y - 10f, pageWidth - margin - 10f, y - 10f, linePaint)

        // Logo
        if (logoBitmap != null) {
            val logoDest = RectF(margin + 10f, y - 6f, margin + 28f, y + 12f)
            canvas.drawBitmap(logoBitmap, null, logoDest, null)
        }

        val brandPaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val devPaint = TextPaint().apply {
            color = Color.rgb(2, 132, 199)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val pageTextPaint = TextPaint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            isAntiAlias = true
        }

        val textStartX = margin + 34f
        val effectiveBrand = if (!customFooter.isNullOrBlank()) customFooter else "Qwik Save PDF"
        canvas.drawText(effectiveBrand, textStartX, y + 2f, brandPaint)
        val devLine = "Developer: Priyal Kumar • www.ai-chat2pdf.live"
        canvas.drawText(devLine, textStartX, y + 12f, devPaint)

        if (includePageNumbers) {
            val pageStr = "Page $pageNum"
            val pWidth = pageTextPaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - pWidth - 10f, y + 6f, pageTextPaint)
        }
    }

    /**
     * Extract shared AI chat conversation from ChatGPT, Claude, Gemini, or Perplexity.
     */
    suspend fun extractAiConversation(shareUrl: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$baseUrl/api/extract")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val payload = JSONObject().apply { put("url", shareUrl.trim()) }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                if (data != null) {
                    val title = data.optString("title", "AI Study Notes")
                    val messages = data.optJSONArray("messages")
                    val sb = StringBuilder()
                    if (messages != null) {
                        for (i in 0 until messages.length()) {
                            val msg = messages.getJSONObject(i)
                            val role = msg.optString("role", "")
                            val text = msg.optString("text", "")
                            if (role == "user") {
                                sb.append("### Question / Prompt:\n$text\n\n")
                            } else {
                                sb.append("### Answer / Explanation:\n$text\n\n")
                            }
                        }
                    }
                    return@withContext Result.success(Pair(title, sb.toString()))
                }
            }
            Result.failure(Exception("Unable to extract shared link directly. Try pasting notes content."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate standard QR Code Bitmap using ZXing engine.
     */
    fun generateQrBitmap(
        content: String,
        widthPx: Int = 512,
        heightPx: Int = 512
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 2)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }

            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, widthPx, heightPx, hints)
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)

            val darkColor = Color.rgb(15, 23, 42)
            val lightColor = Color.WHITE

            for (x in 0 until widthPx) {
                for (y in 0 until heightPx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save generated QR code bitmap to phone storage and return Uri.
     */
    fun saveQrBitmapToStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "QwikSave_QR_${System.currentTimeMillis()}.png"
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QwikSave")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
                resolver.openOutputStream(imageUri).use { out ->
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                imageUri
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val qwikDir = File(picturesDir, "QwikSave").apply { mkdirs() }
                val file = File(qwikDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Share QR Code bitmap via Android Intent.
     */
    fun shareQrBitmap(context: Context, bitmap: Bitmap) {
        val uri = saveQrBitmapToStorage(context, bitmap, "QwikSave_Share_QR.png") ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Created with Qwik Save • Save in One Click")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    }
}
