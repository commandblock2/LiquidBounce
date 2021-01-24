/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */


package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.block.BlockAir
import net.minecraft.util.AxisAlignedBB

class Tunnel : SpeedMode("Tunnel") {

    override fun onEnable() {
    }

    override fun onMotion() {

    }

    override fun onMove(event: MoveEvent) {

    }

    override fun onUpdate() {
        if (MovementUtils.isMoving() && mc.thePlayer.onGround)
            mc.thePlayer.jump();
    }

    override fun onBB(event: BlockBBEvent) {
        if (event.block is BlockAir && event.y > mc.thePlayer.posY + mc.thePlayer.height)
            event.boundingBox = AxisAlignedBB.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.toDouble(), Math.floor(mc.thePlayer.posY) + 2, event.z + 1.toDouble())
    }
}