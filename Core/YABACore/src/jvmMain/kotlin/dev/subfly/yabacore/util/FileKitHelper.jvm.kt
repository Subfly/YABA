package dev.subfly.yabacore.util

import io.github.vinceglb.filekit.FileKit

actual object FileKitHelper {
    actual fun init(platformContext: Any?) {
        FileKit.init(appId = "dev.subfly.yaba")
    }
}
