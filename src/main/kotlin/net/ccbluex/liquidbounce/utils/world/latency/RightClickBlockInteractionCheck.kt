package net.ccbluex.liquidbounce.utils.world.latency

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.world.LatencyStats.MAX_EXPECTED_LATENCY
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

object RightClickBlockInteractionCheck : LatencyCheck {

    private data class BlockInteraction(
        val pos: BlockPos,
        val adjacentPos: BlockPos,
        val startTime: Long,
        var posClicked: Boolean = false,
        var adjacentClicked: Boolean = false
    )


    private val pendingInteractions = ArrayDeque<BlockInteraction>()


    private val latencyEntries = mutableListOf<LatencyEntry>()

    val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlayerInteractBlockC2SPacket -> {
                if (packet.hand != Hand.MAIN_HAND)
                    return@handler
                val pos = packet.blockHitResult.blockPos
                val adjacentPos = pos.offset(packet.blockHitResult.side)

                if (pendingInteractions.find {
                        pos == it.pos && adjacentPos == it.adjacentPos
                    } != null)
                    return@handler

                pendingInteractions.addLast(BlockInteraction(pos, adjacentPos, System.currentTimeMillis()))
            }

            is BlockUpdateS2CPacket -> {
                val updatePos = packet.pos

                pendingInteractions
                    .removeIf {
                        System.currentTimeMillis() - MAX_EXPECTED_LATENCY > it.startTime
                    }

                pendingInteractions.find {
                    it.pos == updatePos || it.adjacentPos == updatePos
                }?.let {
                    when {
                        !it.posClicked -> it.posClicked = true
                        !it.adjacentClicked -> {
                            it.adjacentClicked = true
                            val latency = System.currentTimeMillis() - it.startTime
                            latencyEntries.addLast(LatencyEntry(latency, System.currentTimeMillis()))
                            pendingInteractions.remove(it)
                        }

                        else -> {}
                    }

                }

            }
        }
    }

    override val name = "BlockInteractionCheck"

    override fun reset() {
        pendingInteractions.clear()
        latencyEntries.clear()
    }

    override fun getLatencyEntries(): List<LatencyEntry> {
        return latencyEntries
    }
}
