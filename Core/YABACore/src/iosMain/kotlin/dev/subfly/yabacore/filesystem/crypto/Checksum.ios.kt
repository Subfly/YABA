package dev.subfly.yabacore.filesystem.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256

@OptIn(ExperimentalForeignApi::class)
actual fun computeSha256Hex(data: ByteArray): String = memScoped {
    val digestSize = CC_SHA256_DIGEST_LENGTH.convert<Int>()
    val digest = UByteArray(digestSize)
    data.usePinned { pinned ->
        digest.usePinned { digestPinned ->
            CC_SHA256(
                pinned.addressOf(0),
                data.size.convert(),
                digestPinned.addressOf(0),
            )
        }
    }
    digest.joinToString(separator = "") { byte -> byte.toString(16).padStart(2, '0') }
}
