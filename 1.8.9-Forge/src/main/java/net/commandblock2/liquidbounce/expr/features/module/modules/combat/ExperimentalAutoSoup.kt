/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.commandblock2.liquidbounce.expr.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.init.Items
import net.minecraft.network.play.client.*
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.util.*
import kotlin.concurrent.timerTask

@ModuleInfo(
    name = "ExperimentalAutoSoup",
    description = "Makes you automatically eat soup whenever your health is low.",
    category = ModuleCategory.EXPERIMENTAL
)
class ExperimentalAutoSoup : Module() {

    private val healthValue = FloatValue("Health", 5f, 0f, 20f)
    private val delayValue = IntegerValue("Delay", 150, 0, 500)
    private val bowlValue = ListValue("Bowl", arrayOf("Drop"), "Drop")

    private val timer = MSTimer()
    private var currentTimer: Timer? = null

    override val tag: String
        get() = healthValue.get().toString()

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {

        val ogIndex = mc.thePlayer.inventory.currentItem

        val soupInHotbar = InventoryUtils.findItem(36, 45, Items.mushroom_stew)
        if (mc.thePlayer.health <= healthValue.get() && soupInHotbar != -1) {
            mc.thePlayer.inventory.currentItem = soupInHotbar - 36
            mc.rightClickMouse()

            if (currentTimer != null)
                return

            currentTimer = Timer("setTimeout", true)
            currentTimer!!.schedule(timerTask {
                mc.thePlayer.dropOneItem(true)

                currentTimer!!.schedule(timerTask {
                    mc.thePlayer.inventory.currentItem = ogIndex

                    currentTimer!!.schedule(timerTask {
                        currentTimer = null }, delayValue.get().toLong())

                }, delayValue.get().toLong())
            }, delayValue.get().toLong())
        }


        if (mc.currentScreen is GuiInventory && timer.hasTimePassed(delayValue.get().toLong())) {
            val soupInInventory = InventoryUtils.findItem(9, 36, Items.mushroom_stew)
            if (soupInInventory != -1 && InventoryUtils.hasSpaceHotbar()) {

                mc.playerController.windowClick(0, soupInInventory, 0, 1, mc.thePlayer)
                timer.reset()
            }
        }

    }

}