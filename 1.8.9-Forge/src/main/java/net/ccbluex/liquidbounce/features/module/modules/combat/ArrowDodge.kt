package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.command.commands.HClipCommand
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityEgg
import net.minecraft.entity.projectile.EntitySnowball
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color
import java.util.*
import kotlin.math.atan2

@ModuleInfo(
    name = "ArrowDodge",
    description = "Automatically dodges the coming arrow",
    category = ModuleCategory.COMBAT
)
class ArrowDodge : Module() {

    private val dodgingMode = ListValue(
        "DodgingMode", arrayOf(
            "HorizontalWalk", "HorizontalSpeed", "HorizontalTp",
            "Block", "VerticalTp"
        ), "HorizontalWalk"
    )
    private val autoSword = BoolValue("AutoSword", true)

    private val predictTicks = IntegerValue("TicksToDodge", 10, 0, 20)
    private val minimalTicks = IntegerValue("MinimalDodgeTime", 5, 5, 10)

    private val render = BoolValue("Render", true)
    private val releaseBow = BoolValue("ReleaseBowOnDodge", true)

    private val entityType2Gravity = mapOf(
        EntityArrow::class.java to 0.05,
        EntitySnowball::class.java to 0.03,
        EntityEgg::class.java to 0.03
    )

    private var entity2History = mutableMapOf<Entity, MutableList<Vec3>>()
    private var entity2Prediction = mutableMapOf<Entity, MutableList<Vec3>>()
    //Isn't Int and Entity are all pointers, so that shouldn't matter if i
    //use Entity over Int as the key of a Map right?

    //I somehow feel that it can be better with only Map not MutableMap

    var dodgingObject: Entity? = null
    var oYaw = -1f
    var oIndex = -1
    var oSpeedState = false
    var oBlockState = false
    var oForwardState = false
    var onGoingTimer: Timer? = null

