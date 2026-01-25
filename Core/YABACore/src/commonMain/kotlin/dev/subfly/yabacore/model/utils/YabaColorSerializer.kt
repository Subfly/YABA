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

object YabaColorSerializer : KSerializer<YabaColor> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("YabaColor", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: YabaColor) {
        encoder.encodeInt(value.code)
    }

    override fun deserialize(decoder: Decoder): YabaColor {
        val jsonDecoder = decoder as? JsonDecoder
        val element = jsonDecoder?.decodeJsonElement() as? JsonPrimitive
        if (element != null) {
            element.intOrNull?.let { return YabaColor.fromCode(it) }
            element.contentOrNull?.let { return YabaColor.fromRoleString(it) }
        }

        val code = runCatching { decoder.decodeInt() }.getOrNull()
        return code?.let { YabaColor.fromCode(it) } ?: YabaColor.NONE
    }
}
