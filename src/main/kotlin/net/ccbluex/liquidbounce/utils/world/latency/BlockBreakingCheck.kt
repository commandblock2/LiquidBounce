package net.ccbluex.liquidbounce.utils.world.latency

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.world.LatencyStats.MAX_EXPECTED_LATENCY
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.util.math.BlockPos

object BlockBreakingCheck : LatencyCheck {
    override val name = "BlockBreakingCheck"

    private data class BlockBreakingEntry(val time: Long, val blockPos: BlockPos)

    private val latencyEntries = mutableListOf<LatencyEntry>()
    private val pendingBlockUpdates = mutableListOf<BlockBreakingEntry>()



    val packetHandler = handler<PacketEvent> { event ->
        when(val packet = event.packet) {
            is PlayerActionC2SPacket -> {
                if (packet.action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK)
                    pendingBlockUpdates.addLast(BlockBreakingEntry(System.currentTimeMillis(), packet.pos))
            }

            is BlockUpdateS2CPacket -> {

                pendingBlockUpdates.removeIf {
                    System.currentTimeMillis() - MAX_EXPECTED_LATENCY > it.time
                }

                pendingBlockUpdates.find {
                    it.blockPos == packet.pos
                }?.let {
                    val latency = System.currentTimeMillis() - it.time
                    pendingBlockUpdates.remove(it)
                    latencyEntries.addLast(LatencyEntry(latency, System.currentTimeMillis()))
                }
            }
        }
    }

    override fun reset() {
        latencyEntries.clear()
    }

    override fun getLatencyEntries(): List<LatencyEntry> {
        return latencyEntries
    }

}
