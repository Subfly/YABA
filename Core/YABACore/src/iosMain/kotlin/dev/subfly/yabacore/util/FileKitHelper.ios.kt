package dev.subfly.yabacore.util

actual object FileKitHelper {
    actual fun init(platformContext: Any?) {
        // Apple: FileKit works without explicit initialization.
    }
}
