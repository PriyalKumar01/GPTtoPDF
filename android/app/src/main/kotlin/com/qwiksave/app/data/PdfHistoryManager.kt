package com.qwiksave.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PdfHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedSize(): String {
        return when {
            fileSizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSizeBytes / (1024f * 1024f))
            fileSizeBytes >= 1024 -> String.format(Locale.US, "%d KB", fileSizeBytes / 1024)
            else -> "$fileSizeBytes B"
        }
    }
}

object PdfHistoryManager {
    private const val PREF_NAME = "qwiksave_pdf_history"
    private const val KEY_HISTORY = "recent_pdfs"
    private const val MAX_HISTORY = 12
    private const val EXPIRATION_MS = 48L * 60 * 60 * 1000 // 48 Hours

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun addPdf(context: Context, file: File): PdfHistoryItem {
        val existing = getPdfs(context).toMutableList()
        // Prune entries older than 48 hours
        val cutoff = System.currentTimeMillis() - EXPIRATION_MS
        existing.removeAll { it.timestamp < cutoff }

        // Remove if duplicate path exists
        existing.removeAll { it.filePath == file.absolutePath }

        val newItem = PdfHistoryItem(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSizeBytes = if (file.exists()) file.length() else 0L,
            timestamp = System.currentTimeMillis()
        )
        existing.add(0, newItem)

        // Limit to max 12
        val trimmed = existing.take(MAX_HISTORY)
        saveList(context, trimmed)
        return newItem
    }

    @Synchronized
    fun getPdfs(context: Context): List<PdfHistoryItem> {
        val raw = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<PdfHistoryItem>()
        val cutoff = System.currentTimeMillis() - EXPIRATION_MS
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val item = PdfHistoryItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    filePath = obj.getString("filePath"),
                    fileName = obj.getString("fileName"),
                    fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                // Filter out if expired (48h privacy auto-clear) or file doesn't exist
                if (item.timestamp >= cutoff) {
                    val file = File(item.filePath)
                    if (file.exists()) {
                        list.add(item)
                    }
                }
            }
        } catch (_: Exception) {}

        // Persist pruned list if any items were expired
        saveList(context, list)
        return list
    }

    @Synchronized
    fun deletePdf(context: Context, id: String) {
        val existing = getPdfs(context).toMutableList()
        val target = existing.firstOrNull { it.id == id }
        if (target != null) {
            existing.remove(target)
            saveList(context, existing)
            try {
                val f = File(target.filePath)
                if (f.exists()) {
                    f.delete()
                }
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }

    private fun saveList(context: Context, items: List<PdfHistoryItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("filePath", item.filePath)
                put("fileName", item.fileName)
                put("fileSizeBytes", item.fileSizeBytes)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun openPdf(context: Context, item: PdfHistoryItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                return
            }
            val uri: Uri = FileProvider.getUriForFile(context, "com.qwiksave.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, item: PdfHistoryItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                return
            }
            val uri: Uri = FileProvider.getUriForFile(context, "com.qwiksave.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
