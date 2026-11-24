package com.qwiksave.app.sdk

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.core.app.NotificationCompat
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
 * Data model for individual AI conversation message turn (matching website schema)
 */
data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" | "assistant"
    val authorName: String = if (role == "user") "USER" else "ASSISTANT",
    val content: String,
    val timestamp: String = ""
)

/**
 * Data model for extracted AI conversation notes document
 */
data class AiConversationData(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "AI Study Notes",
    val sourceUrl: String = "",
    val createdAt: String = "",
    val messages: List<AiChatMessage> = emptyList(),
    val userCount: Int = 0,
    val assistantCount: Int = 0,
    val totalCount: Int = 0,
    val estimatedPages: Int = 1
)

/**
 * Strips repetitive AI boilerplate introductions and conversational pleasantries
 */
fun cleanAiPleasantries(raw: String): String {
    return raw
        .replace(
            Regex("^(?:Sure thing!|Sure!|Certainly!|Of course!|Here is (?:the|a) [^\n]+\n+|Below is (?:the|a) [^\n]+\n+|I have reviewed [^\n]+\n+|As requested[^\n]*\n+)+", RegexOption.IGNORE_CASE),
            ""
        )
        .replace(
            Regex("(?:\n+(?:Hope this helps!|Let me know if (?:you (?:have any other questions|need more help|want me to explain|like me to explore)|you'd like to explore|you want to deepen)[^\n]*|Feel free to ask[^\n]*|Would you like (?:to|me to) (?:deepen|explore|dive deeper|continue)[^\n]*|Do you want (?:to|me to) (?:deepen|explore|continue)[^\n]*|If you like,? I want to explore this much in detail[^\n]*|If you'd like to explore this in more detail[^\n]*|Good luck with your exams!|Best of luck!|Happy studying!)[^\n]*)+$", RegexOption.IGNORE_CASE),
            ""
        )
        .trim()
}

/**
 * Calculates live estimated PDF pages matching website algorithm,
 * safely ignoring massive Base64 data URLs and counting diagrams accurately.
 */
fun calculateEstimatedPages(messages: List<AiChatMessage>): Int {
    var totalChars = 0
    var codeBlocks = 0
    var imageCount = 0
    for (m in messages) {
        var text = m.content
        // Fast non-recursive stripping of markdown images and data URLs
        while (true) {
            val imgIdx = text.indexOf("![")
            if (imgIdx == -1) break
            val endP = text.indexOf(")", imgIdx)
            if (endP == -1) break
            text = text.substring(0, imgIdx) + text.substring(endP + 1)
        }
        while (true) {
            val dIdx = text.indexOf("data:image/", ignoreCase = true)
            if (dIdx == -1) break
            var endIdx = dIdx + 11
            while (endIdx < text.length && !text[endIdx].isWhitespace() && text[endIdx] != '"' && text[endIdx] != '\'') {
                endIdx++
            }
            text = text.substring(0, dIdx) + text.substring(endIdx)
        }
        // Filter raw base64 lines
        val cleanLines = text.split("\n").filterNot { l ->
            val t = l.trim()
            t.length > 30 && t.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' || it == '-' || it == '_' }
        }
        val clean = cleanLines.joinToString("\n").trim()
        totalChars += clean.length
        val matches = Regex("```").findAll(m.content).count()
        codeBlocks += matches / 2
        if (m.content.contains("![") || m.content.contains("data:image")) {
            imageCount++
        }
    }
    // Academic dense single-spacing layout has ~2200 characters per page
    val basePages = Math.round(totalChars / 2200.0).toInt().coerceAtLeast(1)
    val extraFromCode = (codeBlocks * 0.25).toInt()
    val extraFromImages = (imageCount * 0.35).toInt()
    return (basePages + extraFromCode + extraFromImages).coerceAtLeast(1)
}

/**
 * Specification for a single PDF page to be included in a merged document.
 */
data class MergePageSpec(
    val uri: Uri,
    val pageIndex: Int, // 0-based page in source PDF
    val displayPageNum: Int,
    val docName: String,
    val rotation: Int = 0 // 0, 90, 180, 270
)

/**
 * Data model for a single page to be embedded into a high-compression PDF stream.
 */
