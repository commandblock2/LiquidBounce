package net.ccbluex.liquidbounce.utils.world.latency

import net.ccbluex.liquidbounce.event.Listenable

interface LatencyCheck : Listenable {
    val name: String
    fun reset()
    fun getLatencyEntries(): List<LatencyEntry>
}

data class LatencyEntry(
    val latency: Long,
    val worldTime: Long,
)
