package net.commandblock2.liquidbounce.expr.value

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.Value
import net.commandblock2.liquidbounce.expr.utils.TargetingPolicy
import java.util.function.Consumer

open class TargetValue(name: String, value: TargetingPolicy) : Value<TargetingPolicy>(name, value) {

    constructor(name: String) : this(name, TargetingPolicy())

    enum class InheritFrom {
        GLOBAL,
        CATEGORY,
        NONE
    }

    var inherit: InheritFrom = InheritFrom.GLOBAL

    fun get(module: Module): TargetingPolicy {
        when (inherit) {
            InheritFrom.NONE -> return get()
            InheritFrom.GLOBAL -> return LiquidBounce.exprimental.globalPolicy
            InheritFrom.CATEGORY -> return LiquidBounce
                .exprimental
                .categoryPolicies[module.category] as TargetingPolicy?
                ?: LiquidBounce.exprimental.globalPolicy
        }
    }

    override fun toJson(): JsonElement? {
        val valueObject = JsonObject()
        value.javaClass.declaredFields
            .forEach {

                if (!it.isAccessible)
                    it.isAccessible = true

                when {
                    it.type == Boolean::class.java -> valueObject.addProperty(it.name, it.get(value) as Boolean)
                    it.isEnumConstant -> valueObject.addProperty(it.name, Gson().toJson(it.get(value)))
                }
                //lambda ?
            }
        val returnValue = JsonObject()
        returnValue.addProperty("inherit", inherit.name)
        returnValue.add("target", valueObject)
        return returnValue
    }

    override fun fromJson(element: JsonElement) {
        if (!element.isJsonObject) return
        inherit = InheritFrom.valueOf(element.asJsonObject["inherit"].asString)
        element.asJsonObject["target"].asJsonObject.entrySet().forEach(Consumer {

            val field = value.javaClass.getDeclaredField(it.key)

            if (!field.isAccessible)
                field.isAccessible = true

            when {
                field.type == Boolean::class.java -> field.setBoolean(value, it.value.asBoolean)
                field.isEnumConstant -> field.set(
                    value,
                    field.declaringClass.getDeclaredField(it.value.asString).get(null)
                )
            }
        })
    }


}