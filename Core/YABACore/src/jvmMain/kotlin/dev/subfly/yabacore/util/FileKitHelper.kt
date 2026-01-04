package dev.subfly.yabacore.util

import io.github.vinceglb.filekit.FileKit

object FileKitHelper {
    fun initialize() {
        FileKit.init(appId = "dev.subfly.yaba")
    }
}