    override fun onEnable() {
        onGoingTimer = null
        dodgingObject = null
    }

    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) {
        val entities = mc.theWorld.loadedEntityList.filter {
            entityType2Gravity.containsKey(it.javaClass)
        }

        entities.forEach {
            if (it is EntityArrow && it.prevPosX == it.posX && it.prevPosY == it.posY && it.prevPosZ == it.posZ) {
                if (entity2History.containsKey(it)) {
                    entity2History.remove(it)
                    entity2Prediction.remove(it)
                    return
                }
            } else {

                if (!entity2History.containsKey(it))
                    entity2History[it] = mutableListOf()

                entity2History[it]!!.add(it.positionVector)
                entity2Prediction[it] = predict(it)
            }
        }


        entity2History = entity2History.filterKeys { entity -> entities.contains(entity) }.toMutableMap()
        entity2Prediction = entity2Prediction.filterKeys { entity -> entities.contains(entity) }.toMutableMap()

        doDodge()
    }

    private fun doDodge() {

        //later this will be refactored to use targetRotation
        if (dodgingObject == null) {
            oYaw = mc.thePlayer.rotationYaw
            oIndex = mc.thePlayer.inventory.currentItem
            oSpeedState = (LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed).state
            oBlockState = mc.gameSettings.keyBindUseItem.pressed
            oForwardState = mc.gameSettings.keyBindForward.pressed
            return
        }

        if (onGoingTimer != null)
            return

        if (mc.thePlayer.isUsingItem && mc.thePlayer.heldItem.item is ItemBow) {
            /*val target = mc.theWorld.loadedEntityList.filter {
                it is EntityLivingBase && EntityUtils.isSelected(it, true) && mc.thePlayer.canEntityBeSeen(it)
            }.minBy { RotationUtils.getRotationDifference(it) }
            if (target != null)
                RotationUtils.faceBow(target, false, true, mc.thePlayer.getDistanceToEntity(target) / 2)*/
                    //currently the bowaimbot is bullshit, will make it better later
            Timer("setTimeout", true).schedule(object : TimerTask(){
                override fun run() {
                    mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                }
            }, 10)
            mc.gameSettings.keyBindUseItem.pressed = false
            return
        }

        onGoingTimer = Timer("setTimeout", true)

        if (dodgingMode.get().toLowerCase().contains("horizontal")) {

            val incYaw = atan2(dodgingObject!!.motionZ, dodgingObject!!.motionX) * 180 / Math.PI + 90

            val right = Rotation((incYaw - 90F).toFloat(), mc.thePlayer.rotationPitch)
            val left = Rotation((incYaw + 90F).toFloat(), mc.thePlayer.rotationPitch)

            val rightDis = mc.thePlayer.positionVector.add(RotationUtils.getVectorForRotation(right)).subtract(
                dodgingObject!!.positionVector
            ).lengthVector()
            val leftDis = mc.thePlayer.positionVector.add(RotationUtils.getVectorForRotation(left))
                .subtract(dodgingObject!!.positionVector).lengthVector()

            val rot = if (rightDis > leftDis) right else left
            rot.toPlayer(mc.thePlayer)



            //This is fucking spaghetti
            when (dodgingMode.get()) {

                "HorizontalWalk" -> {
                    mc.gameSettings.keyBindForward.pressed = true

                    onGoingTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            restore()
                        }
                    }, minimalTicks.get() * 50L)
                }

                "HorizontalSpeed" -> {
                    mc.gameSettings.keyBindForward.pressed = true
                    (LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed).state = true

                    onGoingTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            restore()
                        }
                    }, minimalTicks.get() * 50L)
                }

                "HorizontalTp" -> {
                    HClipCommand().execute(arrayOf(".hclip", "0.6"))
                    restore()
                }

                else -> {
                }
            }

        }

        when (dodgingMode.get()) {
            "Block" -> {
                mc.gameSettings.keyBindUseItem.pressed = true

                if (autoSword.get()) {
                    repeat(9) {
                        val stack = mc.thePlayer.inventory.getStackInSlot(it)
                        if (stack != null && stack.item is ItemSword)
                            mc.thePlayer.inventory.currentItem = it
                    }
                }

                onGoingTimer!!.schedule(object : TimerTask() {
                    override fun run() {
                        restore()
                    }
                }, minimalTicks.get() * 50L)
            }

            "VerticalTp" -> {
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
                restore()
            }

            else -> {
            }
        }
    }

    private fun restore() {
        if (dodgingMode.get().contains("Horizontal"))
            mc.thePlayer.rotationYaw = oYaw

        if (dodgingMode.get().equals("HorizontalWalk"))
            mc.gameSettings.keyBindForward.pressed = oForwardState

        when (dodgingMode.get()) {

            "Block" -> {
                mc.gameSettings.keyBindUseItem.pressed = oBlockState
                if (autoSword.get())
                    mc.thePlayer.inventory.currentItem = oIndex
            }

            "HorizontalSpeed" -> {
                (LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed).state = oSpeedState
                mc.gameSettings.keyBindForward.pressed = oForwardState
            }

        }

        dodgingObject = null
        onGoingTimer = null
    }

    @EventTarget
    fun onRender3d(render3DEvent: Render3DEvent) {
        if (!render.get()) return

        entity2History.forEach { (_, mutableList) -> RenderUtils.drawPoses(Color(255, 255, 0), mutableList) }
        entity2Prediction.forEach { (_, mutableList) -> RenderUtils.drawPoses(Color(86, 156, 214), mutableList) }
    }

    private fun predict(entity: Entity): MutableList<Vec3> {
        var motionX = entity.motionX
        var motionY = entity.motionY
        var motionZ = entity.motionZ

        val poses = mutableListOf<Vec3>()
        poses.add(entity.positionVector)

        var x = entity.posX
        var y = entity.posY
        var z = entity.posZ

        while (y > 0) {
            val posBefore = Vec3(x, y, z)
            val posAfter = Vec3(x + motionX, y + motionY, z + motionZ)

            poses.add(posAfter)

            val landingPos = mc.theWorld.rayTraceBlocks(posBefore, posAfter, false, true, false)

            if (landingPos != null) {
                poses.add(landingPos.hitVec)
                break
            }

            val playerBBox = mc.thePlayer.entityBoundingBox.expand(0.3, 0.4, 0.3)
            val prediction = playerPrediction(predictTicks.get())

            if ((playerBBox.calculateIntercept(posBefore, posAfter) != null ||
                        playerBBox.offset(-mc.thePlayer.posX, -mc.thePlayer.posY, -mc.thePlayer.posZ)
                            .offset(prediction.xCoord, prediction.yCoord, prediction.zCoord)
                            .calculateIntercept(posBefore, posAfter) != null)
                && poses.size <= predictTicks.get()
            ) {
                dodgingObject = entity
            }

            x += motionX
            y += motionY
            z += motionZ

            if (mc.theWorld.getBlockState(BlockPos(x, y, z)).block.material == Material.water) {
                motionX *= 0.6
                motionY *= 0.6
                motionZ *= 0.6
            } else {
                motionX *= 0.99
                motionY *= 0.99
                motionZ *= 0.99
            }

            motionY -= entityType2Gravity[entity.javaClass]!!
        }


        return poses
    }

    private fun playerPrediction(ticks: Int): Vec3 {
        var x = mc.thePlayer.posX
        var y = mc.thePlayer.posY
        var z = mc.thePlayer.posZ

        x += mc.thePlayer.motionX * ticks
        y += mc.thePlayer.motionY * ticks
        z += mc.thePlayer.motionZ * ticks

        return Vec3(x, y, z)
    }

    override val tag: String?
        get() = if (dodgingObject != null) "Dodging " + dodgingObject!!.name else "Idle"
}