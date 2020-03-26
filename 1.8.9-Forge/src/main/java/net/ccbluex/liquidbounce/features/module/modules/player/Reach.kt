/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import kotlin.math.abs

@ModuleInfo(name = "Reach", description = "Increases your reach.", category = ModuleCategory.PLAYER)
class Reach : Module() {

    val combatReachValue = FloatValue("CombatReach", 3.5f, 3f, 7f)
    val buildReachValue = FloatValue("BuildReach", 5f, 4.5f, 7f)
    val serverSideCheck = BoolValue("serverSideCheck",true)
    val doNotShorten = BoolValue("doNotShorten",true)
    val serverSideCheckDistance = FloatValue("serverSideCheckDistance",3.0f,2.0f,7f)
    val manuallySpecifiedPing = IntegerValue("manuallySpecifiedPing",0,0,2000)
    val debugging = BoolValue("debugging", true)

    private var ping_history = mutableListOf<Int>()
    val length = 10
    var belief: Float = .0f


    val maxRange: Float
        get() {
            val combatRange = combatReachValue.get()
            val buildRange = buildReachValue.get()

            return if (combatRange > buildRange) combatRange else buildRange
        }

    override fun onEnable()
    {

    }

    override fun onDisable()
    {

    }

    @EventTarget
    fun onRender3D(event: Render3DEvent)
    {
        var updatedping = EntityUtils.getPing(mc.thePlayer)

        if (updatedping < 5) return

        if (ping_history.size == 0)
            ping_history.add(updatedping)

        if (updatedping != ping_history.last())
        {
            ping_history.add(updatedping)
            if (ping_history.size > 10)
                ping_history.removeAt(0)

            var avgping: Int = 0
            belief = .0f
            ping_history.forEach { elem -> avgping += elem/ping_history.size}
            ping_history.forEach { elem -> if (abs(elem - avgping) < 10) {belief += (0.1f)}}

            ClientUtils.displayChatMessage("§3Ping belief §a${belief}§3")
        }
    }

}
