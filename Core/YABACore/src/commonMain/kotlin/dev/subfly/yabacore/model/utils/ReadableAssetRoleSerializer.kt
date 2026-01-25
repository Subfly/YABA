package dev.subfly.yabacore.model.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object ReadableAssetRoleSerializer : KSerializer<ReadableAssetRole> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReadableAssetRole", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ReadableAssetRole) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): ReadableAssetRole {
        val jsonDecoder = decoder as? JsonDecoder
        val element = jsonDecoder?.decodeJsonElement() as? JsonPrimitive
        if (element != null) {
            element.contentOrNull?.let { return ReadableAssetRole.fromRaw(it) }
            element.intOrNull?.let { return ReadableAssetRole.fromRaw(it.toString()) }
        }

        val raw = runCatching { decoder.decodeString() }.getOrNull()
        return ReadableAssetRole.fromRaw(raw)
    }
}
