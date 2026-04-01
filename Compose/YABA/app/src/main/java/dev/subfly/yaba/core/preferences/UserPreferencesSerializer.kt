package dev.subfly.yaba.core.preferences

import androidx.datastore.core.Serializer
import dev.subfly.yaba.core.security.Crypto
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.Charsets
import kotlinx.serialization.json.Json

internal object UserPreferencesSerializer : Serializer<UserPreferences> {
    private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val defaultValue: UserPreferences
        get() = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        val raw = withContext(Dispatchers.IO) {
            input.readBytes()
        }
        if (raw.isEmpty()) return defaultValue
        return try {
            val encrypted =
                    Base64.getDecoder().decode(String(raw, Charsets.US_ASCII).trim())
            val decrypted = Crypto.decrypt(encrypted)
            val text = decrypted.decodeToString()
            json.decodeFromString(UserPreferences.serializer(), text)
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            val text = json.encodeToString(UserPreferences.serializer(), t)
            val encrypted = Crypto.encrypt(text.toByteArray(Charsets.UTF_8))
            val base64 = Base64.getEncoder().encodeToString(encrypted)
            output.write(base64.toByteArray(Charsets.US_ASCII))
        }
    }
}
