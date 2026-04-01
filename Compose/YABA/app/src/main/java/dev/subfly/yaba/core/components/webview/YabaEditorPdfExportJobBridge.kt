package dev.subfly.yaba.core.components.webview

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject

internal object YabaEditorPdfExportJobBridge {

    private val jobs = ConcurrentHashMap<String, CompletableDeferred<String>>()

    fun register(jobId: String, deferred: CompletableDeferred<String>) {
        jobs[jobId] = deferred
    }

    fun remove(jobId: String) {
        jobs.remove(jobId)
    }

    fun onEditorPdfExportMessage(root: JSONObject) {
        val jobId = root.optString("jobId", "")
        if (jobId.isBlank()) return
        val deferred = jobs[jobId] ?: return
        when (root.optString("status")) {
            "done" -> {
                val b64 = root.optString("pdfBase64", "")
                deferred.complete(b64)
            }
            "error" -> deferred.complete("")
        }
    }
}
