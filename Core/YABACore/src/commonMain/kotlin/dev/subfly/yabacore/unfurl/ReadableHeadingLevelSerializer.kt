package dev.subfly.yabacore.unfurl

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

object ReadableHeadingLevelSerializer : KSerializer<ReadableHeadingLevel> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReadableHeadingLevel", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ReadableHeadingLevel) {
        val level = when (value) {
            ReadableHeadingLevel.H1 -> 1
            ReadableHeadingLevel.H2 -> 2
            ReadableHeadingLevel.H3 -> 3
            ReadableHeadingLevel.H4 -> 4
            ReadableHeadingLevel.H5 -> 5
            ReadableHeadingLevel.H6 -> 6
        }
        encoder.encodeInt(level)
    }

    override fun deserialize(decoder: Decoder): ReadableHeadingLevel {
        val jsonDecoder = decoder as? JsonDecoder
        val element = jsonDecoder?.decodeJsonElement() as? JsonPrimitive
        if (element != null) {
            element.intOrNull?.let { return ReadableHeadingLevel.fromInt(it) }
            element.contentOrNull?.toIntOrNull()?.let { return ReadableHeadingLevel.fromInt(it) }
        }

        val raw = runCatching { decoder.decodeInt() }.getOrNull() ?: 3
        return ReadableHeadingLevel.fromInt(raw)
    }
}
