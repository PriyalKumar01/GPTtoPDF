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
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    /**
     * Checks if a string is promotional placeholder / carousel teaser from search engines.
     */
    fun isPromotionalOrCarousel(text: String): Boolean {
        val t = text.trim()
        if (t.contains("Generate an image of a futuristic car", ignoreCase = true)) return true
        if (t.contains("Rewrite this email draft", ignoreCase = true)) return true
        if (t.contains("Give me a list of 5 well-known sci-fi books", ignoreCase = true)) return true
        if (t.contains("Give me caption options to tell the world about my new fur baby", ignoreCase = true)) return true
        if (t.contains("The Neo-Glam Revival", ignoreCase = true)) return true
        if (t.contains("How to Set Up the Perfect Movie Marathon Night", ignoreCase = true)) return true
        if (t.contains("WIZ_global_data", ignoreCase = true)) return true
        return false
    }

    /**
     * Checks if a string contains internal script tags, telemetry JSON, or garbage config.
     */
    fun isGarbageScriptOrTelemetry(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return true
        if (isPromotionalOrCarousel(t)) return true
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

        // 1. Direct ChatGPT API Extraction on-device (Only for chatgpt.com domain)
        val chatGptMatch = Regex("chatgpt\\.com/share/([a-zA-Z0-9_-]{10,80})", RegexOption.IGNORE_CASE).find(normalizedUrl)
        if (chatGptMatch != null) {
            val shareId = chatGptMatch.groupValues[1]
            val chatGptRes = extractChatGPTDirect(normalizedUrl, shareId)
            if (chatGptRes.isSuccess && chatGptRes.getOrNull()?.messages?.isNotEmpty() == true) {
                return chatGptRes
            }
        }

        // 2. Direct fast HTTP extraction for standard web pages (SKIP for Gemini / Claude / ChatGPT SPAs)
        val isSpaUrl = normalizedUrl.contains("gemini.google", ignoreCase = true) ||
                       normalizedUrl.contains("claude.ai", ignoreCase = true) ||
                       normalizedUrl.contains("chatgpt.com", ignoreCase = true)
        if (!isSpaUrl) {
            try {
                val fastRes = extractViaHttp(normalizedUrl)
                if (fastRes.isSuccess && fastRes.getOrNull()?.messages?.isNotEmpty() == true) {
                    return fastRes
                }
            } catch (_: Exception) {}
        }

        // 3. Headless WebView to handle client-side rendering (Google Gemini / Claude SPAs)
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
                        settings.databaseEnabled = true
                        settings.userAgentString = DESKTOP_UA
                        settings.loadsImagesAutomatically = true

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
                        while (isActive && !isDone && attempts < 36) {
                            delay(700)
                            attempts++
                            val currentWv = webView ?: break

                            val jsInspect = """
                                (function() {
                                    try {
                                        var isClaudeLogin = false;
                                        try {
                                            var curHref = window.location.href || '';
                                            if (curHref.indexOf('claude.ai/login') !== -1 || curHref.indexOf('returnTo=') !== -1) {
                                                isClaudeLogin = true;
                                            }
                                        } catch(e) {}

                                        var title = '';
                                        var titleEl = document.querySelector('share-viewer h1, h1.headline, .share-title-section h1, h1');
                                        if (titleEl && titleEl.innerText && titleEl.innerText.trim()) {
                                            title = titleEl.innerText.trim();
                                        }
                                        if (!title) {
                                            var dt = document.title || '';
                                            title = dt.replace(/^[\s\u200E\u200F]+/, '').replace(/[-|].*$/, '').replace(/Gemini\s*-\s*/i, '').replace(/Claude\s*-\s*/i, '').trim();
                                        }

                                        var users = [];
                                        var models = [];
                                        var ordered = [];

                                        // 1. Check Claude __NEXT_DATA__ JSON payload & scripts
                                        var nextDataEl = document.getElementById('__NEXT_DATA__');
                                        if (nextDataEl && nextDataEl.textContent) {
                                            try {
                                                var nd = JSON.parse(nextDataEl.textContent);
                                                var chat = nd.props && nd.props.pageProps ? (nd.props.pageProps.sharedConversation || nd.props.pageProps.sharedChat || nd.props.pageProps.chat) : null;
                                                if (chat) {
                                                    if (chat.name || chat.title) title = chat.name || chat.title;
                                                    var chatMsgs = chat.chat_messages || chat.messages || [];
                                                    for (var mi = 0; mi < chatMsgs.length; mi++) {
                                                        var cm = chatMsgs[mi];
                                                        var role = cm.sender === 'human' ? 'user' : 'assistant';
                                                        var text = cm.text || (Array.isArray(cm.content) ? cm.content.map(function(c) { return c.text || ''; }).join('\n') : '');
                                                        if (text && text.trim().length > 0) {
                                                            ordered.push({ role: role, text: text.trim() });
                                                            if (role === 'user') users.push(text.trim());
                                                            else models.push(text.trim());
                                                        }
                                                    }
                                                }
                                            } catch(e) {}
                                        }

                                        // 2. Check Google Gemini Lit web components (Strictly isolate share-viewer, ignore promo carousel)
                                        if (ordered.length === 0 && users.length === 0 && models.length === 0) {
                                            var shareViewer = document.querySelector('share-viewer') || document.querySelector('main');
                                            if (shareViewer) {
                                                var turns = shareViewer.querySelectorAll('share-turn-viewer, .share-turn, .conversation-turn');
                                                if (turns && turns.length > 0) {
                                                    for (var t = 0; t < turns.length; t++) {
                                                        var turnEl = turns[t];
                                                        var uEl = turnEl.querySelector('.query-text, user-query .query-text, .user-query-container, .query-content, user-query');
                                                        var mEl = turnEl.querySelector('message-content .markdown-main-panel, message-content .markdown, response-container message-content, message-content, .model-response, div.markdown');
                                                        
                                                        // Pierce ShadowRoot if present
                                                        if (!uEl && turnEl.shadowRoot) uEl = turnEl.shadowRoot.querySelector('.query-text, user-query, .query-content');
                                                        if (!mEl && turnEl.shadowRoot) mEl = turnEl.shadowRoot.querySelector('message-content, .markdown, .model-response');

                                                        var uText = uEl && (uEl.innerText || uEl.textContent) ? (uEl.innerText || uEl.textContent).trim() : '';
                                                        var mText = mEl && (mEl.innerText || mEl.textContent) ? (mEl.innerText || mEl.textContent).trim() : '';

                                                        // Ignore Google promotional carousel teaser cards
                                                        if (mText.indexOf("Sure, here is an image of a futuristic car") !== -1 ||
                                                            mText.indexOf("The leaf in the image is covered in rust") !== -1 ||
                                                            mText.indexOf("new fur baby, Finn") !== -1) {
                                                            continue;
                                                        }

                                                        if (uText.length > 0 && !uText.startsWith('{"') && !uText.includes('WIZ_global_data')) {
                                                            ordered.push({ role: 'user', text: uText });
                                                            users.push(uText);
                                                        }
                                                        if (mText.length > 0 && !mText.startsWith('{"') && !mText.includes('WIZ_global_data')) {
                                                            ordered.push({ role: 'assistant', text: mText });
                                                            models.push(mText);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2b. Gemini WIZ_global_data data payload fallback
                                        if (ordered.length === 0 && users.length === 0 && models.length === 0 && window.WIZ_global_data) {
                                            try {
                                                for (var wKey in window.WIZ_global_data) {
                                                    var wVal = window.WIZ_global_data[wKey];
                                                    if (typeof wVal === 'string' && wVal.indexOf('∰') !== -1) {
                                                        var gTurns = wVal.split('∰');
                                                        for (var gk = 0; gk < gTurns.length; gk++) {
                                                            var gParts = gTurns[gk].split('∞');
                                                            if (gParts.length >= 2) {
                                                                var gu = gParts[0].trim();
                                                                var gm = gParts[gParts.length - 1].replace(/<[^>]*>/g, '').trim();
                                                                if (gu.length > 0 && gu.indexOf('http') === -1) {
                                                                    ordered.push({ role: 'user', text: gu });
                                                                    users.push(gu);
                                                                }
                                                                if (gm.length > 0 && gm.indexOf('http') === -1) {
                                                                    ordered.push({ role: 'assistant', text: gm });
                                                                    models.push(gm);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch(_wErr) {}
                                        }

                                        // 3. DOM selectors for standard rendered turns (ChatGPT, Claude web, etc. - skip on Gemini to avoid carousel)
                                        var isGeminiDomain = window.location.hostname.indexOf('gemini.google') !== -1 || document.querySelector('share-viewer') !== null;
                                        if (!isGeminiDomain && ordered.length === 0 && users.length === 0 && models.length === 0) {
                                            var userNodes = document.querySelectorAll('.query-content, [data-test-id="user-query"], .user-query, message-content.user-message, div[data-message-author-role="user"], [data-message-author-role="user"], .font-user-message, div[data-testid="user-message"]');
                                            var modelNodes = document.querySelectorAll('.model-response, [data-test-id="model-response"], .model-response-text, .response-content, message-content, message-content.model-message, div[data-message-author-role="model"], div[data-message-author-role="assistant"], [data-message-author-role="assistant"], .font-claude-message, div[data-testid="claude-message"], .prose');
                                            
                                            for (var i = 0; i < userNodes.length; i++) {
                                                var u = userNodes[i].innerText ? userNodes[i].innerText.trim() : '';
                                                if (u.length > 0 && !u.startsWith('{"') && !u.startsWith('{\\"')) {
                                                    ordered.push({ role: 'user', text: u });
                                                    users.push(u);
                                                }
                                            }
                                            
                                            for (var j = 0; j < modelNodes.length; j++) {
                                                var m = modelNodes[j].innerText ? modelNodes[j].innerText.trim() : '';
                                                if (m.length > 0 && !m.startsWith('{"') && !m.startsWith('{\\"')) {
                                                    ordered.push({ role: 'assistant', text: m });
                                                    models.push(m);
                                                }
                                            }
                                        }

                                        // 4. Fallback: inspect readable article or paragraphs
                                        var paras = [];
                                        if (ordered.length === 0 && users.length === 0 && models.length === 0) {
                                            var pNodes = document.querySelectorAll('article p, main p, .conversation p, .chat-history p');
                                            for (var p = 0; p < pNodes.length; p++) {
                                                var pt = pNodes[p].innerText ? pNodes[p].innerText.trim() : '';
                                                if (pt.length > 25 && !pt.startsWith('{"') && !pt.startsWith('{\\"') && !pt.includes('WIZ_global_data')) {
                                                    paras.push(pt);
                                                }
                                            }
                                        }

                                        return JSON.stringify({
                                            isClaudeLogin: isClaudeLogin,
                                            title: title,
                                            ordered: ordered,
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
                                    val userArr = obj.optJSONArray("users")
                                    val modelArr = obj.optJSONArray("models")
                                    val rawTitle = obj.optString("title", "").trim()
                                    val cleanTitle = if (rawTitle.isNotBlank() && !isGarbageScriptOrTelemetry(rawTitle)) rawTitle else "AI Conversation"

                                    if (obj.optBoolean("isClaudeLogin", false) && (userArr == null || userArr.length() == 0) && (modelArr == null || modelArr.length() == 0)) {
                                        finishWithError("Claude requires an active account login to view shared links. Please paste your conversation text directly into the 'Direct Notes' tab!")
                                        return@evaluateJavascript
                                    }

                                    val msgList = mutableListOf<AiChatMessage>()
                                    val orderedArr = obj.optJSONArray("ordered")

                                    if (orderedArr != null && orderedArr.length() > 0) {
                                        for (k in 0 until orderedArr.length()) {
                                            val oObj = orderedArr.getJSONObject(k)
                                            val role = oObj.optString("role", "assistant")
                                            val txt = oObj.optString("text", "").trim()
                                            if (txt.isNotBlank() && !isGarbageScriptOrTelemetry(txt)) {
                                                msgList.add(
                                                    AiChatMessage(
                                                        id = "msg-$k",
                                                        role = if (role == "user") "user" else "assistant",
                                                        authorName = if (role == "user") "USER" else "ASSISTANT",
                                                        content = txt
                                                    )
                                                )
                                            }
                                        }
                                    } else if (userArr != null && modelArr != null && (userArr.length() > 0 || modelArr.length() > 0)) {
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
