/*
 * Licensed under GPL-v3
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.*
import net.minecraft.realms.RealmsMth.clamp
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import java.lang.Exception
import kotlin.math.abs
import kotlin.math.floor

@ModuleInfo(
    name = "WtapBot", description = "The module emulate the legit player's combat action.",
    category = ModuleCategory.COMBAT
)
class WtapBot : Module() {

    private val maxCPSValue: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20) {

        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minCPSValue.get()
            if (minCPS > newValue)
                set(minCPS)
        }

    }

    private val minCPSValue: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20) {

        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = maxCPSValue.get()
            if (maxCPS < newValue)
                set(maxCPS)
        }

    }

    private val captureRange = FloatValue("CaptureRange", 15f, 10f, 30f)


    //is set manually in case u use more than normal reach
    private val reach = FloatValue("ClientReach", 3f, 3f, 6f)
    private val hurtTime = IntegerValue("HurtTime", 1, 0, 10)
    private val wTapTimeOut = IntegerValue("WTapTimeout", 120, 0, 500)

    private val doJumpTap = BoolValue("JumpTap", true)

    private val doSTap = BoolValue("STap", true)
    private val sTapTimeOut = IntegerValue("STapTimeOut", 500, 0, 1000)

    private val doADStrafe = BoolValue("ADStrafe", true)
    private val adStrafeInterval = IntegerValue("ADStrafeInterval", 200, 0, 1000)

    private val doBlock = BoolValue("Block", true)
    private val stopKey = TextValue("StopKey", "z")


    private val backTraces = mutableListOf<Vec3>()
    private val wTapTimer = MSTimer()
    private val sTapTimer = MSTimer()
    private val adStrafeTimer = MSTimer()

    private var target: Entity? = null
    private var lastTarget: Entity? = null
    private var counts = 5

    private var leftDelay = 0L
    private var leftLastSwing = 0L
    private var lastFrameLeftDown = false
    private var strafeLeft = false

    private var combo = 0

    override fun onDisable() {
        reset()
    }

    @EventTarget
    fun onRender3D(render3DEvent: Render3DEvent) {

        if (mc.gameSettings.keyBindAttack.pressed && !lastFrameLeftDown)
            onLeftClick()

        lastFrameLeftDown = mc.gameSettings.keyBindAttack.pressed


        if (target != null && !mc.thePlayer.isUsingItem && System.currentTimeMillis() - leftLastSwing >= leftDelay) {
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

            leftLastSwing = System.currentTimeMillis()
            leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }

        if (target != null && target !in mc.theWorld.loadedEntityList)
            reset()

        target ?: return

        if (mc.thePlayer.getDistanceToEntityBox(target!!) > captureRange.get())
            reset()

        target ?: return

        if (mc.thePlayer.getDistanceToEntityBox(target!!) > reach.get() + 3f)
            combo = 0

        aim()
        setKeyStates()

    }

    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) {

        target ?: return

        if (backTraces.size > 10)
            backTraces.removeAt(0)
        backTraces.add(target!!.positionVector)
    }

    @EventTarget
    fun onAttack(attackEvent: AttackEvent) {
        if (attackEvent.targetEntity is EntityLivingBase && attackEvent.targetEntity.hurtTime <= hurtTime.get())
            wTapTimer.reset()
    }

    @EventTarget
    fun onKey(keyEvent: KeyEvent) {
        if (keyEvent.key == Keyboard.getKeyIndex(stopKey.get().toUpperCase()))
            reset()
    }

    @EventTarget
    fun onPacket(packetEvent: PacketEvent) {
        mc.thePlayer ?: return
        if (packetEvent.packet is S12PacketEntityVelocity && packetEvent.packet.entityID == mc.thePlayer.entityId)
            combo = if (combo > 0) 0 else combo - 1

        target ?: return
        if (packetEvent.packet is S19PacketEntityStatus &&
            packetEvent.packet.getEntity(mc.theWorld) == target &&
            packetEvent.packet.opCode.toInt() == 2
        )
            combo++
    }

    private fun reset() {

        counts = 5
        val settings = mc.gameSettings

        settings.keyBindUseItem.pressed = GameSettings.isKeyDown(settings.keyBindUseItem)
        settings.keyBindSneak.pressed = GameSettings.isKeyDown(settings.keyBindSneak)
        settings.keyBindForward.pressed = GameSettings.isKeyDown(settings.keyBindForward)
        settings.keyBindBack.pressed = GameSettings.isKeyDown(settings.keyBindBack)
        settings.keyBindLeft.pressed = GameSettings.isKeyDown(settings.keyBindLeft)
        settings.keyBindRight.pressed = GameSettings.isKeyDown(settings.keyBindRight)
        settings.keyBindSprint.pressed = GameSettings.isKeyDown(settings.keyBindSprint)
        settings.keyBindJump.pressed = GameSettings.isKeyDown(settings.keyBindJump)

        target = null
        combo = 0
        backTraces.clear()
    }

    private fun setKeyStates() {

        val settings = mc.gameSettings

        if (mc.thePlayer.getDistanceSqToEntity(target!!) < reach.get() + 1.0f && combo <= 0 && abs(mc.thePlayer.rotationYaw - target!!.rotationYaw) < 45)
            if (sTapTimer.hasTimePassed(sTapTimeOut.get().toLong() * 3))
                triggerStap()

        if (mc.thePlayer.getDistanceSqToEntity(target!!) < reach.get() - 0.3f && combo > 0)
            triggerStap()

        //wtap
        if (wTapTimer.hasTimePassed(wTapTimeOut.get().toLong())) {
            settings.keyBindUseItem.pressed = false
            settings.keyBindSprint.pressed = true
            settings.keyBindForward.pressed = true
        } else {
            if (doBlock.get()) settings.keyBindUseItem.pressed = true
            settings.keyBindSprint.pressed = false
            settings.keyBindForward.pressed = false
        }

        //stap
        if (!sTapTimer.hasTimePassed(sTapTimeOut.get().toLong()) && doSTap.get()) {
            settings.keyBindBack.pressed = true
        } else settings.keyBindBack.pressed = Keyboard.isKeyDown(settings.keyBindBack.keyCode)

        //adtap
        if (combo > 0 && doADStrafe.get()) {
            if (strafeLeft) {
                settings.keyBindLeft.pressed = true
                settings.keyBindRight.pressed = false
            } else {
                settings.keyBindLeft.pressed = false
                settings.keyBindRight.pressed = true
            }
        }

        if (combo > 0 && adStrafeTimer.hasTimePassed(adStrafeInterval.get().toLong())) {
            adStrafeTimer.reset()
            strafeLeft = !strafeLeft
        }


        if (combo <= 0) {
            if (mc.thePlayer.getDistanceToEntityBox(target!!) < reach.get() + 1.5f) {
                if (strafeLeft) {
                    settings.keyBindLeft.pressed = true
                    settings.keyBindRight.pressed = false
                } else {
                    settings.keyBindLeft.pressed = false
                    settings.keyBindRight.pressed = true
                }
            } else {
                settings.keyBindLeft.pressed = false
                settings.keyBindRight.pressed = false
            }
        }

        //jumptap
        if (doJumpTap.get() && combo > 2)
            mc.gameSettings.keyBindJump.pressed = true
        else
            mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(settings.keyBindJump.keyCode)

        //manual ad override
        val leftDown = Keyboard.isKeyDown(settings.keyBindLeft.keyCode)
        val rightDown = Keyboard.isKeyDown(settings.keyBindRight.keyCode)

        if (rightDown) {
            mc.gameSettings.keyBindRight.pressed = true
            mc.gameSettings.keyBindLeft.pressed = false
        }

        if (leftDown) {
            mc.gameSettings.keyBindRight.pressed = false
            mc.gameSettings.keyBindLeft.pressed = true
        }

        if (rightDown && leftDown) {
            mc.gameSettings.keyBindLeft.pressed = true
            mc.gameSettings.keyBindRight.pressed = true
        }
    }

    private fun onLeftClick() {
        val thisTarget = mc.theWorld.loadedEntityList
            .filter { KillAura::isEnemy.invoke(LiquidBounce.moduleManager.getModule("KillAura") as KillAura, it) }
            .filter { mc.thePlayer.getDistanceToEntityBox(it) < captureRange.get() }
            .minBy { RotationUtils.getRotationDifference(it) }


        if (thisTarget == lastTarget && lastTarget != null) {
            if (counts > 0)
                counts--
        } else {
            lastTarget = thisTarget

            reset()
        }

        when (counts) {
            0 -> {
                target = thisTarget
                LiquidBounce.hud.addNotification(Notification("Target ${lastTarget!!.name} locked"))
            }

            2 -> {
                LiquidBounce.hud.addNotification(Notification("Click 2 more time to lock ${lastTarget!!.name}"))
            }

            else -> {
            }
        }
    }

    private fun aim() {
        if(backTraces.isEmpty())
            backTraces.add(target!!.positionVector)

        backTraces[backTraces.size - 1] = target!!.positionVector

        val index = floor(
            backTraces.size * (1 - clamp(
                mc.thePlayer.getDistanceToEntityBox(target!!) / reach.get() - 1,
                0.01,
                1.0
            ))
        ).toInt()
        val aimPos = backTraces[clamp(index, 0, backTraces.size - 1)]

        val xOffset = aimPos.xCoord - target!!.posX
        val yOffset = aimPos.yCoord - target!!.posY
        val zOffset = aimPos.zCoord - target!!.posZ

        RotationUtils.searchCenter(
            target!!.entityBoundingBox.offset(xOffset, yOffset, zOffset).expand(-0.05, -0.05, -0.05),
            false,
            false,
            false,
            true,
            300F
        )
            .rotation.toPlayer(mc.thePlayer)
    }

    private fun triggerStap() {
        if (sTapTimer.hasTimePassed(sTapTimeOut.get().toLong()))
            sTapTimer.reset()
    }

    override val tag: String?
        get() = if (target == null) "Idle" else target!!.name
}