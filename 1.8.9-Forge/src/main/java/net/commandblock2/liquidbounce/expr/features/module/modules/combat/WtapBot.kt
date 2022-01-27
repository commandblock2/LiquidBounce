/*
 * Licensed under GPL-v3
 */
package net.commandblock2.liquidbounce.expr.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.Rotation
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
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.*
import net.minecraft.realms.RealmsMth.clamp
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import java.lang.Exception
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.min

@ModuleInfo(
    name = "WtapBot",
    description = "The module emulate the legit player's combat action.",
    category = ModuleCategory.COMBAT
)
class WtapBot : Module() {

    private val maxCPSValue: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20) {

        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minCPSValue.get()
            if (minCPS > newValue) set(minCPS)
        }

    }

    private val minCPSValue: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20) {

        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = maxCPSValue.get()
            if (maxCPS < newValue) set(maxCPS)
        }

    }

    private val captureRange = FloatValue("CaptureRange", 15f, 10f, 30f)


    //is set manually in case u use more than normal reach
    private val reach = FloatValue("ClientReach", 3f, 3f, 6f)
    private val comboExtendedReach = FloatValue("ComboExtendedReach", 1f, 0f, 3f)
    private val hurtTime = IntegerValue("HurtTime", 1, 0, 10)
    private val wTapTimeOut = IntegerValue("WTapTimeout", 120, 0, 500)

    private val doJumpTap = BoolValue("JumpTap", true)

    private val doSTap = BoolValue("STap", true)
    private val sTapTimeOut = IntegerValue("STapTimeOut", 500, 0, 1000)

    private val doADStrafe = BoolValue("ADStrafe", true)
    private val adStrafeInterval = IntegerValue("ADStrafeInterval", 200, 0, 1000)

    private val doBlock = BoolValue("Block", true)
    private val doInteractBlock = BoolValue("InteractBlock", false)
    private val blockIdleTimeout = IntegerValue("BlockIdleTimeout", 100, 0, 200)
    private val stopKey = TextValue("StopKey", "z")

    private val doEscape = BoolValue("EscapeWhen", true)
    private val health = FloatValue("HealthIsLowerThan", 6f, 0f, 20f)
    private val comboed = IntegerValue("BeingComboedMoreThan", 2, 2, 5)

    private val auraMode = BoolValue("AuraMode", false)

    private val backTraces = mutableListOf<Vec3>()
    private val wTapTimer = MSTimer()
    private val sTapTimer = MSTimer()
    private val adStrafeTimer = MSTimer()
    private val blockIdleTimer = MSTimer()

    private var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    private var counts = 3

    private var leftDelay = 0L
    private var leftLastSwing = 0L
    private var lastFrameLeftDown = false
    private var strafeLeft = false

    public var blocking = false

    private var combo = 0

    private lateinit var rotation: Rotation


    override fun onDisable() {
        reset(false)
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {

        val (yaw) = RotationUtils.targetRotation ?: return
        var strafe = event.strafe
        var forward = event.forward
        val friction = event.friction

        var f = strafe * strafe + forward * forward

        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f)

            if (f < 1.0F) f = 1.0F

            f = friction / f
            strafe *= f
            forward *= f

            val yawSin = MathHelper.sin((yaw * Math.PI / 180F).toFloat())
            val yawCos = MathHelper.cos((yaw * Math.PI / 180F).toFloat())

            mc.thePlayer.motionX += strafe * yawCos - forward * yawSin
            mc.thePlayer.motionZ += forward * yawCos + strafe * yawSin
        }

        event.cancelEvent()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {

        if (mc.gameSettings.keyBindAttack.pressed && !lastFrameLeftDown) onLeftClick()

        lastFrameLeftDown = mc.gameSettings.keyBindAttack.pressed



        if (target != null && target !in mc.theWorld.loadedEntityList) reset()

        target ?: return

        if (mc.thePlayer.getDistanceToEntityBox(target!!) > captureRange.get()) reset()

        target ?: return

        if (mc.thePlayer.getDistanceToEntityBox(target!!) > calcReach() + 1f)
            combo = 0

    }

    @EventTarget
    private fun attack(event: UpdateEvent) {

        target ?: return

        if (backTraces.size > 10) backTraces.removeAt(0)
        backTraces.add(target!!.positionVector)

        setKeyStates()

        aim()

        if (mc.thePlayer != null && target != null && !blocking && System.currentTimeMillis() - leftLastSwing >= leftDelay && blockIdleTimer.hasTimePassed(
                blockIdleTimeout.get().toLong()
            )
        ) {
            // KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

            mc.thePlayer.swingItem()
            if (auraMode.get()) onLeftClick()

            val entt = RaycastUtils.raycastEntity(reach.get().toDouble(), rotation.yaw,rotation.pitch) {
                (it is EntityLivingBase && it !is EntityArmorStand)
            }
            entt?.also {

                if (mc.thePlayer.getDistanceToEntityBox(it) <= calcReach()) {

                    LiquidBounce.eventManager.callEvent(AttackEvent(it))
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(it, C02PacketUseEntity.Action.ATTACK))
                    mc.thePlayer.attackTargetEntityWithCurrentItem(it)
                }
            }
            if (entt == null && mc.thePlayer.getDistanceToEntityBox(target!!) < reach.get())
                ClientUtils.displayChatMessage(mc.thePlayer.getDistanceToEntityBox(target!!).toString())

            leftLastSwing = System.currentTimeMillis()
            leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }
    }


    @EventTarget
    fun onAttack(attackEvent: AttackEvent) {
        if (attackEvent.targetEntity is EntityLivingBase && attackEvent.targetEntity.hurtTime <= hurtTime.get())
            wTapTimer.reset()
    }

    @EventTarget
    fun onKey(keyEvent: KeyEvent) {
        if (keyEvent.key == Keyboard.getKeyIndex(stopKey.get().toUpperCase())) reset(false)
    }

    @EventTarget
    fun onPacket(packetEvent: PacketEvent) {
        mc.thePlayer ?: return
        if (packetEvent.packet is S12PacketEntityVelocity && packetEvent.packet.entityID == mc.thePlayer.entityId) combo =
            if (combo > 0) 0 else combo - 1


        target ?: return
        if (packetEvent.packet is S19PacketEntityStatus && packetEvent.packet.getEntity(mc.theWorld) == target && packetEvent.packet.opCode.toInt() == 2) combo =
            if (combo < 0) 0 else combo + 1

        if (packetEvent.packet is S12PacketEntityVelocity && packetEvent.packet.entityID == mc.thePlayer.entityId && doEscape.get()) {

            if (mc.thePlayer.health < health.get()) {
                reset()
                mc.thePlayer.rotationYaw = -(atan2(
                    packetEvent.packet.motionX.toDouble(), packetEvent.packet.motionZ.toDouble()
                ) * 180 / Math.PI).toFloat()
                mc.gameSettings.keyBindForward.pressed = true
            }

            if (combo <= -comboed.get()) {
                reset()
                mc.thePlayer.rotationYaw = -(atan2(
                    packetEvent.packet.motionX.toDouble(), packetEvent.packet.motionZ.toDouble()
                ) * 180 / Math.PI).toFloat()
                mc.gameSettings.keyBindForward.pressed = true
            }
        }
    }

    private fun reset(bool: Boolean = true) {

        counts = 3
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
        tryUnblock()


        backTraces.clear()

        if (auraMode.get() && bool) onLeftClick()
    }

    private fun setKeyStates() {

        val settings = mc.gameSettings

        if (mc.thePlayer.getDistanceSqToEntity(target!!) < calcReach() + 1.0f && combo <= 0 && abs(
                RotationUtils.getAngleDifference(
                    RotationUtils.serverRotation.yaw,
                    target!!.rotationYaw
                )
            ) < 45
        ) if (sTapTimer.hasTimePassed(sTapTimeOut.get().toLong() * 3)) triggerStap()

        if (mc.thePlayer.getDistanceSqToEntity(target!!) < calcReach() - 0.3f && combo > 0) triggerStap()

        //wtap
        if (wTapTimer.hasTimePassed(wTapTimeOut.get().toLong())) {
            tryUnblock()
            settings.keyBindSprint.pressed = true
            settings.keyBindForward.pressed = true
        } else {
            if (doBlock.get()) {
                tryBlock()
            }
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
            if (mc.thePlayer.getDistanceToEntityBox(target!!) < calcReach() + 1.5f) {
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
        if (doJumpTap.get() && combo > 2) mc.gameSettings.keyBindJump.pressed = true
        else mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(settings.keyBindJump.keyCode)

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

    private fun tryBlock() {

        mc.thePlayer.heldItem ?: return

        if (mc.thePlayer.heldItem.item !is ItemSword) return

        if (blocking) return

        if (!blockIdleTimer.hasTimePassed(blockIdleTimeout.get().toLong())) return

        val interact = mc.thePlayer.getDistanceToEntityBox(target!!) < calcReach() && doInteractBlock.get()

        if (interact) {
            val positionEye = mc.renderViewEntity.getPositionEyes(1F)

            val expandSize = target!!.collisionBorderSize.toDouble()
            val aABB = target!!.entityBoundingBox.expand(expandSize, expandSize, expandSize)

            val (yaw, pitch) = RotationUtils.serverRotation
            val yawCos = MathHelper.cos(-yaw * 0.017453292f - Math.PI.toFloat())
            val yawSin = MathHelper.sin(-yaw * 0.017453292f - Math.PI.toFloat())
            val pitchCos = -MathHelper.cos(-pitch * 0.017453292f)
            val pitchSin = MathHelper.sin(-pitch * 0.017453292f)
            val range = min(calcReach().toDouble(), mc.thePlayer.getDistanceToEntityBox(target!!)) + 1
            val lookAt = positionEye.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

            val movingObject = aABB.calculateIntercept(positionEye, lookAt)
            movingObject ?: return
            val hitVec = movingObject.hitVec

            mc.netHandler.addToSendQueue(
                C02PacketUseEntity(
                    target!!, Vec3(
                        hitVec.xCoord - target!!.posX, hitVec.yCoord - target!!.posY, hitVec.zCoord - target!!.posZ
                    )
                )
            )

            mc.netHandler.addToSendQueue(C02PacketUseEntity(target!!, C02PacketUseEntity.Action.INTERACT))
        }

        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))

        blocking = true
    }

    private fun tryUnblock() {
        if (blocking) {
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN
                )
            )

            blocking = false
            blockIdleTimer.reset()

            leftLastSwing = System.currentTimeMillis()
            leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }
    }

    private fun onLeftClick() {
        val thisTarget = mc.theWorld.loadedEntityList.filter {
            KillAura::isEnemy.invoke(
                LiquidBounce.moduleManager.getModule("KillAura") as KillAura,
                it
            )
        }.filter {
            mc.thePlayer.getDistanceToEntityBox(it) < captureRange.get()
        }.filterIsInstance<EntityLivingBase>()
            .minWith(
                if (!auraMode.get())
                    compareBy {
                        RotationUtils.getRotationDifference(
                            RotationUtils.toRotation(
                                RotationUtils.getCenter(it.entityBoundingBox),
                                false
                            ), RotationUtils.serverRotation
                        )
                    }
                else
                    compareBy<EntityLivingBase> {
                        mc.thePlayer.getDistanceToEntityBox(it) > calcReach() + 3
                    }
                        .thenBy {
                            RotationUtils.getRotationDifference(
                                RotationUtils.toRotation(
                                    RotationUtils.getCenter(it.entityBoundingBox),
                                    false
                                ), RotationUtils.serverRotation
                            ) > 15
                        }
                        .thenBy { mc.thePlayer.getDistanceToEntityBox(it) > calcReach() }
                        .thenBy { it.hurtResistantTime }
            )


        if (thisTarget == lastTarget && lastTarget != null) {

            if (counts > 0) counts--
        } else {
            lastTarget = thisTarget

            if (!auraMode.get()) reset(false)
        }

        if (auraMode.get() && combo < 3) target = thisTarget

        when (counts) {
            0 -> {
                target = thisTarget
                // LiquidBounce.hud.addNotification(Notification("Target ${lastTarget!!.name} locked"))
                if (!auraMode.get())
                    combo = 0
            }
//
//            2 -> {
//                LiquidBounce.hud.addNotification(Notification("Click 2 more time to lock ${lastTarget!!.name}"))
//            }

            else -> {
            }
        }
    }

    private fun aim() {
        target ?: return

        if (backTraces.isEmpty()) backTraces.add(target!!.positionVector)

        backTraces[backTraces.size - 1] = target!!.positionVector

        val index = floor(
            backTraces.size * (1 - clamp(
                mc.thePlayer.getDistanceToEntityBox(target!!) / calcReach() - 1, 0.01, 1.0
            ))
        ).toInt()
        val aimPos = backTraces[clamp(index, 0, backTraces.size)]

        val xOffset = aimPos.xCoord - target!!.posX
        val yOffset = aimPos.yCoord - target!!.posY
        val zOffset = aimPos.zCoord - target!!.posZ

        val vecRot = RotationUtils.searchCenter(
            target!!.entityBoundingBox.offset(xOffset, yOffset, zOffset).expand(-0.05, -0.05, -0.05),
            false,
            false,
            false,
            false,
            300F
        )


        val rotation = vecRot?.rotation

        RotationUtils.setTargetRotation(rotation ?: RotationUtils.serverRotation)
        this.rotation = RotationUtils.targetRotation
    }

    private fun triggerStap() {
        if (sTapTimer.hasTimePassed(sTapTimeOut.get().toLong())) sTapTimer.reset()
    }

    private fun calcReach(): Float {
        return if (combo > 0 && mc.thePlayer.isSprinting) reach.get() + comboExtendedReach.get() else reach.get()
    }

    override val tag: String?
        get() = if (target == null) "Idle" else target!!.name + " " + combo
}