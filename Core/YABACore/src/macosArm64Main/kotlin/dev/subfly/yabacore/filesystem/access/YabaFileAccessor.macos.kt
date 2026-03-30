package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication

internal actual suspend fun platformShareFile(file: PlatformFile) {
    FileKit.openFileWithDefaultApplication(file)
}

internal actual suspend fun platformShareFiles(files: List<PlatformFile>) {
    files.firstOrNull()?.let { FileKit.openFileWithDefaultApplication(it) }
}

internal actual suspend fun platformCapturePhoto(): PlatformFile? =
    FileKit.openFilePicker(
        type = FileKitType.Image,
        directory = null,
        dialogSettings = FileKitDialogSettings.createDefault(),
    )
