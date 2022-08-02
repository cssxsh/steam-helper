package xyz.cssxsh.mirai.steam.data

import net.mamoe.mirai.console.data.*
import xyz.cssxsh.mirai.steam.*
import kotlin.properties.*
import kotlin.reflect.*

public object SteamAuthData : AutoSavePluginData("SteamAuth"), ReadWriteProperty<SteamHelper, String> {

    @ValueName("login_keys")
    public val keys: MutableMap<Long, String> by value()

    @ValueName("bind")
    public val bind: MutableMap<Long, String> by value()

    override fun getValue(thisRef: SteamHelper, property: KProperty<*>): String {
        return when (property.name) {
            "key" -> keys[thisRef.id].orEmpty()
            "name" -> bind[thisRef.id].orEmpty()
            else -> throw NoSuchElementException(property.name)
        }
    }

    override fun setValue(thisRef: SteamHelper, property: KProperty<*>, value: String) {
        return when (property.name) {
            "key" -> keys[thisRef.id] = value
            "name" -> bind[thisRef.id] = value
            else -> throw NoSuchElementException(property.name)
        }
    }
}