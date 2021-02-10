/*
    Licensed under GPL-v3
* */
package net.ccbluex.liquidbounce.features.module.modules.annoyance

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue
import net.minecraft.network.play.client.C14PacketTabComplete
import kotlin.math.min

@ModuleInfo(name = "InsultAssistant", description = "Tab completion for the swearwords", category = ModuleCategory.ANNOYANCE)
class InsultAssistant : Module() {

    private val useTags = BoolValue("UseTags", true)
    private val maxTabs = IntegerValue("MaxTabs", 4, 0, 30)

    public var prefix = TextValue("Prefix", "#")

    public fun handleTabComplete(input: String) : Array<String> {
        val insults = LiquidBounce.fileManager.insultsConfig.insults

        val tagsMatch = ".*#(.*)".toRegex().find(input)
        val haveTags = useTags.get() && tagsMatch != null

        val tagsStr = if(haveTags) tagsMatch!!.destructured.toList()[0] else ""
        val intendedTags = tagsStr.split(',')

        insults.sortBy {
            var score = 0

            if (it.text.startsWith(input))
                score += 1000 * 1000 * 1000

            if (haveTags)
                for (intendedTag in intendedTags)
                    for (tag in it.tags)
                        if (tag.equals(intendedTag, true))
                            score += 1000

            score += it.used

            -score
        }

        val insultsText = insults.map { insult -> insult.text }.toTypedArray()

        return insultsText.sliceArray(0..min(insultsText.size - 1, maxTabs.get()))
    }

    public fun recordUse(string: String){
        for (insult in LiquidBounce.fileManager.insultsConfig.insults)
            if (string.contains(insult.text))
                insult.used++

        LiquidBounce.fileManager.insultsConfig.saveConfig();
    }
}