package net.commandblock2.liquidbounce.expr.utils

import net.ccbluex.liquidbounce.utils.EntityUtils.isAnimal
import net.ccbluex.liquidbounce.utils.EntityUtils.isMob
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.RotationUtils.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer

class TargetingPolicy : MinecraftInstance() {

    public var targetInvisible = false
    public var targetPlayer = true
    public var targetMobs = true
    public var targetAnimals = false
    public var targetDead = false

    public var customSelector: CustomTargetSelector? = null

    public var customPriority: PriorityFunc? = null
    public var priority: Priority? = null


    enum class Priority(val displayName: String, val rule: PriorityFunc?) {
        HEALTH("Health", { lhs, rhs -> lhs.health < rhs.health }),
        DISTANCE(
            "DistanceToPlayer",
            { lhs, rhs -> mc.thePlayer.getDistanceToEntityBox(lhs) < mc.thePlayer.getDistanceToEntityBox(rhs) }),
        DIRECTION(
            "Direction",
            { lhs, rhs ->
                getRotationDifference(lhs) < getRotationDifference(rhs)
            }),
        LIVINGTIME(
            "LivingTime",
            { lhs, rhs -> lhs.ticksExisted > rhs.ticksExisted }
        ),
        HURTTIME(
            "HurtTime",
            { lhs, rhs -> lhs.hurtResistantTime < rhs.hurtResistantTime }
        )
    }

    public fun getTargets(): List<EntityLivingBase> {
        return getLoadedEntityLivingBase()
            .asSequence()
            .filter { targetInvisible || !it.isInvisible }
            .filter { targetPlayer || it !is EntityPlayer }
            .filter { targetMobs || !isMob(it) }
            .filter { targetAnimals || !isAnimal(it) }
            .filter { targetDead || it.isEntityAlive }
            .toList()
    }

    fun getLoadedEntityLivingBase(): List<EntityLivingBase> {
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()
    }


    class PolicyBuilder {
        val targetingPolicy = TargetingPolicy()

        fun targetInvisible(option: Boolean = true): PolicyBuilder {
            targetingPolicy.targetInvisible = option
            return this
        }

        fun targetPlayer(option: Boolean = true): PolicyBuilder {
            targetingPolicy.targetPlayer = option
            return this
        }

        fun targetMobs(option: Boolean = true): PolicyBuilder {
            targetingPolicy.targetMobs = option
            return this
        }

        fun targetDead(option: Boolean = true): PolicyBuilder {
            targetingPolicy.targetDead = option
            return this
        }

        fun targetAnimals(option: Boolean = true): PolicyBuilder {
            targetingPolicy.targetAnimals = option
            return this
        }

        fun priority(priority: Priority): PolicyBuilder {
            targetingPolicy.priority = priority;
            return this
        }

        fun customPriority(customPriority: PriorityFunc?): PolicyBuilder {
            targetingPolicy.customPriority = customPriority
            return this
        }

        fun customTargetSelector(customTargetSelector: CustomTargetSelector?): PolicyBuilder {
            targetingPolicy.customSelector = customTargetSelector
            return this
        }

    }

}

typealias PriorityFunc = (EntityLivingBase, EntityLivingBase) -> Boolean
typealias CustomTargetSelector = ((entity: Entity) -> Boolean)