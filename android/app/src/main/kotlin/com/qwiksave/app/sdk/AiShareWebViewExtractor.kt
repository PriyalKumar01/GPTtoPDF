package com.qwiksave.app.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume

/**
 * On-Device AI Share Link Extractor for Android.
 * Securely extracts public conversation shares from ChatGPT, Claude, and Gemini
 * directly on device using native API calls and headless WebView DOM parsing.
 * Filters out all internal telemetry, Statsig JSON, and script artifacts.
 */
object AiShareWebViewExtractor {

    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    /**
     * Checks if a string contains internal script tags, telemetry JSON, or garbage config.
     */
    fun isGarbageScriptOrTelemetry(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return true
        if (t.startsWith("{\"") || t.startsWith("{ \"") || t.startsWith("[{\"") || t.startsWith("{\\\"")) return true
        if (t.contains("dangerousOriginBypassScriptNonce", ignoreCase = true)) return true
        if (t.contains("statsig::", ignoreCase = true)) return true
        if (t.contains("layer_exposure", ignoreCase = true)) return true
        if (t.contains("emailFieldPresentation", ignoreCase = true)) return true
        if (t.contains("auth_login_signup_opt", ignoreCase = true)) return true
        if (t.contains("holdout-stable-id", ignoreCase = true)) return true
        if (t.contains("\"key\":", ignoreCase = true) && t.contains("\"value\":", ignoreCase = true)) return true
        if (t.contains("webpackChunk", ignoreCase = true) || t.contains("__NEXT_DATA__", ignoreCase = true)) return true
        if (t.contains("AF_initDataCallback", ignoreCase = true) || t.contains("WLS_state", ignoreCase = true)) return true

        // Reject strings that are primarily raw JSON punctuation without sentence structure
        val jsonPunctuation = t.count { it == '{' || it == '}' || it == '\\' || it == '"' || it == ':' }
        if (t.length > 80 && (jsonPunctuation.toFloat() / t.length.toFloat()) > 0.12f) return true

        return false
    }

    /**
     * Extracts structured AI conversation data directly on-device.
     */
    suspend fun extractFromDevice(
        context: Context,
        rawUrl: String,
        timeoutMs: Long = 12000
    ): Result<AiConversationData> {
        val trimmed = rawUrl.trim()
        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }

        // 1. Direct ChatGPT API Extraction on-device
        val chatGptMatch = Regex("/share/([a-zA-Z0-9_-]{10,80})", RegexOption.IGNORE_CASE).find(normalizedUrl)
        if (chatGptMatch != null) {
            val shareId = chatGptMatch.groupValues[1]
            val chatGptRes = extractChatGPTDirect(normalizedUrl, shareId)
            if (chatGptRes.isSuccess && chatGptRes.getOrNull()?.messages?.isNotEmpty() == true) {
                return chatGptRes
            }
        }

        // 2. Direct fast HTTP extraction for standard web pages
        try {
            val fastRes = extractViaHttp(normalizedUrl)
            if (fastRes.isSuccess && fastRes.getOrNull()?.messages?.isNotEmpty() == true) {
                return fastRes
            }
        } catch (_: Exception) {}

