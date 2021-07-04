package io.github.gnuf0rce.steam.data

import kotlinx.serialization.*
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class UserData(
    @SerialName("rgCreatorsFollowed")
    val creatorsFollowed: Set<Long>,
    @SerialName("rgCreatorsIgnored")
    val creatorsIgnored: JsonSet,
    @SerialName("rgCurators")
    val curators: List<Curator>,
    @SerialName("rgCuratorsIgnored")
    val curatorsIgnored: JsonSet,
    @SerialName("rgFollowedApps")
    val followedApps: Set<Long>,
    @SerialName("rgIgnoredApps")
    val ignoredApps: JsonSet,
    @SerialName("rgIgnoredPackages")
    val ignoredPackages: Set<Long>,
    @SerialName("rgOwnedApps")
    val ownedApps: Set<Long>,
    @SerialName("rgOwnedPackages")
    val ownedPackages: Set<Long>,
    @SerialName("rgWishlist")
    val wishlist: Set<Long>
)

@Serializable
data class Curator(
    @SerialName("avatar")
    val avatar: String,
    @SerialName("clanid")
    val clanid: Int,
    @SerialName("name")
    val name: String
)

@Serializable(JsonSet.Companion::class)
class JsonSet(origin: Set<Long>) : Set<Long> by origin {

    companion object : KSerializer<JsonSet> {
        override val descriptor: SerialDescriptor =
            buildSerialDescriptor(JsonSet::class.qualifiedName!!, StructureKind.LIST)

        override fun deserialize(decoder: Decoder): JsonSet {
            return when (val json = decoder.decodeSerializableValue(JsonElement.serializer())) {
                is JsonObject -> JsonSet(origin = json.keys.map { it.toLong() }.toSet())
                is JsonArray -> JsonSet(origin = json.map { it.jsonPrimitive.long }.toSet())
                else -> throw IllegalArgumentException(json.toString())
            }
        }

        override fun serialize(encoder: Encoder, value: JsonSet) {
            if (value.isEmpty()) {
                encoder.encodeSerializableValue(SetSerializer(Long.serializer()), value)
            } else {
                encoder.encodeSerializableValue(JsonObject.serializer(), buildJsonObject {
                    value.forEach { put(it.toString(), 0) }
                })
            }
        }
    }
}