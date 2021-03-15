package net.ccbluex.liquidbounce.features.special

import com.replaymod.replaystudio.replay.ReplayFile
import com.replaymod.replaystudio.replay.ReplayMetaData
import com.replaymod.replaystudio.replay.ZipReplayFile
import com.replaymod.replaystudio.studio.ReplayStudio
import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.entity.DataWatcher
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.EnumConnectionState
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.*
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.roundToInt

object ReplayRecording : Listenable, MinecraftInstance() {
    override fun handleEvents() = enabled

    var enabled = false


    private var replayFile: ReplayFile? = null
    private var replayMetaData = ReplayMetaData()
    private var packetOutputStream: DataOutputStream? = null

    private var saveService: ExecutorService? = null
    private var startTime = 0L

    var lastRiding: Entity? = null
    var wasSleeping = false
    var wasEating = false
    var heldItem: ItemStack? = null
    private val armors: Array<ItemStack?> = arrayOfNulls(4)

    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) {
        // To manually record the player's location
        // nvm I think converting C packet to S packet should be easier
        // still need some here

        val dx = mc.thePlayer.posX - mc.thePlayer.prevPosZ
        val dy = mc.thePlayer.posY - mc.thePlayer.prevPosY
        val dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ
        val packet = if (abs(dx) > 4.0 || abs(dy) > 4.0 || abs(dz) > 4.0) {
            val x = MathHelper.floor_double(mc.thePlayer.posX * 32.0)
            val y = MathHelper.floor_double(mc.thePlayer.posY * 32.0)
            val z = MathHelper.floor_double(mc.thePlayer.posZ * 32.0)
            val yaw = ((mc.thePlayer.rotationYaw * 256.0f / 360.0f).toInt()).toByte()
            val pitch = ((mc.thePlayer.rotationPitch * 256.0f / 360.0f).toInt()).toByte()
            S18PacketEntityTeleport(mc.thePlayer.entityId, x, y, z, yaw, pitch, mc.thePlayer.onGround)
        } else {
            val newYaw = ((mc.thePlayer.rotationYaw * 256.0f / 360.0f).toInt()).toByte()
            val newPitch = ((mc.thePlayer.rotationPitch * 256.0f / 360.0f).toInt()).toByte()
            S14PacketEntity.S17PacketEntityLookMove(
                mc.thePlayer.entityId,
                (dx * 32).roundToInt().toByte(), (dy * 32).roundToInt().toByte(),
                (dz * 32).roundToInt().toByte(),
                newYaw, newPitch, mc.thePlayer.onGround
            )
        }

        save(packet)

        if (mc.thePlayer.ridingEntity != lastRiding) {
            if (lastRiding != null)
                processServerPacket(S1BPacketEntityAttach(0, mc.thePlayer, null))
            else
                processServerPacket(S1BPacketEntityAttach(1, mc.thePlayer, mc.thePlayer.ridingEntity))
        }


        if (mc.thePlayer.isPlayerSleeping != wasSleeping) {
            if (mc.thePlayer.isPlayerSleeping)
                processServerPacket(S0APacketUseBed(mc.thePlayer, BlockPos(mc.thePlayer)))
            else
                processServerPacket(S0BPacketAnimation(mc.thePlayer, 2))
        }

        if (mc.thePlayer.isEating != wasEating) {
            if (mc.thePlayer.isEating)
                processServerPacket(S0BPacketAnimation(mc.thePlayer, 3))
            else
                processServerPacket(S19PacketEntityStatus(mc.thePlayer, 9.toByte()))
        }

        if (mc.playerController.curBlockDamageMP != 0F)
            processServerPacket(
                S25PacketBlockBreakAnim(mc.thePlayer.entityId, mc.playerController.currentBlock,
                (mc.playerController.curBlockDamageMP * 10 - 1).toInt()
            )
            )

        if (heldItem != mc.thePlayer.heldItem)
            processServerPacket(S04PacketEntityEquipment(mc.thePlayer.entityId, 0, mc.thePlayer.heldItem))

        repeat(4) {
            if (armors[it] != mc.thePlayer.inventory.armorInventory[it])
                processServerPacket(S04PacketEntityEquipment(mc.thePlayer.entityId, it + 1, mc.thePlayer.inventory.armorInventory[it]))
        }

        lastRiding = mc.thePlayer.ridingEntity
        wasSleeping = mc.thePlayer.isPlayerSleeping
        wasEating = mc.thePlayer.isEating
        heldItem = mc.thePlayer.heldItem
        repeat(4) {
            armors[it] = mc.thePlayer.inventory.armorInventory[it]
        }
    }

    @EventTarget
    fun onPacket(packetEvent: PacketEvent) {
        enabled ?: return

        replayFile ?: return

        val packet = packetEvent.packet

        if (EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet) == null)
            processClientPacket(packet) // starts with C then it is a packet from client
        else
            processServerPacket(packet) // OMFG FMLProxyPacket doesn't start with a C nor a S
    }

    @EventTarget
    fun onWorld(worldEvent: WorldEvent) {
        if (worldEvent.worldClient == null && replayFile != null)
            onDisconnect()
    }


    private fun processClientPacket(packet: Packet<*>) {
        when (packet) {

            is C03PacketPlayer -> {
                if (packet is C03PacketPlayer.C05PacketPlayerLook || packet is C03PacketPlayer.C06PacketPlayerPosLook)
                    processServerPacket(S19PacketEntityHeadLook(mc.thePlayer, (mc.thePlayer.rotationYawHead * 256 / 360).toInt().toByte()))

                save(S12PacketEntityVelocity(mc.thePlayer.entityId, mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ))
            }

            is C0APacketAnimation -> processServerPacket(S0BPacketAnimation(mc.thePlayer, 0))

            else -> {

            }
        }
    }

    public fun processServerPacket(packet: Packet<*>) {
        when (packet) {

            is S07PacketRespawn -> {
                val spawnPacket = spawnPlayer(mc.thePlayer)
                spawnPacket ?: return
                save(spawnPacket)
            }

            is S0DPacketCollectItem -> {
                if (mc.thePlayer != null)
                    return
                // Holy fuck what should I do with
                // [this](https://github.com/ReplayMod/ReplayMod/blob/8c2d7d096c3645108ae61aaf0eb36a353bd8c833/src/main/java/com/replaymod/recording/packet/PacketListener.java#L333)
            }

            is FMLProxyPacket -> {
                packet.toS3FPackets().forEach(this::save)
                return
            }

            else -> {
                save(packet)
            }
        }


    }

    public fun onConnect() {
        if (mc.currentServerData == null) {
            LiquidBounce.hud.addNotification(Notification("Not intended for sp"))
            enabled = false
            return
        }

        saveService = Executors.newSingleThreadExecutor()

        LiquidBounce.hud.addNotification(Notification("Started Recoding Replay"))

        val currentFile = File(
            LiquidBounce.fileManager.replaysDir,
            "${SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().time)}.mcpr"
        )

        replayFile = ZipReplayFile(ReplayStudio(), currentFile)

        replayMetaData.isSingleplayer = false
        replayMetaData.serverName = mc.currentServerData.serverIP;
        replayMetaData.generator = "LiquidBounce-Legacy-1.8"
        startTime = System.currentTimeMillis()
        replayMetaData.date = startTime
        replayMetaData.mcVersion = "1.8.9"

        packetOutputStream = DataOutputStream(replayFile!!.writePacketData())
    }

    private fun onDisconnect() {
        LiquidBounce.hud.addNotification(Notification("Replay saved"))
        replayMetaData.duration = lastSendPacket.toInt()
        saveMetaData()

        saveService!!.shutdown()
        saveService!!.awaitTermination(10, TimeUnit.SECONDS)

        synchronized(replayFile!!) {
            replayFile!!.save()
            replayFile!!.close()
        }
    }

    /*
    The following code is from ReplayMod
     */

    fun spawnPlayer(player: EntityPlayer): S0CPacketSpawnPlayer? {
        return try {
            val packet = S0CPacketSpawnPlayer()
            val bb = Unpooled.buffer()
            val pb = PacketBuffer(bb)
            pb.writeVarIntToBuffer(player.entityId)
            pb.writeUuid(EntityPlayer.getUUID(player.gameProfile))
            pb.writeInt(MathHelper.floor_double(player.posX * 32.0))
            pb.writeInt(MathHelper.floor_double(player.posY * 32.0))
            pb.writeInt(MathHelper.floor_double(player.posZ * 32.0))
            pb.writeByte((player.rotationYaw * 256.0f / 360.0f).toInt())
            pb.writeByte((player.rotationPitch * 256.0f / 360.0f).toInt())
            val itemStack = player.inventory.getCurrentItem()
            pb.writeShort(if (itemStack == null) 0 else Item.getIdFromItem(itemStack.item))
            player.dataWatcher.writeTo(pb)
            packet.readPacketData(pb)
            packet
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private val lastSavedMetaDataId = AtomicInteger()

    @Volatile
    private var serverWasPaused = false
    private var timePassedWhilePaused = 0L
    private var lastSendPacket = 0L

    private fun save(packet: Packet<*>) {

        if (packet is S0CPacketSpawnPlayer) {
            val uuids = replayMetaData.players.toHashSet()
            uuids.add(packet.player.toString())

            saveMetaData()
        }

        if (packet is S46PacketSetCompressionLevel)
            return

        val now = System.currentTimeMillis()

        val data = getPacketData(packet)
        saveService!!.submit {
            if (serverWasPaused) {
                timePassedWhilePaused = now - lastSendPacket
                serverWasPaused = false
            }
            val timeStamp = now - startTime - timePassedWhilePaused
            lastSendPacket = timeStamp

            try {
                packetOutputStream!!.writeInt(timeStamp.toInt())
                packetOutputStream!!.writeInt(data!!.size)
                packetOutputStream!!.write(data)
            } catch (e :Exception) {
            }

        }
    }

    private fun saveMetaData() {
        val id = lastSavedMetaDataId.incrementAndGet()
        saveService!!.submit {
            try {
                if (lastSavedMetaDataId.get() != id)
                    return@submit

                synchronized(replayFile!!) {
                    replayFile!!.writeMetaData(replayMetaData)
                }
            } catch (e :Exception) {

            }

        }
    }

    @Throws(IOException::class)
    private fun getPacketData(packet: Packet<*>): ByteArray? {
        if (packet is S0FPacketSpawnMob) {
            val p = packet
            if (p.field_149043_l == null) {
                p.field_149043_l = DataWatcher(null)
                if (p.func_149027_c() != null) {
                    for (wo in p.func_149027_c() as List<DataWatcher.WatchableObject>) {
                        p.field_149043_l.addObject(wo.dataValueId, wo.getObject())
                    }
                }
            }
        }
        if (packet is S0CPacketSpawnPlayer) {
            val p = packet
            if (p.watcher == null) {
                p.watcher = DataWatcher(null)
                if (p.func_148944_c() != null) {
                    for (wo in p.func_148944_c()) {
                        p.watcher.addObject(wo.dataValueId, wo.getObject())
                    }
                }
            }
        }


        val packetId = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet)
            ?: throw IOException("Unknown packet type:" + packet.javaClass)
        val byteBuf = Unpooled.buffer()
        val packetBuffer = PacketBuffer(byteBuf)
        packetBuffer.writeVarIntToBuffer(packetId)
        packet.writePacketData(packetBuffer)
        byteBuf.readerIndex(0)
        val array = ByteArray(byteBuf.readableBytes())
        byteBuf.readBytes(array)
        byteBuf.release()
        return array
    }
}