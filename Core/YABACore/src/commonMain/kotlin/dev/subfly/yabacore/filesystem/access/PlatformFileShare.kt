package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.PlatformFile

internal expect suspend fun platformShareFile(file: PlatformFile)
internal expect suspend fun platformShareFiles(files: List<PlatformFile>)
internal expect suspend fun platformCapturePhoto(): PlatformFile?
