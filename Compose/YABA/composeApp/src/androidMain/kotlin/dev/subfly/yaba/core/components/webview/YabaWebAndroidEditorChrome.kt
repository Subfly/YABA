package dev.subfly.yaba.core.components.webview

import android.Manifest
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.MutableState

/**
 * Hardened settings and permissions are taken from:
 * https://github.com/GrapheneOS/PdfViewer
 */

internal fun editorWebChromeClient(
    onProgressChanged: (Int) -> Unit,
    pendingPermissionRequestRef: MutableState<Pair<PermissionRequest, Array<String>>?>,
    onLaunchPermissionRequest: (Array<String>) -> Unit,
    onEditorHostEvent: (String) -> Unit = {},
): WebChromeClient = object : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        val permissions = mutableListOf<String>()
        request.resources.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    permissions.add(Manifest.permission.CAMERA)
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                else -> Unit
            }
        }
        val uniquePermissions = permissions.distinct()
        if (uniquePermissions.isEmpty()) {
            request.deny()
            return
        }
        pendingPermissionRequestRef.value = request to uniquePermissions.toTypedArray()
        onLaunchPermissionRequest(uniquePermissions.toTypedArray())
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        callback?.invoke(origin, false, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            onEditorHostEvent(consoleMessage.message())
            Log.d(
                YABA_WEBVIEW_LOG_TAG,
                "JS ${consoleMessage.messageLevel()} ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}",
            )
        }
        return super.onConsoleMessage(consoleMessage)
    }
}
