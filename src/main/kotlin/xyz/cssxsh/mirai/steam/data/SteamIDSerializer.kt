package xyz.cssxsh.mirai.steam.data

import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

public object SteamIDSerializer : KSerializer<SteamID> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SteamID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SteamID {
        return SteamID().apply {
            val text = decoder.decodeString()
            check(setFromSteam3String(text)) {
                "SteamID $text error"
            }
        }
    }

    override fun serialize(encoder: Encoder, value: SteamID) {
        encoder.encodeString(value.render(true))
    }
}