package net.commandblock2.liquidbounce.expr.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.commandblock2.liquidbounce.expr.utils.extensions.multiply
import net.minecraft.client.settings.GameSettings
import net.minecraft.util.Vec3

@ModuleInfo(
    name = "ExperimentalFreeCam",
    description = "A Real FreeCam", category = ModuleCategory.EXPERIMENTAL
)
class ExperimentalFreeCam : Module() {
    private val speedValue = FloatValue("Speed", .8F, .1F, 2F)

    var pos: Vec3? = null
        get() = field ?: mc.thePlayer.positionVector
        private set

    var lastTickPos: Vec3? = null


    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) {
        if (mc.thePlayer != null && pos == null)
            pos = mc.thePlayer.positionVector

        lastTickPos = pos

        mc.gameSettings.keyBindForward.pressed = false
        mc.gameSettings.keyBindBack.pressed = false
        mc.gameSettings.keyBindRight.pressed = false
        mc.gameSettings.keyBindLeft.pressed = false
        mc.gameSettings.keyBindJump.pressed = false
        mc.gameSettings.keyBindSneak.pressed = false


        val rotation = Rotation(mc.thePlayer.rotationYaw, 0F)

        var offset = Vec3(.0, .0, .0)

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
            offset = offset.add(RotationUtils.getVectorForRotation(rotation))

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindBack))
            offset = offset.add(
                RotationUtils.getVectorForRotation(rotation)
                    .rotateYaw((Math.PI).toFloat())
            )

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
            offset = offset.add(
                RotationUtils.getVectorForRotation(rotation)
                    .rotateYaw((Math.PI / 2).toFloat())
            )

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindRight))
            offset = offset.add(
                RotationUtils.getVectorForRotation(rotation)
                    .rotateYaw((Math.PI / -2).toFloat())
            )

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindJump))
            offset = offset.addVector(.0, speedValue.get().toDouble(), .0)

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
            offset = offset.addVector(.0, -speedValue.get().toDouble(), .0)

        pos = pos?.add((offset).multiply(speedValue.get()))
    }

    override fun onDisable() {
        pos = null
        lastTickPos = null

        mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
        mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
        mc.gameSettings.keyBindRight.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
        mc.gameSettings.keyBindLeft.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSprint)
    }

    fun offset(partialTick: Float): Vec3 {
        return mc.thePlayer.positionVector
            .subtract(pos)
            .add(
                pos?.subtract(lastTickPos ?: pos)
                    ?.multiply(1 - partialTick)
            )
            .subtract(
                mc.thePlayer.positionVector.subtract(
                    mc.thePlayer.prevPosX,
                    mc.thePlayer.prevPosY,
                    mc.thePlayer.prevPosZ
                ).multiply(1 - partialTick)
            )
    }
}