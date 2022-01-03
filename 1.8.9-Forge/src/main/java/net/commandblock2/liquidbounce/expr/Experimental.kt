package net.commandblock2.liquidbounce.expr

import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.commandblock2.liquidbounce.expr.utils.TargetingPolicy


class Experimental {
    val globalPolicy = TargetingPolicy()

    val categoryPolicies = mapOf(
        ModuleCategory.RENDER to
                TargetingPolicy.PolicyBuilder()
                    .targetPlayer()
                    .targetMobs()
                    .targetInvisible()
                    .targetDead()
                    .targetAnimals()
                    .targetingPolicy,

        ModuleCategory.COMBAT to
                TargetingPolicy.PolicyBuilder()
                    .targetPlayer()
                    .targetInvisible()
                    .targetMobs()
    )
}