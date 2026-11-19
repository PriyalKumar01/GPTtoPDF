package com.qwiksave.app.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

object InstagramWebViewExtractor {

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractFromDevice(
        context: Context,
        rawUrl: String,
        timeoutMs: Long = 13000
    ): Result<ExtractedMedia> = withContext(Dispatchers.Main) {
        val shortcode = extractShortcode(rawUrl) ?: "ig_${System.currentTimeMillis()}"
        val isReel = rawUrl.contains("/reel/", ignoreCase = true) || rawUrl.contains("/reels/", ignoreCase = true)
        val normalizedUrl = if (isReel) {
            "https://www.instagram.com/reel/$shortcode/"
        } else {
            "https://www.instagram.com/p/$shortcode/"
        }

        val extraction = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Result<ExtractedMedia>> { cont ->
                var isDone = false
                var webView: WebView? = null
                val detectedVideos = mutableListOf<String>()

                fun finishWithMedia(media: ExtractedMedia) {
                    if (isDone) return
                    isDone = true
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}
                    cont.resume(Result.success(media))
                }

                fun finishWithError(errorMsg: String) {
                    if (isDone) return
                    isDone = true
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}
                    cont.resume(Result.failure(Exception(errorMsg)))
                }

                try {
                    webView = WebView(context.applicationContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = MOBILE_UA
                        settings.mediaPlaybackRequiresUserGesture = false

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: ""
                                // Only capture complete, un-chunked standalone MP4 files
                                // Strictly reject byte-range fragments (bytestart/byteend) and audio chunks that cause 0-second corrupt files
                                if (reqUrl.contains(".mp4", ignoreCase = true) &&
                                    !reqUrl.contains("bytestart=") &&
                                    !reqUrl.contains("byteend=") &&
                                    !reqUrl.contains(".m4s") &&
                                    !reqUrl.contains("audio", ignoreCase = true)
                                ) {
                                    synchronized(detectedVideos) {
                                        if (!detectedVideos.contains(reqUrl)) {
                                            detectedVideos.add(reqUrl)
                                        }
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isDone) return@postDelayed
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var items = [];
                                            var seen = {};

                                            function addMedia(rawUrl, isVid) {
                                                if (!rawUrl || typeof rawUrl !== 'string' || !rawUrl.startsWith('http')) return;
                                                // Strip byte-range fragment parameters to get the full stream
                                                var clean = rawUrl.replace(/&?bytestart=\d+/g, '').replace(/&?byteend=\d+/g, '').replace(/\?&/g, '?').replace(/[?&]$/, '');
                                                if (!seen[clean] && !clean.includes('150x150') && !clean.includes('s150x150') && !clean.includes('rsrc.php')) {
                                                    seen[clean] = true;
                                                    items.push({ url: clean, isVideo: !!isVid });
                                                }
                                            }

                                            // 1. Videos on page (Reels / Video Posts)
                                            var vids = document.querySelectorAll('video');
                                            for (var i = 0; i < vids.length; i++) {
                                                var vSrc = vids[i].src || vids[i].currentSrc;
                                                if (!vSrc) {
                                                    var srcEl = vids[i].querySelector('source');
                                                    if (srcEl) vSrc = srcEl.src;
                                                }
                                                addMedia(vSrc, true);
                                            }

                                            // 2. Meta og:video tag
                                            var ogV = document.querySelector('meta[property="og:video"]');
                                            if (ogV && ogV.content) {
                                                addMedia(ogV.content, true);
                                            }

                                            // 3. Carousel slides & post images (highest resolution from srcset)
                                            var slideImgs = document.querySelectorAll('article ul li img, article img[srcset], div._aagv img, div[role="presentation"] img, div[role="button"] img');
                                            for (var j = 0; j < slideImgs.length; j++) {
                                                var img = slideImgs[j];
                                                var bestSrc = '';
                                                var srcset = img.getAttribute('srcset');
                                                if (srcset) {
                                                    var parts = srcset.split(',');
                                                    var lastPart = parts[parts.length - 1].trim().split(' ')[0];
                                                    if (lastPart && lastPart.startsWith('http')) {
                                                        bestSrc = lastPart;
                                                    }
                                                }
                                                if (!bestSrc && img.src && img.src.startsWith('http')) {
                                                    bestSrc = img.src;
                                                }
                                                addMedia(bestSrc, false);
                                            }

                                            // 4. Fallback og:image
                                            if (items.length === 0) {
                                                var ogI = document.querySelector('meta[property="og:image"]');
                                                if (ogI && ogI.content) {
                                                    addMedia(ogI.content, false);
                                                }
                                            }

                                            var title = document.title || 'Instagram Post';
                                            return JSON.stringify({
                                                title: title,
                                                items: items
                                            });
                                        })()
                                        """.trimIndent()
                                    ) { jsResult ->
                                        if (isDone) return@evaluateJavascript
                                        try {
                                            // Handle JSON or quoted string
                                            val cleanJson = if (jsResult != null && jsResult.startsWith("\"") && jsResult.endsWith("\"")) {
                                                JSONObject("{\"res\":$jsResult}").getString("res")
                                            } else {
                                                jsResult ?: ""
                                            }

                                            val parsed = JSONObject(cleanJson)
                                            val jsItems = parsed.optJSONArray("items")
                                            val title = parsed.optString("title", "Instagram Post ($shortcode)")

                                            val mediaItemList = mutableListOf<MediaItem>()

                                            // 1. Process DOM items first (photos and videos explicitly on page)
                                            if (jsItems != null) {
                                                for (k in 0 until jsItems.length()) {
                                                    val itm = jsItems.getJSONObject(k)
                                                    val u = itm.optString("url", "")
                                                    val isVid = itm.optBoolean("isVideo", false)
                                                    if (u.isNotBlank() && mediaItemList.none { it.url == u }) {
                                                        mediaItemList.add(
                                                            MediaItem(
                                                                url = u,
                                                                isVideo = isVid,
                                                                previewUrl = u,
                                                                quality = if (isVid) "1080p Full HD" else "Original HD Photo"
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            // 2. If it is a Reel or video is needed and not found in DOM, check clean intercepted videos
                                            if (mediaItemList.isEmpty() || (isReel && mediaItemList.none { it.isVideo })) {
                                                synchronized(detectedVideos) {
                                                    for (vUrl in detectedVideos) {
                                                        val cleanVUrl = vUrl.replace(Regex("&?bytestart=\\d+"), "")
                                                            .replace(Regex("&?byteend=\\d+"), "")
                                                            .replace(Regex("\\?&"), "?")
                                                            .trimEnd('?', '&')
                                                        if (mediaItemList.none { it.url == cleanVUrl }) {
                                                            mediaItemList.add(
                                                                0, // Insert video at top for reels
                                                                MediaItem(
                                                                    url = cleanVUrl,
                                                                    isVideo = true,
                                                                    previewUrl = "https://www.instagram.com/p/$shortcode/media/?size=l",
                                                                    quality = "1080p Full HD"
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (mediaItemList.isNotEmpty()) {
                                                val primary = mediaItemList[0]
                                                val finalIsVideo = if (isReel) true else primary.isVideo

                                                finishWithMedia(
                                                    ExtractedMedia(
                                                        id = shortcode,
                                                        title = if (mediaItemList.size > 1) "Instagram Carousel (${mediaItemList.size} items)" else if (finalIsVideo) "Instagram Reel ($shortcode)" else "Instagram Photo ($shortcode)",
                                                        thumbnailUrl = primary.previewUrl ?: "https://www.instagram.com/p/$shortcode/media/?size=l",
                                                        streamUrl = primary.url,
                                                        quality = if (finalIsVideo) "1080p Full HD" else "Original HD Photo",
                                                        fileSizeMb = if (finalIsVideo) "HD Video" else "High-Res Image",
                                                        platform = "instagram",
                                                        isVideo = finalIsVideo,
                                                        author = "@instagram_creator",
                                                        durationText = null,
                                                        items = mediaItemList
                                                    )
                                                )
                                            } else {
                                                // If still no items, check if we intercepted any video
                                                synchronized(detectedVideos) {
                                                    if (detectedVideos.isNotEmpty()) {
                                                        val v = detectedVideos[0].replace(Regex("&?bytestart=\\d+"), "")
                                                            .replace(Regex("&?byteend=\\d+"), "")
                                                            .replace(Regex("\\?&"), "?")
                                                            .trimEnd('?', '&')
                                                        finishWithMedia(
                                                            ExtractedMedia(
                                                                id = shortcode,
                                                                title = "Instagram Reel ($shortcode)",
                                                                thumbnailUrl = "https://www.instagram.com/p/$shortcode/media/?size=l",
                                                                streamUrl = v,
                                                                quality = "1080p Full HD",
                                                                fileSizeMb = "HD Video",
                                                                platform = "instagram",
                                                                isVideo = true,
                                                                author = "@instagram_creator",
                                                                durationText = null,
                                                                items = listOf(MediaItem(v, true, quality = "1080p Full HD"))
                                                            )
                                                        )
                                                        return@evaluateJavascript
                                                    }
                                                }
                                                finishWithError("Could not find media on Instagram post. Verify it is public.")
                                            }
                                        } catch (parseErr: Exception) {
                                            synchronized(detectedVideos) {
                                                if (detectedVideos.isNotEmpty()) {
                                                    val v = detectedVideos[0].replace(Regex("&?bytestart=\\d+"), "")
                                                        .replace(Regex("&?byteend=\\d+"), "")
                                                        .replace(Regex("\\?&"), "?")
                                                        .trimEnd('?', '&')
                                                    finishWithMedia(
                                                        ExtractedMedia(
                                                            id = shortcode,
                                                            title = "Instagram Reel ($shortcode)",
                                                            thumbnailUrl = "https://www.instagram.com/p/$shortcode/media/?size=l",
                                                            streamUrl = v,
                                                            quality = "1080p Full HD",
                                                            fileSizeMb = "HD Video",
                                                            platform = "instagram",
                                                            isVideo = true,
                                                            author = "@instagram_creator",
                                                            durationText = null,
                                                            items = listOf(MediaItem(v, true, quality = "1080p Full HD"))
                                                        )
                                                    )
                                                } else {
                                                    finishWithError("Extraction error: ${parseErr.message}")
                                                }
                                            }
                                        }
                                    }
                                }, 2200)
                            }
                        }

                        loadUrl(normalizedUrl)
                    }

                    cont.invokeOnCancellation {
                        finishWithError("Extraction cancelled")
                    }
                } catch (e: Exception) {
                    finishWithError(e.message ?: "Extraction failed")
                }
            }
        }

        extraction ?: Result.failure(Exception("Extraction timed out. Please check that the Instagram post or reel is public."))
    }

    private fun extractShortcode(url: String): String? {
        val regex = Regex("""/(?:reel|reels|p|tv)/([A-Za-z0-9_-]+)""")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}
