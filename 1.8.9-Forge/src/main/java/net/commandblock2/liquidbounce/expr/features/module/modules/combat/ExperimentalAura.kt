package net.commandblock2.liquidbounce.expr.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.commandblock2.liquidbounce.expr.utils.TargetingPolicy
import net.commandblock2.liquidbounce.expr.value.TargetValue
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "ExperimentalAura", description = "Experimental KillAura",
    category = ModuleCategory.EXPERIMENTAL)
class ExperimentalAura : Module() {
    private val targetPolicy = TargetValue("TargetPolicy", TargetingPolicy())
}