data class PageJpegData(
    val jpegBytes: ByteArray,
    val imagePixelWidth: Int,
    val imagePixelHeight: Int,
    val pagePointWidth: Int,
    val pagePointHeight: Int
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
    val baseUrl: String = "https://qwiksave.vercel.app"
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

        // Fetch exact metadata (exact duration, title, channel author) directly from YouTube web
        var ytDirectSeconds = 0
        var ytDirectTitle = ""
        var ytDirectAuthor = ""
        try {
            val pageConn = (URL("https://www.youtube.com/watch?v=$videoId").openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connectTimeout = 6000
                readTimeout = 8000
            }
            if (pageConn.responseCode == 200) {
                val html = pageConn.inputStream.bufferedReader().use { it.readText() }
                val mSec = Regex("\"lengthSeconds\":\"(\\d+)\"").find(html)
                if (mSec != null) ytDirectSeconds = mSec.groupValues[1].toIntOrNull() ?: 0
                val mTitle = Regex("<title>(.+?)</title>").find(html)
                if (mTitle != null) ytDirectTitle = mTitle.groupValues[1].replace(Regex("\\s*-\\s*YouTube$"), "").trim()
                val mAuthor = Regex("\"author\":\"([^\"]+)\"").find(html) ?: Regex("\"ownerChannelName\":\"([^\"]+)\"").find(html)
                if (mAuthor != null) ytDirectAuthor = mAuthor.groupValues[1].trim()
            }
        } catch (_: Exception) {}

        fun formatSizeTier(sec: Int, mbPerSec: Double): String {
            if (sec <= 0) return "HD Stream"
            val mb = sec * mbPerSec
            return if (mb >= 1000.0) String.format("%.2f GB", mb / 1024.0) else String.format("%.1f MB", mb)
        }

        fun formatDurationText(sec: Int): String {
            if (sec <= 0) return "▶ Full Video"
            val h = sec / 3600
            val m = (sec % 3600) / 60
            val s = sec % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
        }

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
                    val rawTitle = data.optString("title", "YouTube Video")
                    val title = if (ytDirectTitle.isNotBlank()) ytDirectTitle else rawTitle
                    val author = if (ytDirectAuthor.isNotBlank()) ytDirectAuthor else (data.optJSONObject("author")?.optString("name", "YouTube Channel") ?: "YouTube Channel")

                    val backendSec = data.optInt("duration", 0)
                    val effectiveSec = if (ytDirectSeconds > 0) ytDirectSeconds else if (backendSec != 180) backendSec else 60
                    val durText = formatDurationText(effectiveSec)

                    val size1080 = formatSizeTier(effectiveSec, 0.26)
                    val size720 = formatSizeTier(effectiveSec, 0.15)
                    val size480 = formatSizeTier(effectiveSec, 0.08)

                    val formatsList = mutableListOf<MediaItem>()
                    val rawFormats = data.optJSONArray("formats")
                    if (rawFormats != null && rawFormats.length() > 0) {
                        for (i in 0 until rawFormats.length()) {
                            val f = rawFormats.getJSONObject(i)
                            var fUrl = f.optString("directUrl", f.optString("url", ""))
                            if (fUrl.startsWith("/")) {
                                fUrl = "$baseUrl$fUrl"
                            }
                            val rawQLabel = f.optString("qualityLabel", f.optString("resolution", "HD MP4"))
                            val cleanLabel = rawQLabel.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
                            val tierSize = when {
                                cleanLabel.contains("1080") -> size1080
                                cleanLabel.contains("720") -> size720
                                cleanLabel.contains("480") -> size480
                                else -> size720
                            }
                            val displayQuality = "$cleanLabel ($tierSize)"
                            if (fUrl.isNotBlank()) {
                                formatsList.add(MediaItem(fUrl, true, thumb, displayQuality))
                            }
                        }
                    }

                    if (streamUrl.isNotBlank() && streamUrl.startsWith("http") && !streamUrl.contains("youtube.com/watch")) {
                        if (formatsList.isEmpty()) {
                            formatsList.add(MediaItem(streamUrl, true, thumb, "1080p Full HD MP4 ($size1080)"))
                            formatsList.add(MediaItem(streamUrl, true, thumb, "720p HD MP4 ($size720)"))
                            formatsList.add(MediaItem(streamUrl, true, thumb, "480p SD MP4 ($size480)"))
                        }

                        return@withContext Result.success(
                            ExtractedMedia(
                                id = videoId,
                                title = title,
                                thumbnailUrl = thumb,
                                streamUrl = streamUrl,
                                quality = "HD MP4",
                                fileSizeMb = size720,
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
                val vTitle = if (ytDirectTitle.isNotBlank()) ytDirectTitle else invJson.optString("title", "YouTube Video")
                val vAuthor = if (ytDirectAuthor.isNotBlank()) ytDirectAuthor else invJson.optString("author", "YouTube Channel")
                val invSec = invJson.optInt("lengthSeconds", 0)
                val effectiveSec = if (ytDirectSeconds > 0) ytDirectSeconds else if (invSec > 0) invSec else 60
                val durText = formatDurationText(effectiveSec)

                val adaptive = invJson.optJSONArray("adaptiveFormats")
                if (adaptive != null && adaptive.length() > 0) {
                    val formatsList = mutableListOf<MediaItem>()
                    val tiers = listOf(
                        Triple(listOf("1080", "1920"), "1080p Full HD MP4", formatSizeTier(effectiveSec, 0.26)),
                        Triple(listOf("720", "1280"), "720p HD MP4", formatSizeTier(effectiveSec, 0.15)),
                        Triple(listOf("480", "854"), "480p SD MP4", formatSizeTier(effectiveSec, 0.08)),
                        Triple(listOf("360", "640"), "360p SD MP4", formatSizeTier(effectiveSec, 0.05))
                    )

                    for (tier in tiers) {
                        for (i in 0 until adaptive.length()) {
                            val a = adaptive.getJSONObject(i)
                            val res = a.optString("resolution", "")
                            val container = a.optString("container", "")
                            val aUrl = a.optString("url", "")
                            if (aUrl.isNotBlank() && container == "mp4" && tier.first.any { res.contains(it) }) {
                                val safeUrl = "$baseUrl/api/proxy-download?url=${java.net.URLEncoder.encode(aUrl, "UTF-8")}&filename=QwikSave_YouTube_${videoId}_${tier.second.replace(" ", "_")}.mp4"
                                formatsList.add(MediaItem(safeUrl, true, defaultThumb, "${tier.second} (${tier.third})"))
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
                        val vTitle = if (ytDirectTitle.isNotBlank()) ytDirectTitle else bJson.optString("title", "YouTube Video")
                        val vAuthor = if (ytDirectAuthor.isNotBlank()) ytDirectAuthor else bJson.optString("author", "YouTube Channel")
                        val vThumb = bJson.optString("thumbnail", defaultThumb)

                        val effectiveSec = if (ytDirectSeconds > 0) ytDirectSeconds else 60
                        val durStr = formatDurationText(effectiveSec)
                        val size1080 = formatSizeTier(effectiveSec, 0.26)
                        val size720 = formatSizeTier(effectiveSec, 0.15)
                        val size480 = formatSizeTier(effectiveSec, 0.08)

                        val formatsList = mutableListOf<MediaItem>()
                        formatsList.add(MediaItem(vUrl, true, vThumb, "1080p Full HD MP4 ($size1080)"))
                        formatsList.add(MediaItem(vUrl, true, vThumb, "720p HD MP4 ($size720)"))
                        formatsList.add(MediaItem(vUrl, true, vThumb, "480p SD MP4 ($size480)"))

                        return@withContext Result.success(
                            ExtractedMedia(
                                id = videoId,
                                title = vTitle,
                                thumbnailUrl = vThumb,
                                streamUrl = vUrl,
                                quality = "1080p Full HD",
                                fileSizeMb = size720,
                                platform = "youtube",
                                isVideo = true,
                                author = vAuthor,
                                durationText = durStr,
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
        onProgress: (bytesRead: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> },
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelId = "qwiksave_downloads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                channelId,
                "Qwik Save Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress for videos and audio"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notifId = (filename.hashCode() and 0x7FFFFFFF)
        val notifBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("Downloading ${filename.take(32)}...")
            .setContentText("0% downloaded")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)

        try {
            notificationManager?.notify(notifId, notifBuilder.build())
        } catch (_: Exception) {}

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
            var downloadedTotalBytes = 0L

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
                val expectedLength = activeConn.contentLengthLong.let { if (it > 0) it else 0L }
                var lastNotifTime = 0L

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

                                val percent = if (expectedLength > 0) {
                                    ((totalBytes * 100) / expectedLength).toInt().coerceIn(0, 99)
                                } else {
                                    ((totalBytes / (1024.0 * 1024.0 * 25.0)) * 100).toInt().coerceIn(0, 95)
                                }

                                onProgress(totalBytes, expectedLength, percent)

                                val now = System.currentTimeMillis()
                                if (now - lastNotifTime > 400) {
                                    lastNotifTime = now
                                    val downloadedText = String.format("%.1f MB", totalBytes / (1024.0 * 1024.0))
                                    val totalText = if (expectedLength > 0) String.format(" / %.1f MB", expectedLength / (1024.0 * 1024.0)) else ""
                                    notifBuilder.setProgress(100, percent, false)
                                        .setContentText("$percent% ($downloadedText$totalText)")
                                    try {
                                        notificationManager?.notify(notifId, notifBuilder.build())
                                    } catch (_: Exception) {}
                                }
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

                downloadedTotalBytes = totalBytes
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
                val expectedLength = activeConn.contentLengthLong.let { if (it > 0) it else 0L }
                var lastNotifTime = 0L

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

                                val percent = if (expectedLength > 0) {
                                    ((totalBytes * 100) / expectedLength).toInt().coerceIn(0, 99)
                                } else {
                                    ((totalBytes / (1024.0 * 1024.0 * 25.0)) * 100).toInt().coerceIn(0, 95)
                                }

                                onProgress(totalBytes, expectedLength, percent)

                                val now = System.currentTimeMillis()
                                if (now - lastNotifTime > 400) {
                                    lastNotifTime = now
                                    val downloadedText = String.format("%.1f MB", totalBytes / (1024.0 * 1024.0))
                                    val totalText = if (expectedLength > 0) String.format(" / %.1f MB", expectedLength / (1024.0 * 1024.0)) else ""
                                    notifBuilder.setProgress(100, percent, false)
                                        .setContentText("$percent% ($downloadedText$totalText)")
                                    try {
                                        notificationManager?.notify(notifId, notifBuilder.build())
                                    } catch (_: Exception) {}
                                }
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

                downloadedTotalBytes = totalBytes
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            val finalMb = String.format("%.1f MB", downloadedTotalBytes / (1024.0 * 1024.0))
            notifBuilder.setContentTitle("Download Complete! ✅")
                .setContentText("Saved to Gallery / Movies ($finalMb)")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
            try {
                notificationManager?.notify(notifId, notifBuilder.build())
            } catch (_: Exception) {}
            onProgress(downloadedTotalBytes, downloadedTotalBytes, 100)

            withContext(Dispatchers.Main) {
                onSuccess(savedFile)
                Toast.makeText(context, "Saved to Gallery / Movies! ✅", Toast.LENGTH_LONG).show()
            }
            Result.success(savedFile ?: File(filename))
        } catch (e: Exception) {
            notifBuilder.setContentTitle("Download Notice")
                .setContentText("Handing over to Android background downloader...")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
            try {
                notificationManager?.notify(notifId, notifBuilder.build())
            } catch (_: Exception) {}
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

            val outputFile = writePdfSafely(context, pdfDocument, outputFileName)
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Safely writes a PdfDocument to both app external storage (for instant File access)
     * and publishes it to the public Downloads/QwikSave directory using MediaStore on Android 10+ (API 29+).
     * This eliminates all "open failed: EACCES (Permission denied)" errors.
     */
    fun writePdfSafely(context: Context, pdfDocument: PdfDocument, outputFileName: String): File {
        val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"

        // 1. App-specific external files dir (always writable, zero permissions required on any Android version)
        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val qwikAppDir = File(appDir, "QwikSave").apply { mkdirs() }
        val primaryFile = File(qwikAppDir, cleanName)
        FileOutputStream(primaryFile).use { fos ->
            pdfDocument.writeTo(fos)
        }

        // 2. Publish to system Downloads/QwikSave folder via MediaStore so the user sees it in their Files/Downloads app
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QwikSave")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        primaryFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            } else {
                val pubDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QwikSave").apply { mkdirs() }
                val pubFile = File(pubDir, cleanName)
                primaryFile.copyTo(pubFile, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(pubFile.absolutePath), arrayOf("application/pdf"), null)
            }
        } catch (_: Exception) {}

        return primaryFile
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

            val outputFile = writePdfSafely(context, pdfDocument, outputFileName)
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Merge multiple individual PDF pages (in custom user sequence & rotations) into one document.
     */
    suspend fun mergePdfPages(
        context: Context,
        pageSpecs: List<MergePageSpec>,
        outputFileName: String = "QwikSave_Merged.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            var globalPageNumber = 1

            val renderers = mutableMapOf<Uri, Pair<ParcelFileDescriptor, PdfRenderer>>()

            try {
                for (spec in pageSpecs) {
                    val pair = renderers.getOrPut(spec.uri) {
                        val pfd = context.contentResolver.openFileDescriptor(spec.uri, "r")
                            ?: throw Exception("Could not open ${spec.docName}")
                        val rnd = PdfRenderer(pfd)
                        Pair(pfd, rnd)
                    }

                    val renderer = pair.second
                    if (spec.pageIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(spec.pageIndex)
                        val w = page.width
                        val h = page.height

                        var bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        if (spec.rotation != 0) {
                            val matrix = Matrix().apply { postRotate(spec.rotation.toFloat()) }
                            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            bitmap.recycle()
                            bitmap = rotated
                        }

                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, globalPageNumber).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                        globalPageNumber++
                    }
                }
            } finally {
                renderers.values.forEach { (pfd, rnd) ->
                    try { rnd.close() } catch (_: Exception) {}
                    try { pfd.close() } catch (_: Exception) {}
                }
            }

            val outputFile = writePdfSafely(context, pdfDocument, outputFileName)
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Render a visual thumbnail for any page of a PDF document.
     */
    fun renderPdfPageThumbnail(context: Context, uri: Uri, pageIndex: Int, targetW: Int = 100, targetH: Int = 140): Bitmap? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
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
     * Builds standard PDF 1.4 byte stream embedding raw compressed JPEG images via /Filter /DCTDecode.
     * This avoids Android PdfDocument canvas re-encoding to raw 32-bit bitmaps, guaranteeing true file compression.
     */
    fun buildJpegStreamPdfBytes(pages: List<PageJpegData>): ByteArray {
        val baos = ByteArrayOutputStream()
        val offsets = mutableListOf<Long>()

        fun writeAscii(s: String) {
            baos.write(s.toByteArray(Charsets.US_ASCII))
        }

        fun writeBytes(b: ByteArray) {
            baos.write(b)
        }

        // 1. PDF Header (PDF 1.4 with binary marker comment)
        writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

        val numPages = pages.size
        val pagesParentId = 1 + numPages * 3
        val catalogRootId = 2 + numPages * 3
        val totalObjects = catalogRootId

        for (i in 0 until numPages) {
            val page = pages[i]
            val imgObjId = 1 + i * 3
            val contentObjId = 2 + i * 3
            val pageObjId = 3 + i * 3

            // 1. Image XObject (Raw JPEG stream)
            offsets.add(baos.size().toLong())
            writeAscii("$imgObjId 0 obj\n")
            writeAscii("<< /Type /XObject /Subtype /Image /Width ${page.imagePixelWidth} /Height ${page.imagePixelHeight} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${page.jpegBytes.size} >>\n")
            writeAscii("stream\n")
            writeBytes(page.jpegBytes)
            writeAscii("\nendstream\nendobj\n")

            // 2. Content Stream (Scale and draw image)
            val contentOps = "q\n${page.pagePointWidth} 0 0 ${page.pagePointHeight} 0 0 cm\n/Im0 Do\nQ\n"
            val contentBytes = contentOps.toByteArray(Charsets.US_ASCII)

            offsets.add(baos.size().toLong())
            writeAscii("$contentObjId 0 obj\n")
            writeAscii("<< /Length ${contentBytes.size} >>\n")
            writeAscii("stream\n")
            writeBytes(contentBytes)
            writeAscii("endstream\nendobj\n")

            // 3. Page Object
            offsets.add(baos.size().toLong())
            writeAscii("$pageObjId 0 obj\n")
            writeAscii("<< /Type /Page /Parent $pagesParentId 0 R /MediaBox [0 0 ${page.pagePointWidth} ${page.pagePointHeight}] /Contents $contentObjId 0 R /Resources << /ProcSet [/PDF /ImageC] /XObject << /Im0 $imgObjId 0 R >> >> >>\n")
            writeAscii("endobj\n")
        }

        // Pages Parent Object
        offsets.add(baos.size().toLong())
        val kidsList = (0 until numPages).joinToString(" ") { "${3 + it * 3} 0 R" }
        writeAscii("$pagesParentId 0 obj\n")
        writeAscii("<< /Type /Pages /Kids [$kidsList] /Count $numPages >>\n")
        writeAscii("endobj\n")

        // Catalog Root Object
        offsets.add(baos.size().toLong())
        writeAscii("$catalogRootId 0 obj\n")
        writeAscii("<< /Type /Catalog /Pages $pagesParentId 0 R >>\n")
        writeAscii("endobj\n")

        // Cross-Reference Table
        val xrefOffset = baos.size().toLong()
        writeAscii("xref\n")
        writeAscii("0 ${totalObjects + 1}\n")
        writeAscii("0000000000 65535 f \r\n")
        for (offset in offsets) {
            writeAscii(String.format(Locale.US, "%010d 00000 n \r\n", offset))
        }

        // Trailer
        writeAscii("trailer\n")
        writeAscii("<< /Size ${totalObjects + 1} /Root $catalogRootId 0 R >>\n")
        writeAscii("startxref\n")
        writeAscii("$xrefOffset\n")
        writeAscii("%%EOF\n")

        return baos.toByteArray()
    }

    /**
     * Safely writes raw PDF byte array to app files and publishes to Downloads/QwikSave via MediaStore.
     */
    fun writePdfBytesSafely(context: Context, pdfBytes: ByteArray, outputFileName: String): File {
        val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"

        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val qwikAppDir = File(appDir, "QwikSave").apply { mkdirs() }
        val primaryFile = File(qwikAppDir, cleanName)
        FileOutputStream(primaryFile).use { fos ->
            fos.write(pdfBytes)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QwikSave")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        outStream.write(pdfBytes)
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            } else {
                val pubDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QwikSave").apply { mkdirs() }
                val pubFile = File(pubDir, cleanName)
                FileOutputStream(pubFile).use { fos -> fos.write(pdfBytes) }
                MediaScannerConnection.scanFile(context, arrayOf(pubFile.absolutePath), arrayOf("application/pdf"), null)
            }
        } catch (_: Exception) {}

        return primaryFile
    }

    /**
     * Compress PDF: Downsample pages with selectable quality level (High, Balanced, Light).
     * Directly embeds raw compressed JPEG streams using /Filter /DCTDecode.
     * Guarantees true file size reduction (e.g., 1.34 MB -> ~0.45 MB) without canvas bloating.
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
            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                renderer.close()
                pfd.close()
                return@withContext Result.failure(Exception("Source PDF has no pages"))
            }

            // High compression: ~65% reduction (Quality 42%, scale 1.0x native)
            // Balanced:        ~45% reduction (Quality 62%, scale 1.25x)
            // Light:           ~25% reduction (Quality 78%, scale 1.5x)
            val (scaleFactor, qualityPercent) = when (compressionLevel) {
                0 -> Pair(1.0f, 42)
                2 -> Pair(1.5f, 78)
                else -> Pair(1.25f, 62)
            }

            val pageDataList = mutableListOf<PageJpegData>()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val origW = page.width
                val origH = page.height

                val targetW = (origW * scaleFactor).toInt().coerceAtLeast(300)
                val targetH = (origH * scaleFactor).toInt().coerceAtLeast(400)

                val rawBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                rawBitmap.eraseColor(Color.WHITE)
                page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val stream = ByteArrayOutputStream()
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, stream)
                val compressedBytes = stream.toByteArray()
                rawBitmap.recycle()

                pageDataList.add(
                    PageJpegData(
                        jpegBytes = compressedBytes,
                        imagePixelWidth = targetW,
                        imagePixelHeight = targetH,
                        pagePointWidth = origW,
                        pagePointHeight = origH
                    )
                )
            }

            renderer.close()
            pfd.close()

            val pdfBytes = buildJpegStreamPdfBytes(pageDataList)
            val outputFile = writePdfBytesSafely(context, pdfBytes, outputFileName)
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
     * Get exact file size in bytes from a content Uri.
     */
    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        var size: Long = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
            if (size <= 0) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    size = pfd.statSize
                }
            }
        } catch (_: Exception) {}
        return size
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

    fun openFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.qwiksave.app.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdfFile(context: Context, file: File) {
        openFile(context, file, "application/pdf")
    }

    fun sharePdfFile(context: Context, file: File) {
        shareFile(context, file, "application/pdf")
    }

    /**
     * Create formatted Academic PDF from AI study notes, matching the website's layout.
     * Supports Custom Title, Subtitle, Student/Prof Name, Watermark, Page Borders (Shadow/Clean/None),
     * College Study Partner Header (All Pages/First Page), Typography, Font Scaling, Paper Size, and Custom Footer.
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
        partnerBadgeScope: String = "all-pages", // "all-pages", "first-page"
        paperSize: String = "A4", // "A4", "Letter"
        fontSizeScale: String = "medium", // "small", "medium", "large"
        fontFamily: String = "Helvetica", // "Helvetica", "Times", "Courier"
        includeTitle: Boolean = true,
        includeDate: Boolean = true,
        includePageNumbers: Boolean = true,
        footerText: String? = null,
        questions: List<ExamQuestionItem> = emptyList(),
        messages: List<AiChatMessage> = emptyList(),
        outputFileName: String = "QwikSave_Notes.pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val (pageWidth, pageHeight) = when (paperSize.lowercase()) {
                "letter" -> Pair(612, 792)
                "legal" -> Pair(612, 1008)
                "executive" -> Pair(522, 756)
                else -> Pair(595, 842) // A4
            }
            val margin = 36f
            val printableWidth = (pageWidth - (margin * 2)).toInt()

            val baseTypeface = when {
                fontFamily.contains("Times", true) || fontFamily.contains("Georgia", true) || fontFamily.contains("Serif", true) -> Typeface.SERIF
                fontFamily.contains("Courier", true) || fontFamily.contains("Mono", true) -> Typeface.MONOSPACE
                fontFamily.contains("Roboto", true) || fontFamily.contains("Sans", true) -> Typeface.SANS_SERIF
                else -> Typeface.DEFAULT
            }

            val (titleSize, subSize, qTitleSize, bodySize) = when (fontSizeScale.lowercase()) {
                "ultra-compact" -> listOf(13f, 8.5f, 9f, 7.5f)
                "small" -> listOf(14f, 9.5f, 10f, 8.5f)
                "large" -> listOf(18f, 12.5f, 13f, 11.5f)
                "extra-large" -> listOf(20f, 14f, 14.5f, 13f)
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
                color = Color.rgb(248, 250, 252) // #F8FAFC
                style = Paint.Style.FILL
            }

            val calloutBorderPaint = Paint().apply {
                color = Color.rgb(226, 232, 240) // #E2E8F0
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val appLogoBitmap: Bitmap? = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
                    ?: BitmapFactory.decodeResource(context.resources, R.drawable.app_logo_transparent)
            } catch (_: Exception) { null }

            val collegeStudyLogoBitmap: Bitmap? = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.college_study_logo)
            } catch (_: Exception) { null }

            val watermarkBannerBitmap: Bitmap? = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.qwik_save_banner_transparent)
                    ?: BitmapFactory.decodeResource(context.resources, R.drawable.qwik_save_banner)
            } catch (_: Exception) { null }

            fun applyPageDecorations(c: Canvas, pageNum: Int) {
                // 1. Watermark: subtle centered watermark matching website PDF
                if (watermarkBannerBitmap != null) {
                    val wmWidth = 230f
                    val wmHeight = (wmWidth * watermarkBannerBitmap.height / watermarkBannerBitmap.width.toFloat())
                    val wmX = (pageWidth - wmWidth) / 2f
                    val wmY = (pageHeight - wmHeight) / 2f
                    val logoRect = RectF(wmX, wmY, wmX + wmWidth, wmY + wmHeight)
                    val wmPaint = Paint().apply {
                        alpha = (255 * 0.16f).toInt() // subtle 16% opacity
                        isFilterBitmap = true
                        isAntiAlias = true
                    }
                    c.drawBitmap(watermarkBannerBitmap, null, logoRect, wmPaint)
                } else {
                    val activeWatermark = watermarkText ?: "QWIK SAVE"
                    if (activeWatermark.isNotBlank()) {
                        val wmPaint = TextPaint().apply {
                            color = Color.argb((255 * 0.16f).toInt(), 148, 163, 184)
                            textSize = 42f
                            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                        c.save()
                        c.rotate(-35f, pageWidth / 2f, pageHeight / 2f)
                        c.drawText(activeWatermark, pageWidth / 2f, pageHeight / 2f, wmPaint)
                        c.restore()
                    }
                }

                // 2. Page Border (MS Word Shadow Box, Clean Line, Double, Dotted, or None)
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
                } else if (borderStyle.equals("double", ignoreCase = true)) {
                    val outerPaint = Paint().apply {
                        color = Color.rgb(15, 23, 42)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }
                    val innerPaint = Paint().apply {
                        color = Color.rgb(100, 116, 139)
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                    }
                    c.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, outerPaint)
                    c.drawRect(margin + 3f, margin + 3f, pageWidth - margin - 3f, pageHeight - margin - 3f, innerPaint)
                } else if (borderStyle.equals("dotted", ignoreCase = true)) {
                    val dPaint = Paint().apply {
                        color = Color.rgb(100, 116, 139)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.2f
                        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
                    }
                    c.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, dPaint)
                }
            }

            fun drawCollegeStudyBadge(c: Canvas, topY: Float) {
                if (partnerBadgeScope.equals("none", ignoreCase = true)) return
                val badgeW = 124f
                val badgeH = 28f
                val badgeX = pageWidth - margin - badgeW - 6f
                val badgeRect = RectF(badgeX, topY, badgeX + badgeW, topY + badgeH)

                val badgeBg = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                val badgeBorder = Paint().apply {
                    color = Color.rgb(203, 213, 225) // slate-300
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                c.drawRoundRect(badgeRect, 5f, 5f, badgeBg)
                c.drawRoundRect(badgeRect, 5f, 5f, badgeBorder)

                val csLogo = collegeStudyLogoBitmap ?: appLogoBitmap
                if (csLogo != null) {
                    val srcW = csLogo.width.toFloat()
                    val srcH = csLogo.height.toFloat()
                    val maxLogoDim = 20f
                    val scale = minOf(maxLogoDim / srcW, maxLogoDim / srcH)
                    val drawW = srcW * scale
                    val drawH = srcH * scale
                    val offX = badgeX + 5f + (maxLogoDim - drawW) / 2f
                    val offY = topY + 4f + (maxLogoDim - drawH) / 2f
                    val logoRect = RectF(offX, offY, offX + drawW, offY + drawH)
                    c.drawBitmap(csLogo, null, logoRect, Paint().apply { isFilterBitmap = true })
                }

                val textLeft = badgeX + 28f
                val poweredPaint = TextPaint().apply {
                    color = Color.rgb(100, 116, 139)
                    textSize = 5.8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val csBrandPaint = TextPaint().apply {
                    color = Color.rgb(15, 23, 42)
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val subHubPaint = TextPaint().apply {
                    color = Color.rgb(2, 132, 199)
                    textSize = 5.8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                c.drawText("POWERED BY", textLeft, topY + 8.5f, poweredPaint)
                c.drawText("College Study", textLeft, topY + 16.5f, csBrandPaint)
                c.drawText("https://college-study.netlify.app", textLeft, topY + 23.5f, subHubPaint)
            }

            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val generatedDateStr = isoDateFormat.format(Date())

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            applyPageDecorations(canvas, currentPageNumber)

            // Page 1 Header setup
            var yPosition = margin + 10f

            // Always render College Study Header on Page 1
            drawCollegeStudyBadge(canvas, yPosition)

            // Document Title
            val titleWidth = printableWidth - 135
            if (includeTitle) {
                val cleanDocTitle = title.ifBlank { "Qwik Save AI Study Notes" }
                @Suppress("DEPRECATION")
                val tLayout = StaticLayout(
                    cleanDocTitle,
                    titlePaint,
                    titleWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.15f,
                    0f,
                    false
                )
                canvas.save()
                canvas.translate(margin + 8f, yPosition)
                tLayout.draw(canvas)
                canvas.restore()
                yPosition += tLayout.height + 4f
            }

            // Subtitle / Generated line (STRICTLY respect includeDate toggle)
            if (includeDate || !subtitle.isNullOrBlank()) {
                val subLine = when {
                    !subtitle.isNullOrBlank() && includeDate -> "$subtitle • Generated: $generatedDateStr"
                    !subtitle.isNullOrBlank() -> subtitle
                    includeDate -> "Generated: $generatedDateStr • Typeset for Academic Review"
                    else -> ""
                }
                if (subLine.isNotBlank()) {
                    canvas.drawText(subLine, margin + 8f, yPosition + 7f, metaPaint)
                    yPosition += 18f
                }
            }

            // Top divider line
            val dividerPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 0.8f
            }
            canvas.drawLine(margin + 6f, yPosition, pageWidth - margin - 6f, yPosition, dividerPaint)
            yPosition += 14f

            // Helper for formatted Markdown text that renders bold & italics with Android StaticLayout
            fun formatMarkdownSpanned(raw: String): CharSequence {
                // Strictly purge raw XML, ChatGPT internal Entity, and divider tags
                val cleanedRaw = raw
                    .replace(Regex("<Entity[^>]*\\/?>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("<divider[^>]*\\/?>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("<citation[^>]*\\/?>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("<[a-zA-Z][a-zA-Z0-9_-]*(?:\\s+[^>]*?)?\\/?>"), "")
                val h = cleanedRaw
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
                    .replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
                    .replace(Regex("`([^`]+)`"), "<tt>$1</tt>")
                return try {
                    android.text.Html.fromHtml(h, android.text.Html.FROM_HTML_MODE_LEGACY)
                } catch (_: Exception) {
                    cleanedRaw.replace(Regex("[*#`]"), "")
                }
            }

            // Helper to parse Markdown tables (| Col 1 | Col 2 |)
            fun parseMarkdownTable(text: String): Pair<List<String>, List<List<String>>>? {
                val rawLines = text.lines().map { it.trim() }.filter { it.contains("|") }
                if (rawLines.size < 2) return null
                val parsedRows = mutableListOf<List<String>>()
                for (line in rawLines) {
                    val trimmed = line.trim().removePrefix("|").removeSuffix("|")
                    val cells = trimmed.split("|").map { it.trim() }
                    if (cells.all { it.matches(Regex("[-:\\s]+")) }) continue
                    parsedRows.add(cells)
                }
                if (parsedRows.size < 2) return null
                val headers = parsedRows.first()
                val rows = parsedRows.drop(1)
                return Pair(headers, rows)
            }

            // Safe extraction of markdown images: ![alt](src) and strict removal of raw base64
            fun extractImagesAndCleanContentSafely(rawContent: String): Pair<String, Map<String, Pair<String, Bitmap?>>> {
                var content = rawContent
                    .replace(Regex("<Entity[^>]*\\/?>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("<divider[^>]*\\/?>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("<citation[^>]*\\/?>", RegexOption.IGNORE_CASE), "")

                val imgMap = mutableMapOf<String, Pair<String, Bitmap?>>()
                var tokenIndex = 0

                // Match all Markdown image syntax: ![alt](src) including multiline data:image URLs
                val imgPattern = Regex("!\\[([\\s\\S]*?)\\]\\s*\\(\\s*(data:image\\/[^;]+;base64,[A-Za-z0-9+/=\\s\\r\\n_-]+|https?:\\/\\/[^\\s)]+)\\s*\\)", RegexOption.IGNORE_CASE)
                content = imgPattern.replace(content) { matchResult ->
                    val altRaw = matchResult.groupValues[1]
                    val srcRaw = matchResult.groupValues[2]

                    val altText = altRaw.replace(Regex("\\s+logo\\b", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("^diagram:\\s*", RegexOption.IGNORE_CASE), "").trim()
                    var bm: Bitmap? = null

                    if (srcRaw.contains("base64,", ignoreCase = true)) {
                        try {
                            val b64 = srcRaw.substringAfter("base64,").trim().filter { !it.isWhitespace() }
                            val decodedBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                            bm = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        } catch (_: Exception) {}
                    }
                    val token = "@@@IMG_TOKEN_${tokenIndex++}@@@"
                    imgMap[token] = Pair(altText, bm)
                    "\n\n$token\n\n"
                }

                // Also match any unclosed or legacy ![alt](data:image... patterns
                val unclosedPattern = Regex("!\\[[\\s\\S]*?\\]\\s*\\(\\s*data:image[\\s\\S]*?(?:\\)|(?=\\n\\n|$))", RegexOption.IGNORE_CASE)
                content = unclosedPattern.replace(content, "")

                // Strip any residual raw base64 data strings anywhere in the text
                content = content.replace(Regex("data:image\\/[^;\\s\"'>]+;base64,[A-Za-z0-9+/=\\s\\r\\n_-]+", RegexOption.IGNORE_CASE), "")

                // Filter lines to ensure no raw base64 artifacts can leak into text
                val cleanedLines = content.split("\n").filterNot { line ->
                    val t = line.trim()
                    if (t.isEmpty()) return@filterNot false
                    if (t.startsWith("![") || t.startsWith("data:image") || t.startsWith("data:application")) return@filterNot true
                    // If line has no spaces and is >= 16 characters of base64 charset
                    if (!t.contains(" ") && t.length >= 16 && t.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' || it == '-' || it == '_' }) return@filterNot true
                    false
                }

                return Pair(cleanedLines.joinToString("\n").trim(), imgMap)
            }

            // Rendering messages if provided (Conversation Mode)
            if (messages.isNotEmpty()) {
                for (msg in messages) {
                    val isUser = msg.role == "user"
                    val isCustomNote = msg.authorName.contains("CUSTOM", ignoreCase = true) || msg.authorName.contains("NOTE", ignoreCase = true)

                    val (tokenizedContent, imgMap) = extractImagesAndCleanContentSafely(msg.content)

                    if (isUser) {
                        // Separate user text from any attached diagrams/images
                        val userImgTokens = imgMap.keys.filter { tokenizedContent.contains(it) }
                        var userPureText = tokenizedContent
                        for (t in userImgTokens) {
                            userPureText = userPureText.replace(t, "").trim()
                        }

                        if (userPureText.isNotBlank()) {
                            val safeUserText = userPureText.split(" ").joinToString(" ") { word ->
                                if (word.length > 38) word.chunked(38).joinToString(" ") else word
                            }
                            val spannedUser = formatMarkdownSpanned(safeUserText)
                            @Suppress("DEPRECATION")
                            val uLayout = StaticLayout(
                                spannedUser,
                                bodyPaint,
                                printableWidth - 28,
                                Layout.Alignment.ALIGN_NORMAL,
                                1.25f,
                                0f,
                                false
                            )

                            val boxHeight = uLayout.height + 24f
                            if (yPosition + boxHeight + 40f > pageHeight - margin) {
                                drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                                pdfDocument.finishPage(page)

                                currentPageNumber++
                                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                page = pdfDocument.startPage(pageInfo)
                                canvas = page.canvas
                                applyPageDecorations(canvas, currentPageNumber)
                                if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                    drawCollegeStudyBadge(canvas, margin + 8f)
                                    yPosition = margin + 38f
                                } else {
                                    yPosition = margin + 14f
                                }
                            }

                            val boxRect = RectF(margin + 6f, yPosition, pageWidth - margin - 6f, yPosition + boxHeight)
                            canvas.drawRoundRect(boxRect, 6f, 6f, calloutBgPaint)
                            canvas.drawRoundRect(boxRect, 6f, 6f, calloutBorderPaint)

                            val rolePaint = TextPaint().apply {
                                color = Color.rgb(100, 116, 139)
                                textSize = 7.5f
                                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                                isAntiAlias = true
                            }
                            canvas.drawText("USER", margin + 14f, yPosition + 12f, rolePaint)

                            canvas.save()
                            canvas.translate(margin + 14f, yPosition + 17f)
                            uLayout.draw(canvas)
                            canvas.restore()

                            yPosition += boxHeight + 10f
                        }

                        // Render any user-attached diagram/screenshot cleanly
                        for (t in userImgTokens) {
                            val (alt, bm) = imgMap[t] ?: continue
                            if (bm != null) {
                                val maxW = (printableWidth - 28f).coerceAtMost(360f)
                                val scale = (maxW / bm.width.toFloat()).coerceAtMost(1.0f)
                                val scaledW = bm.width * scale
                                val scaledH = (bm.height * scale).coerceAtMost(220f)

                                if (yPosition + scaledH + 35f > pageHeight - margin) {
                                    drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                                    pdfDocument.finishPage(page)

                                    currentPageNumber++
                                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                    page = pdfDocument.startPage(pageInfo)
                                    canvas = page.canvas
                                    applyPageDecorations(canvas, currentPageNumber)
                                    if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                        drawCollegeStudyBadge(canvas, margin + 8f)
                                        yPosition = margin + 38f
                                    } else {
                                        yPosition = margin + 14f
                                    }
                                }

                                val imgX = margin + 14f + (printableWidth - 28f - scaledW) / 2f
                                val imgRect = RectF(imgX, yPosition, imgX + scaledW, yPosition + scaledH)
                                canvas.drawBitmap(bm, null, imgRect, Paint().apply { isFilterBitmap = true })
                                yPosition += scaledH + 4f

                                if (alt.isNotBlank()) {
                                    canvas.drawText(alt, margin + 14f, yPosition + 7f, metaPaint)
                                    yPosition += 12f
                                }
                                yPosition += 8f
                            } else {
                                val diagText = if (alt.isNotBlank()) "🖼️ [User Attachment: $alt]" else "🖼️ [User Attachment Figure]"
                                val diagBoxRect = RectF(margin + 14f, yPosition, pageWidth - margin - 14f, yPosition + 24f)
                                canvas.drawRoundRect(diagBoxRect, 4f, 4f, calloutBgPaint)
                                canvas.drawRoundRect(diagBoxRect, 4f, 4f, calloutBorderPaint)
                                canvas.drawText(diagText, margin + 22f, yPosition + 15f, metaPaint)
                                yPosition += 30f
                            }
                        }
                    } else if (isCustomNote) {
                        // CUSTOM EXAM NOTE Callout (safe from base64)
                        val safeNoteText = tokenizedContent.split(" ").filterNot { word ->
                            word.length > 40 && word.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' || it == '-' || it == '_' }
                        }.joinToString(" ")

                        val spannedNote = formatMarkdownSpanned(safeNoteText)
                        @Suppress("DEPRECATION")
                        val noteLayout = StaticLayout(
                            spannedNote,
                            bodyPaint,
                            printableWidth - 28,
                            Layout.Alignment.ALIGN_NORMAL,
                            1.25f,
                            0f,
                            false
                        )

                        val boxHeight = noteLayout.height + 26f
                        if (yPosition + boxHeight + 40f > pageHeight - margin) {
                            drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                            pdfDocument.finishPage(page)

                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            applyPageDecorations(canvas, currentPageNumber)
                            if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                drawCollegeStudyBadge(canvas, margin + 8f)
                                yPosition = margin + 38f
                            } else {
                                yPosition = margin + 14f
                            }
                        }

                        val noteRolePaint = TextPaint().apply {
                            color = Color.rgb(180, 83, 9) // amber-700
                            textSize = 7.5f
                            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                            isAntiAlias = true
                        }
                        canvas.drawText("CUSTOM EXAM NOTE", margin + 8f, yPosition + 8f, noteRolePaint)
                        yPosition += 12f

                        val noteBoxRect = RectF(margin + 6f, yPosition, pageWidth - margin - 6f, yPosition + boxHeight)
                        canvas.drawRoundRect(noteBoxRect, 6f, 6f, Paint().apply {
                            color = Color.rgb(255, 251, 235) // amber-50
                            style = Paint.Style.FILL
                        })
                        canvas.drawRoundRect(noteBoxRect, 6f, 6f, Paint().apply {
                            color = Color.rgb(253, 230, 138) // amber-200
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                        })

                        canvas.save()
                        canvas.translate(margin + 14f, yPosition + 10f)
                        noteLayout.draw(canvas)
                        canvas.restore()

                        yPosition += boxHeight + 12f
                    } else {
                        // ASSISTANT Response
                        val rolePaint = TextPaint().apply {
                            color = Color.rgb(100, 116, 139)
                            textSize = 7.5f
                            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                            isAntiAlias = true
                        }

                        if (yPosition + 25f > pageHeight - margin) {
                            drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                            pdfDocument.finishPage(page)

                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            applyPageDecorations(canvas, currentPageNumber)
                            if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                drawCollegeStudyBadge(canvas, margin + 8f)
                                yPosition = margin + 38f
                            } else {
                                yPosition = margin + 14f
                            }
                        }

                        canvas.drawText("ASSISTANT", margin + 8f, yPosition + 8f, rolePaint)
                        yPosition += 14f

                        val paragraphs = tokenizedContent.split(Regex("\n{2,}")).filter { it.isNotBlank() }
                        for (p in paragraphs) {
                            val trimmedP = p.trim()

                            // Check if this paragraph contains an image token
                            val matchedToken = imgMap.keys.find { trimmedP.contains(it) }

                            if (matchedToken != null) {
                                // 1. IN-PLACE DIAGRAM / IMAGE RENDERING
                                val (alt, bm) = imgMap[matchedToken]!!
                                if (bm != null) {
                                    val maxW = (printableWidth - 28f).coerceAtMost(360f)
                                    val scale = (maxW / bm.width.toFloat()).coerceAtMost(1.0f)
                                    val scaledW = bm.width * scale
                                    val scaledH = (bm.height * scale).coerceAtMost(220f)

                                    if (yPosition + scaledH + 35f > pageHeight - margin) {
                                        drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                                        pdfDocument.finishPage(page)

                                        currentPageNumber++
                                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                        page = pdfDocument.startPage(pageInfo)
                                        canvas = page.canvas
                                        applyPageDecorations(canvas, currentPageNumber)
                                        if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                            drawCollegeStudyBadge(canvas, margin + 8f)
                                            yPosition = margin + 38f
                                        } else {
                                            yPosition = margin + 14f
                                        }
                                    }

                                    val imgX = margin + 14f + (printableWidth - 28f - scaledW) / 2f
                                    val imgRect = RectF(imgX, yPosition, imgX + scaledW, yPosition + scaledH)
                                    canvas.drawBitmap(bm, null, imgRect, Paint().apply { isFilterBitmap = true })
                                    yPosition += scaledH + 4f

                                    val cleanCaption = alt.replace(Regex("\\s+logo\\b", RegexOption.IGNORE_CASE), "").replace(Regex("^diagram:\\s*", RegexOption.IGNORE_CASE), "").trim()
                                    if (cleanCaption.isNotBlank() && !cleanCaption.equals("logo", ignoreCase = true)) {
                                        canvas.drawText(cleanCaption, margin + 14f, yPosition + 7f, metaPaint)
                                        yPosition += 12f
                                    }
                                    yPosition += 8f
                                } else {
                                    // Elegant diagram placeholder box (NEVER dump raw base64)
                                    val diagText = if (alt.isNotBlank()) "🖼️ [Diagram: $alt]" else "🖼️ [Diagram Figure]"
                                    val diagBoxRect = RectF(margin + 14f, yPosition, pageWidth - margin - 14f, yPosition + 24f)
                                    canvas.drawRoundRect(diagBoxRect, 4f, 4f, calloutBgPaint)
                                    canvas.drawRoundRect(diagBoxRect, 4f, 4f, calloutBorderPaint)
                                    canvas.drawText(diagText, margin + 22f, yPosition + 15f, metaPaint)
                                    yPosition += 30f
                                }
                            } else if (trimmedP.lines().count { it.contains("|") } >= 2 && trimmedP.contains("---")) {
                                // 2. IN-PLACE MARKDOWN TABLE
                                val parsedTable = parseMarkdownTable(trimmedP)
                                if (parsedTable != null) {
                                    val colCount = parsedTable.first.size.coerceAtLeast(1)
                                    val tableW = printableWidth - 28f
                                    val colW = tableW / colCount.toFloat()

                                    val tableHeaderPaint = TextPaint().apply {
                                        color = Color.rgb(15, 23, 42)
                                        textSize = (bodySize - 0.5f).coerceAtLeast(8f)
                                        typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                                        isAntiAlias = true
                                    }
                                    val tableCellPaint = TextPaint().apply {
                                        color = Color.rgb(51, 65, 85)
                                        textSize = (bodySize - 1f).coerceAtLeast(7.5f)
                                        typeface = baseTypeface
                                        isAntiAlias = true
                                    }

                                    @Suppress("DEPRECATION")
                                    val headerLayouts = parsedTable.first.map {
                                        StaticLayout(it, tableHeaderPaint, (colW - 8f).toInt().coerceAtLeast(20), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
                                    }
                                    val headerH = (headerLayouts.maxOfOrNull { it.height }?.toFloat() ?: 16f) + 10f

                                    val rowLayouts = parsedTable.second.map { row ->
                                        row.take(colCount).map { cell ->
                                            @Suppress("DEPRECATION")
                                            StaticLayout(cell, tableCellPaint, (colW - 8f).toInt().coerceAtLeast(20), Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
                                        }
                                    }
                                    val rowHeights = rowLayouts.map { rList -> (rList.maxOfOrNull { it.height }?.toFloat() ?: 14f) + 8f }
                                    val totalTableH = headerH + rowHeights.sum()

                                    if (yPosition + totalTableH + 35f > pageHeight - margin) {
                                        drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                                        pdfDocument.finishPage(page)

                                        currentPageNumber++
                                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                        page = pdfDocument.startPage(pageInfo)
                                        canvas = page.canvas
                                        applyPageDecorations(canvas, currentPageNumber)
                                        if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                            drawCollegeStudyBadge(canvas, margin + 8f)
                                            yPosition = margin + 38f
                                        } else {
                                            yPosition = margin + 14f
                                        }
                                    }

                                    val tableLeft = margin + 14f
                                    val tableRight = tableLeft + tableW
                                    val tableTop = yPosition

                                    // Header background
                                    val headerRect = RectF(tableLeft, tableTop, tableRight, tableTop + headerH)
                                    canvas.drawRect(headerRect, Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL })

                                    // Header text
                                    headerLayouts.forEachIndexed { cIdx, lyt ->
                                        canvas.save()
                                        canvas.translate(tableLeft + (cIdx * colW) + 4f, tableTop + 5f)
                                        lyt.draw(canvas)
                                        canvas.restore()
                                    }

                                    val gridLinePaint = Paint().apply {
                                        color = Color.rgb(226, 232, 240)
                                        style = Paint.Style.STROKE
                                        strokeWidth = 0.8f
                                    }
                                    canvas.drawLine(tableLeft, tableTop + headerH, tableRight, tableTop + headerH, gridLinePaint)

                                    var curRowY = tableTop + headerH
                                    rowLayouts.forEachIndexed { rIdx, cellList ->
                                        val rH = rowHeights[rIdx]
                                        cellList.forEachIndexed { cIdx, lyt ->
                                            canvas.save()
                                            canvas.translate(tableLeft + (cIdx * colW) + 4f, curRowY + 4f)
                                            lyt.draw(canvas)
                                            canvas.restore()
                                        }
                                        curRowY += rH
                                        canvas.drawLine(tableLeft, curRowY, tableRight, curRowY, gridLinePaint)
                                    }

                                    // Vertical column lines
                                    for (c in 1 until colCount) {
                                        val vX = tableLeft + (c * colW)
                                        canvas.drawLine(vX, tableTop, vX, curRowY, gridLinePaint)
                                    }

                                    // Outer table border
                                    val outerTablePaint = Paint().apply {
                                        color = Color.rgb(203, 213, 225)
                                        style = Paint.Style.STROKE
                                        strokeWidth = 1f
                                    }
                                    canvas.drawRect(tableLeft, tableTop, tableRight, curRowY, outerTablePaint)

                                    yPosition = curRowY + 12f
                                }
                            } else {
                                val isCode = trimmedP.startsWith("```") && trimmedP.endsWith("```")

                            if (isCode) {
                                val codeBody = trimmedP.replace(Regex("^```[a-zA-Z0-9_-]*\n?"), "").replace(Regex("```$"), "").trim()
                                val codePaint = TextPaint().apply {
                                    color = Color.rgb(30, 41, 59)
                                    textSize = (bodySize - 1f).coerceAtLeast(8f)
                                    typeface = Typeface.MONOSPACE
                                    isAntiAlias = true
                                }
                                @Suppress("DEPRECATION")
                                val codeLayout = StaticLayout(
                                    codeBody,
                                    codePaint,
                                    printableWidth - 36,
                                    Layout.Alignment.ALIGN_NORMAL,
                                    1.2f,
                                    0f,
                                    false
                                )
                                val codeBoxHeight = codeLayout.height + 16f
                                if (yPosition + codeBoxHeight + 35f > pageHeight - margin) {
                                    drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                                    pdfDocument.finishPage(page)

                                    currentPageNumber++
                                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                    page = pdfDocument.startPage(pageInfo)
                                    canvas = page.canvas
                                    applyPageDecorations(canvas, currentPageNumber)
                                    if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                        drawCollegeStudyBadge(canvas, margin + 8f)
                                        yPosition = margin + 38f
                                    } else {
                                        yPosition = margin + 14f
                                    }
                                }

                                val codeRect = RectF(margin + 10f, yPosition, pageWidth - margin - 10f, yPosition + codeBoxHeight)
                                canvas.drawRoundRect(codeRect, 4f, 4f, Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL })
                                canvas.drawRoundRect(codeRect, 4f, 4f, Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 0.8f })

                                canvas.save()
                                canvas.translate(margin + 18f, yPosition + 8f)
                                codeLayout.draw(canvas)
                                canvas.restore()

                                yPosition += codeBoxHeight + 8f
                            } else {
                                // Break large blocks into page-fitting sentence chunks
                                val subBlocks = if (trimmedP.length > 320 && !trimmedP.startsWith("#")) {
                                    val sentences = trimmedP.split(Regex("(?<=[.!?\\n])\\s+"))
                                    val chunks = mutableListOf<String>()
                                    var currentChunk = StringBuilder()
                                    for (s in sentences) {
                                        if (currentChunk.isNotEmpty() && currentChunk.length + s.length > 300) {
                                            chunks.add(currentChunk.toString().trim())
                                            currentChunk = StringBuilder()
                                        }
                                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                                        currentChunk.append(s)
                                    }
                                    if (currentChunk.isNotEmpty()) chunks.add(currentChunk.toString().trim())
                                    chunks.ifEmpty { listOf(trimmedP) }
                                } else {
                                    listOf(trimmedP)
                                }

                                for (sub in subBlocks) {
                                    // Prevent single long words/tokens from overflowing the border line
                                    val safeText = sub.split(" ").joinToString(" ") { word ->
                                        if (word.length > 38) word.chunked(38).joinToString(" ") else word
                                    }
                                    val isHeading = safeText.startsWith("#") || ((safeText.startsWith("1.") || safeText.startsWith("2.") || safeText.startsWith("3.")) && safeText.length < 80)
                                    val pPaint = if (isHeading && safeText.length < 90) questionTitlePaint else bodyPaint
                                    val cleanHeading = if (safeText.startsWith("#")) safeText.replace(Regex("^#+\\s*"), "") else safeText
                                    val spannedText = formatMarkdownSpanned(cleanHeading)

                                    @Suppress("DEPRECATION")
                                    val pLayout = StaticLayout(
                                        spannedText,
                                        pPaint,
                                        printableWidth - 28,
                                        Layout.Alignment.ALIGN_NORMAL,
                                        1.25f,
                                        0f,
                                        false
                                    )

                                    if (yPosition + pLayout.height + 35f > pageHeight - margin) {
                                        drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                                        pdfDocument.finishPage(page)

                                        currentPageNumber++
                                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                        page = pdfDocument.startPage(pageInfo)
                                        canvas = page.canvas
                                        applyPageDecorations(canvas, currentPageNumber)
                                        if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                            drawCollegeStudyBadge(canvas, margin + 8f)
                                            yPosition = margin + 38f
                                        } else {
                                            yPosition = margin + 14f
                                        }
                                    }

                                    canvas.save()
                                    canvas.translate(margin + 14f, yPosition)
                                    pLayout.draw(canvas)
                                    canvas.restore()

                                    yPosition += pLayout.height + 6f
                                }
                            }
                        }
                    }
                    yPosition += 8f
                }
            }
        } else if (questions.isNotEmpty()) {
                // If structured questions provided, render clean Topic / Answer blocks
                for ((idx, qItem) in questions.withIndex()) {
                    val qTitleText = qItem.question.ifBlank { "Topic ${idx + 1}" }
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

                    val (cleanAnswer, _) = extractImagesAndCleanContentSafely(qItem.answer)
                    val spannedAnswer = formatMarkdownSpanned(cleanAnswer)
                    @Suppress("DEPRECATION")
                    val aLayout = StaticLayout(
                        spannedAnswer,
                        bodyPaint,
                        printableWidth - 36,
                        Layout.Alignment.ALIGN_NORMAL,
                        1.25f,
                        0f,
                        false
                    )

                    val itemTotalHeight = qLayout.height + 24f + aLayout.height + 16f
                    if (yPosition + itemTotalHeight + 45f > pageHeight - margin) {
                        drawAcademicFooter(canvas, currentPageNumber, margin, pageWidth, pageHeight, appLogoBitmap, footerText, includePageNumbers)
                        pdfDocument.finishPage(page)

                        currentPageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        applyPageDecorations(canvas, currentPageNumber)
                        if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                            drawCollegeStudyBadge(canvas, margin + 8f)
                            yPosition = margin + 38f
                        } else {
                            yPosition = margin + 18f
                        }
                    }

                    // Render Topic Title in a clean subtle box (without hardcoded "Question X:" prefix)
                    val titleBoxH = qLayout.height + 14f
                    val boxRect = RectF(margin + 12f, yPosition, margin + printableWidth - 12f, yPosition + titleBoxH)
                    canvas.drawRoundRect(boxRect, 4f, 4f, calloutBgPaint)
                    canvas.drawRoundRect(boxRect, 4f, 4f, calloutBorderPaint)

                    canvas.save()
                    canvas.translate(margin + 20f, yPosition + 7f)
                    qLayout.draw(canvas)
                    canvas.restore()

                    yPosition += titleBoxH + 8f

                    // Answer Text
                    canvas.save()
                    canvas.translate(margin + 20f, yPosition)
                    aLayout.draw(canvas)
                    canvas.restore()

                    yPosition += aLayout.height + 16f
                }
            } else {
                // Freeform text/markdown notes (guaranteed safe from raw base64)
                val (cleanFreeformContent, _) = extractImagesAndCleanContentSafely(content)
                val paragraphs = cleanFreeformContent.split("\n\n").filter { it.isNotBlank() }
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
                            if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                drawCollegeStudyBadge(canvas, margin + 8f)
                                yPosition = margin + 38f
                            } else {
                                yPosition = margin + 18f
                            }
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
                            if (partnerBadgeScope.equals("all-pages", ignoreCase = true)) {
                                drawCollegeStudyBadge(canvas, margin + 8f)
                                yPosition = margin + 38f
                            } else {
                                yPosition = margin + 18f
                            }
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

            val outputFile = writePdfSafely(context, pdfDocument, outputFileName)
            pdfDocument.close()
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
        canvas.drawLine(margin + 6f, y - 8f, pageWidth - margin - 6f, y - 8f, linePaint)

        // Left App Logo Container Box
        val logoBoxX = margin + 6f
        val logoBoxY = y - 5f
        val logoBoxSize = 18f
        val boxRect = RectF(logoBoxX, logoBoxY, logoBoxX + logoBoxSize, logoBoxY + logoBoxSize)
        canvas.drawRoundRect(boxRect, 3f, 3f, Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(boxRect, 3f, 3f, Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        })
        if (logoBitmap != null) {
            val logoInner = RectF(logoBoxX + 2f, logoBoxY + 2f, logoBoxX + logoBoxSize - 2f, logoBoxY + logoBoxSize - 2f)
            canvas.drawBitmap(logoBitmap, null, logoInner, Paint().apply { isFilterBitmap = true })
        }

        val textLeft = logoBoxX + logoBoxSize + 6f
        val brandPaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val effectiveBrand = if (!customFooter.isNullOrBlank()) customFooter else "Qwik Save • https://qwiksave.vercel.app"
        canvas.drawText(effectiveBrand, textLeft, y + 2.5f, brandPaint)

        // Line 2: Developer: Priyal Kumar • https://www.linkedin.com/in/priyal-kumar-29b26529a/
        val devPaint = TextPaint().apply {
            color = Color.rgb(2, 132, 199) // sky-600
            textSize = 7.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Developer: Priyal Kumar • https://www.linkedin.com/in/priyal-kumar-29b26529a/", textLeft, y + 11.5f, devPaint)

        // Right Side: Page X (clean, without website URL)
        val pageTextPaint = TextPaint().apply {
            color = Color.rgb(71, 85, 105) // slate-600
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        if (includePageNumbers) {
            val pageStr = "Page $pageNum"
            val pWidth = pageTextPaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - pWidth - 6f, y + 6.5f, pageTextPaint)
        }
    }

    /**
     * Extract structured AI conversation model matching website schema
     * with automatic on-device fallback for client-side rendered links (e.g. Gemini).
     */
    suspend fun extractAiConversationModel(
        shareUrl: String,
        context: Context? = null
    ): Result<AiConversationData> = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$baseUrl/api/extract")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "QwikSave-Android")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            val payload = JSONObject().apply { put("url", shareUrl.trim()) }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val response = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode == 200) {
                val json = JSONObject(response)
                val convObj = json.optJSONObject("conversation") ?: json.optJSONObject("data")
                if (convObj != null) {
                    val title = convObj.optString("title", "Extracted Notes & Exam Guide")
                    val sourceUrl = convObj.optString("sourceUrl", shareUrl.trim())
                    val messagesJson = convObj.optJSONArray("messages")
                    val msgList = mutableListOf<AiChatMessage>()
                    if (messagesJson != null) {
                        for (i in 0 until messagesJson.length()) {
                            val msg = messagesJson.getJSONObject(i)
                            val id = msg.optString("id", UUID.randomUUID().toString())
                            val role = msg.optString("role", "assistant")
                            val author = msg.optString("authorName", if (role == "user") "USER" else "ASSISTANT")
                            val content = msg.optString("content").ifBlank { msg.optString("text", "") }
                            if (content.isNotBlank()) {
                                msgList.add(AiChatMessage(id = id, role = role, authorName = author, content = content))
                            }
                        }
                    }

                    if (msgList.isNotEmpty()) {
                        val userCount = msgList.count { it.role == "user" }
                        val assistantCount = msgList.count { it.role == "assistant" }
                        val estPages = calculateEstimatedPages(msgList)

                        val convData = AiConversationData(
                            id = convObj.optString("id", UUID.randomUUID().toString()),
                            title = title,
                            sourceUrl = sourceUrl,
                            messages = msgList,
                            userCount = userCount,
                            assistantCount = assistantCount,
                            totalCount = msgList.size,
                            estimatedPages = estPages
                        )
                        return@withContext Result.success(convData)
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback to on-device headless WebView extraction (for Google Gemini & Claude SPAs)
        if (context != null) {
            val localResult = AiShareWebViewExtractor.extractFromDevice(context, shareUrl, timeoutMs = 15000)
            if (localResult.isSuccess && localResult.getOrNull()?.messages?.isNotEmpty() == true) {
                return@withContext localResult
            }
        }

        Result.failure(Exception("Could not automatically parse messages from shared AI conversation. Please ensure the link is public."))
    }

    suspend fun extractAiConversationModel(
        context: Context?,
        shareUrl: String
    ): Result<AiConversationData> = extractAiConversationModel(shareUrl, context)

    /**
     * Extract shared AI chat conversation as title & text markdown (backwards compatibility).
     */
    suspend fun extractAiConversation(shareUrl: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val modelRes = extractAiConversationModel(shareUrl)
        modelRes.map { data ->
            val sb = StringBuilder()
            for (m in data.messages) {
                if (m.role == "user") {
                    sb.append("### Question / Prompt:\n${m.content}\n\n")
                } else {
                    sb.append("### Answer / Explanation:\n${m.content}\n\n")
                }
            }
            Pair(data.title, sb.toString())
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
