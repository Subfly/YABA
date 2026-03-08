package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.dialogs.shareFile

internal actual suspend fun platformShareFile(file: PlatformFile) {
    FileKit.shareFile(file)
}

internal actual suspend fun platformShareFiles(files: List<PlatformFile>) {
    FileKit.shareFile(files)
}

internal actual suspend fun platformCapturePhoto(): PlatformFile? =
    FileKit.openCameraPicker()
