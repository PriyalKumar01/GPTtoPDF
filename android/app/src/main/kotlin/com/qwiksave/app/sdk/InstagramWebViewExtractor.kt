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
import kotlin.coroutines.resume

object InstagramWebViewExtractor {

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractFromDevice(
        context: Context,
        rawUrl: String,
        timeoutMs: Long = 12000
    ): Result<ExtractedMedia> = withContext(Dispatchers.Main) {
        val shortcode = extractShortcode(rawUrl) ?: "reel_${System.currentTimeMillis()}"
        val normalizedUrl = "https://www.instagram.com/reel/$shortcode/"

        val extraction = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Result<ExtractedMedia>> { cont ->
                var isDone = false
                var webView: WebView? = null

                fun finish(success: Boolean, streamUrl: String = "", errorMsg: String = "") {
                    if (isDone) return
                    isDone = true
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}

                    if (success && streamUrl.isNotBlank()) {
                        cont.resume(
                            Result.success(
                                ExtractedMedia(
                                    id = shortcode,
                                    title = "Instagram Reel ($shortcode)",
                                    thumbnailUrl = "https://www.instagram.com/p/$shortcode/media/?size=l",
                                    streamUrl = streamUrl,
                                    quality = "1080p Full HD",
                                    fileSizeMb = "HD Video",
                                    platform = "instagram",
                                    isVideo = true,
                                    author = "@instagram_creator",
                                    durationText = "0:30"
                                )
                            )
                        )
                    } else {
                        cont.resume(Result.failure(Exception(errorMsg.ifBlank { "Could not extract video stream." })))
                    }
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
                                if (reqUrl.contains(".mp4") ||
                                    (reqUrl.contains("cdninstagram.com") && reqUrl.contains("bytestart")) ||
                                    (reqUrl.contains("fbcdn.net") && reqUrl.contains(".mp4"))
                                ) {
                                    Handler(Looper.getMainLooper()).post {
                                        finish(true, streamUrl = reqUrl)
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
                                            var v = document.querySelector('video');
                                            if (v && v.src && v.src.startsWith('http')) return v.src;
                                            var og = document.querySelector('meta[property="og:video"]');
                                            if (og && og.content) return og.content;
                                            return '';
                                        })()
                                        """.trimIndent()
                                    ) { jsResult ->
                                        val clean = jsResult?.replace("\"", "")?.trim() ?: ""
                                        if (clean.startsWith("http")) {
                                            finish(true, streamUrl = clean)
                                        }
                                    }
                                }, 2000)
                            }
                        }

                        loadUrl(normalizedUrl)
                    }

                    cont.invokeOnCancellation {
                        finish(false, errorMsg = "Extraction cancelled")
                    }
                } catch (e: Exception) {
                    finish(false, errorMsg = e.message ?: "Extraction failed")
                }
            }
        }

        extraction ?: Result.failure(Exception("Extraction timed out. Please check that the Reel is public."))
    }

    private fun extractShortcode(url: String): String? {
        val regex = Regex("""/(?:reel|reels|p|tv)/([A-Za-z0-9_-]+)""")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}