        // 3. Fallback to headless WebView to handle client-side rendering (Google Gemini / Claude SPAs)
        return extractViaWebView(context, normalizedUrl, timeoutMs)
    }

    /**
     * Native on-device call to ChatGPT official share backend API.
     * Returns 100% structured turns without webpage clutter or scripts.
     */
    private suspend fun extractChatGPTDirect(sourceUrl: String, shareId: String): Result<AiConversationData> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://chatgpt.com/backend-api/share/$shareId"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", DESKTOP_UA)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Cache-Control", "no-cache")
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
            }

            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return@withContext Result.failure(Exception("ChatGPT share API returned HTTP $code"))
            }

            val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(rawJson)
            val title = root.optString("title", "ChatGPT Exam Notes").trim().ifBlank { "ChatGPT Exam Notes" }
            val msgList = mutableListOf<AiChatMessage>()

            val linearConv = root.optJSONArray("linear_conversation")
            if (linearConv != null) {
                for (i in 0 until linearConv.length()) {
                    val item = linearConv.optJSONObject(i) ?: continue
                    val msgObj = item.optJSONObject("message") ?: continue
                    val authorObj = msgObj.optJSONObject("author")
                    val role = authorObj?.optString("role") ?: continue
                    if (role != "user" && role != "assistant") continue

                    val metaObj = msgObj.optJSONObject("metadata")
                    if (metaObj?.optBoolean("is_visually_hidden_from_conversation") == true) continue

                    val contentObj = msgObj.optJSONObject("content") ?: continue
                    val contentType = contentObj.optString("content_type")
                    if (contentType == "thoughts" || contentType == "code" || contentType == "reasoning_recap") continue

                    var text = ""
                    val parts = contentObj.optJSONArray("parts")
                    if (parts != null) {
                        val partStrings = mutableListOf<String>()
                        for (p in 0 until parts.length()) {
                            val partVal = parts.opt(p)
                            if (partVal is String && partVal.isNotBlank()) {
                                partStrings.add(partVal)
                            }
                        }
                        text = partStrings.joinToString("\n\n")
                    } else {
                        text = contentObj.optString("text", "")
                    }

                    val cleaned = text.trim()
                    if (cleaned.isNotBlank() && !isGarbageScriptOrTelemetry(cleaned)) {
                        msgList.add(
                            AiChatMessage(
                                id = msgObj.optString("id", "msg-${msgList.size + 1}"),
                                role = role,
                                authorName = if (role == "user") "USER" else "ASSISTANT",
                                content = cleaned
                            )
                        )
                    }
                }
            }

            if (msgList.isNotEmpty()) {
                val uCount = msgList.count { it.role == "user" }
                val aCount = msgList.count { it.role == "assistant" }
                val estPages = calculateEstimatedPages(msgList)
                return@withContext Result.success(
                    AiConversationData(
                        id = shareId,
                        title = title,
                        sourceUrl = sourceUrl,
                        messages = msgList,
                        userCount = uCount,
                        assistantCount = aCount,
                        totalCount = msgList.size,
                        estimatedPages = estPages
                    )
                )
            }

            Result.failure(Exception("No readable conversation turns found in ChatGPT response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractViaWebView(
        context: Context,
        targetUrl: String,
        timeoutMs: Long
    ): Result<AiConversationData> = withContext(Dispatchers.Main) {
        val extraction = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Result<AiConversationData>> { cont ->
                var isDone = false
                var webView: WebView? = null

                fun finishWithData(data: AiConversationData) {
                    if (isDone) return
                    isDone = true
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}
                    cont.resume(Result.success(data))
                }

                fun finishWithError(err: String) {
                    if (isDone) return
                    isDone = true
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}
                    cont.resume(Result.failure(Exception(err)))
                }

                cont.invokeOnCancellation {
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}
                }

                try {
                    webView = WebView(context.applicationContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = DESKTOP_UA
                        settings.loadsImagesAutomatically = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                            }
                        }

                        loadUrl(targetUrl)
                    }

                    // Polling loop to inspect rendered DOM
                    val job = launch(Dispatchers.Main) {
                        var attempts = 0
                        while (isActive && !isDone && attempts < 14) {
                            delay(800)
                            attempts++
                            val currentWv = webView ?: break

                            val jsInspect = """
                                (function() {
                                    try {
                                        // 1. Remove non-content junk elements from DOM
                                        var junk = document.querySelectorAll('script, style, noscript, meta, link, header, footer, nav');
                                        for (var k = 0; k < junk.length; k++) {
                                            try { junk[k].parentNode.removeChild(junk[k]); } catch(e) {}
                                        }

                                        var title = document.title || '';
                                        var userNodes = document.querySelectorAll('.query-content, [data-test-id="user-query"], .user-query, message-content.user-message, div[data-message-author-role="user"], [data-message-author-role="user"]');
                                        var modelNodes = document.querySelectorAll('.model-response, [data-test-id="model-response"], .model-response-text, .response-content, message-content.model-message, div[data-message-author-role="model"], div[data-message-author-role="assistant"], [data-message-author-role="assistant"]');
                                        
                                        var users = [];
                                        for (var i = 0; i < userNodes.length; i++) {
                                            var u = userNodes[i].innerText ? userNodes[i].innerText.trim() : '';
                                            if (u.length > 0 && !u.startsWith('{"') && !u.startsWith('{\\"')) users.push(u);
                                        }
                                        
                                        var models = [];
                                        for (var j = 0; j < modelNodes.length; j++) {
                                            var m = modelNodes[j].innerText ? modelNodes[j].innerText.trim() : '';
                                            if (m.length > 0 && !m.startsWith('{"') && !m.startsWith('{\\"')) models.push(m);
                                        }
                                        
                                        // If specific CSS classes didn't match, inspect readable article or paragraphs
                                        var paras = [];
                                        if (users.length === 0 && models.length === 0) {
                                            var pNodes = document.querySelectorAll('article p, main p, .conversation p');
                                            for (var p = 0; p < pNodes.length; p++) {
                                                var pt = pNodes[p].innerText ? pNodes[p].innerText.trim() : '';
                                                if (pt.length > 25 && !pt.startsWith('{"') && !pt.startsWith('{\\"')) {
                                                    paras.push(pt);
                                                }
                                            }
                                        }

                                        return JSON.stringify({
                                            title: title,
                                            users: users,
                                            models: models,
                                            paras: paras
                                        });
                                    } catch (e) {
                                        return JSON.stringify({ error: e.toString() });
                                    }
                                })();
                            """.trimIndent()

                            currentWv.evaluateJavascript(jsInspect) { resJson ->
                                if (isDone) return@evaluateJavascript
                                try {
                                    val cleanedRaw = if (resJson.startsWith("\"") && resJson.endsWith("\"")) {
                                        JSONObject("{\"raw\":$resJson}").getString("raw")
                                    } else {
                                        resJson
                                    }

                                    val obj = JSONObject(cleanedRaw)
                                    val titleRaw = obj.optString("title", "AI Study Notes")
                                    val cleanTitle = titleRaw.replace("- Gemini", "").replace("Google Gemini -", "").replace("‎", "").trim()
                                        .ifBlank { "Extracted AI Conversation" }

                                    val userArr = obj.optJSONArray("users")
                                    val modelArr = obj.optJSONArray("models")

                                    val msgList = mutableListOf<AiChatMessage>()

                                    if (userArr != null && modelArr != null && (userArr.length() > 0 || modelArr.length() > 0)) {
                                        val maxLen = maxOf(userArr.length(), modelArr.length())
                                        for (k in 0 until maxLen) {
                                            if (k < userArr.length()) {
                                                val uText = userArr.getString(k).trim()
                                                if (uText.isNotBlank() && !isGarbageScriptOrTelemetry(uText)) {
                                                    msgList.add(
                                                        AiChatMessage(
                                                            id = "u-$k",
                                                            role = "user",
                                                            authorName = "USER",
                                                            content = uText
                                                        )
                                                    )
                                                }
                                            }
                                            if (k < modelArr.length()) {
                                                val mText = modelArr.getString(k).trim()
                                                if (mText.isNotBlank() && !isGarbageScriptOrTelemetry(mText)) {
                                                    msgList.add(
                                                        AiChatMessage(
                                                            id = "a-$k",
                                                            role = "assistant",
                                                            authorName = "ASSISTANT",
                                                            content = mText
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Fallback to verified readable paragraph turns
                                    if (msgList.isEmpty()) {
                                        val paraArr = obj.optJSONArray("paras")
                                        if (paraArr != null && paraArr.length() > 0) {
                                            val validParas = mutableListOf<String>()
                                            for (idx in 0 until paraArr.length()) {
                                                val pStr = paraArr.getString(idx).trim()
                                                if (pStr.length > 25 && !isGarbageScriptOrTelemetry(pStr)) {
                                                    validParas.add(pStr)
                                                }
                                            }
                                            if (validParas.isNotEmpty()) {
                                                msgList.add(
                                                    AiChatMessage(
                                                        id = "u-main",
                                                        role = "user",
                                                        authorName = "USER",
                                                        content = cleanTitle
                                                    )
                                                )
                                                msgList.add(
                                                    AiChatMessage(
                                                        id = "a-main",
                                                        role = "assistant",
                                                        authorName = "ASSISTANT",
                                                        content = validParas.joinToString("\n\n")
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (msgList.isNotEmpty()) {
                                        val estPages = calculateEstimatedPages(msgList)
                                        val uCount = msgList.count { it.role == "user" }
                                        val aCount = msgList.count { it.role == "assistant" }
                                        val convData = AiConversationData(
                                            id = UUID.randomUUID().toString(),
                                            title = cleanTitle,
                                            sourceUrl = targetUrl,
                                            messages = msgList,
                                            userCount = uCount,
                                            assistantCount = aCount,
                                            totalCount = msgList.size,
                                            estimatedPages = estPages
                                        )
                                        finishWithData(convData)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }

                } catch (e: Exception) {
                    finishWithError("Failed to initialize extractor: ${e.message}")
                }
            }
        }

        extraction ?: Result.failure(Exception("Extraction timed out. Please check your internet connection or paste text directly."))
    }

    private suspend fun extractViaHttp(urlStr: String): Result<AiConversationData> = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", DESKTOP_UA)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                connectTimeout = 6000
                readTimeout = 6000
                instanceFollowRedirects = true
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val titleMatch = Regex("<title>([^<]*)</title>", RegexOption.IGNORE_CASE).find(html)
            val title = titleMatch?.groupValues?.get(1)
                ?.replace("- Gemini", "")
                ?.replace("Google Gemini -", "")
                ?.replace("‎", "")
                ?.trim()
                ?.ifBlank { "AI Shared Chat" }
                ?: "AI Shared Chat"

            // Look for paragraphs in the HTML, strictly discarding any JSON telemetry
            val pMatches = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL).findAll(html)
                .map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }
                .filter { it.length > 30 && !isGarbageScriptOrTelemetry(it) }
                .toList()

            if (pMatches.size >= 2) {
                val msgs = listOf(
                    AiChatMessage("u-0", "user", "USER", title),
                    AiChatMessage("a-0", "assistant", "ASSISTANT", pMatches.joinToString("\n\n"))
                )
                val conv = AiConversationData(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    sourceUrl = urlStr,
                    messages = msgs,
                    userCount = 1,
                    assistantCount = 1,
                    totalCount = msgs.size,
                    estimatedPages = calculateEstimatedPages(msgs)
                )
                return@withContext Result.success(conv)
            }

            Result.failure(Exception("No readable conversation found via HTTP"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
