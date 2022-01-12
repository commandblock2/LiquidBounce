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

    public var customPriority: Comparator<EntityLivingBase>? = null
    public var priority: Priority? = null


    private var listOfTargets: List<EntityLivingBase>? = null
        get() = field ?: updateTargets()

    private var visitedTargets = emptyList<EntityLivingBase>()

    public fun nextTarget(): EntityLivingBase? {
        updateTargets()

        val filtered = listOfTargets!!.filter { it !in visitedTargets }
        val target = if (filtered.isEmpty()) {
            visitedTargets = emptyList()

            if (listOfTargets!!.isEmpty())
                null
            else
                listOfTargets!!.first()

        } else
            filtered.first()

        return target
    }

    private fun updateTargets(): List<EntityLivingBase>? {
        listOfTargets = getLoadedEntityLivingBase()
            .asSequence()
            .filter(customSelector ?: {

                (targetInvisible || !it.isInvisible)
                        && (targetPlayer || it !is EntityPlayer)
                        && (targetMobs || !isMob(it))
                        && (targetAnimals || !isAnimal(it))
                        && (targetDead || it.isEntityAlive)

            })
            .sortedWith(customPriority ?: (priority?: Priority.HURTTIME).rule)
            .toList()

        return listOfTargets
    }


    enum class Priority(val displayName: String, val rule: Comparator<EntityLivingBase>) {
        HEALTH("Health", compareBy { it.health }),
        DISTANCE(
            "DistanceToPlayer",
            compareBy { mc.thePlayer.getDistanceToEntityBox(it) }),
        DIRECTION(
            "Direction",
            compareBy { getRotationDifference(it) }),
        LIVINGTIME(
            "LivingTime",
            compareByDescending { it.ticksExisted }
        ),
        HURTTIME(
            "HurtTime",
            compareBy { it.hurtResistantTime }
        )
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

        fun customPriority(customPriority: Comparator<EntityLivingBase>?): PolicyBuilder {
            targetingPolicy.customPriority = customPriority
            return this
        }

        fun customTargetSelector(customTargetSelector: CustomTargetSelector?): PolicyBuilder {
            targetingPolicy.customSelector = customTargetSelector
            return this
        }

    }

}

typealias CustomTargetSelector = ((entity: Entity) -> Boolean)