package net.ccbluex.liquidbounce.utils.world

import net.ccbluex.liquidbounce.utils.world.latency.BlockBreakingCheck
import net.ccbluex.liquidbounce.utils.world.latency.RightClickBlockInteractionCheck
import net.ccbluex.liquidbounce.utils.world.latency.LatencyCheck


object LatencyStats {

    // - Send chat message, expect 1 packet from server for updating the chat
    // - Attack on a entity with 0 hurttime while it is not running away from u and hurt time packet
    // - Fall damage on client side then corresponding damage packet

    const val MAX_EXPECTED_LATENCY = 5 * 1000L

    private val latencyChecks = mutableListOf<LatencyCheck>(
        RightClickBlockInteractionCheck,
        BlockBreakingCheck
    )

    init {
        latencyChecks
    }


}



