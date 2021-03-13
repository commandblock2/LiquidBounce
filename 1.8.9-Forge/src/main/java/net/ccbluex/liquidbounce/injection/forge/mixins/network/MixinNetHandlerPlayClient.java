/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import io.netty.buffer.Unpooled;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EntityMovementEvent;
import net.ccbluex.liquidbounce.features.special.AntiForge;
import net.ccbluex.liquidbounce.features.special.ReplayRecording;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.*;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow
    @Final
    private NetworkManager netManager;

    @Shadow
    private Minecraft gameController;

    @Shadow
    private WorldClient clientWorldController;

    @Shadow
    public int currentServerMaxPlayers;

    @Shadow
    private Map<UUID, NetworkPlayerInfo> playerInfoMap;

    @Inject(method = "handleResourcePack", at = @At("HEAD"), cancellable = true)
    private void handleResourcePack(final S48PacketResourcePackSend p_handleResourcePack_1_, final CallbackInfo callbackInfo) {
        final String url = p_handleResourcePack_1_.getURL();
        final String hash = p_handleResourcePack_1_.getHash();

        try {
            final String scheme = new URI(url).getScheme();
            final boolean isLevelProtocol = "level".equals(scheme);

            if(!"http".equals(scheme) && !"https".equals(scheme) && !isLevelProtocol)
                throw new URISyntaxException(url, "Wrong protocol");

            if(isLevelProtocol && (url.contains("..") || !url.endsWith("/resources.zip")))
                throw new URISyntaxException(url, "Invalid levelstorage resourcepack path");
        }catch(final URISyntaxException e) {
            ClientUtils.getLogger().error("Failed to handle resource pack", e);
            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleJoinGame", at = @At("HEAD"), cancellable = true)
    private void handleJoinGameWithAntiForge(S01PacketJoinGame packetIn, final CallbackInfo callbackInfo) {
        if(!AntiForge.enabled || !AntiForge.blockFML || Minecraft.getMinecraft().isIntegratedServerRunning())
            return;

        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (NetHandlerPlayClient) (Object) this, gameController);
        this.gameController.playerController = new PlayerControllerMP(gameController, (NetHandlerPlayClient) (Object) this);
        this.clientWorldController = new WorldClient((NetHandlerPlayClient) (Object) this, new WorldSettings(0L, packetIn.getGameType(), false, packetIn.isHardcoreMode(), packetIn.getWorldType()), packetIn.getDimension(), packetIn.getDifficulty(), this.gameController.mcProfiler);
        this.gameController.gameSettings.difficulty = packetIn.getDifficulty();
        this.gameController.loadWorld(this.clientWorldController);
        this.gameController.thePlayer.dimension = packetIn.getDimension();
        this.gameController.displayGuiScreen(new GuiDownloadTerrain((NetHandlerPlayClient) (Object) this));
        this.gameController.thePlayer.setEntityId(packetIn.getEntityId());
        this.currentServerMaxPlayers = packetIn.getMaxPlayers();
        this.gameController.thePlayer.setReducedDebug(packetIn.isReducedDebugInfo());
        this.gameController.playerController.setGameType(packetIn.getGameType());
        this.gameController.gameSettings.sendSettingsToServer();
        this.netManager.sendPacket(new C17PacketCustomPayload("MC|Brand", (new PacketBuffer(Unpooled.buffer())).writeString(ClientBrandRetriever.getClientModName())));
        callbackInfo.cancel();
    }

    @Inject(method = "handleEntityMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z"))
    private void handleEntityMovementEvent(S14PacketEntity packetIn, final CallbackInfo callbackInfo) {
        final Entity entity = packetIn.getEntity(this.clientWorldController);

        if(entity != null)
            LiquidBounce.eventManager.callEvent(new EntityMovementEvent(entity));
    }

    @Inject(method = "handlePlayerListItem", at = @At("HEAD"))
    public void recordOwnJoin(S38PacketPlayerListItem packet, CallbackInfo callbackInfo) {
        if (ReplayRecording.INSTANCE.getEnabled() && packet.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
            for (S38PacketPlayerListItem.AddPlayerData data : packet.getEntries()) {
                if (data.getProfile() == null || data.getProfile().getId() == null) continue;
                // Only add spawn packet for our own player and only if he isn't known yet
                if (data.getProfile().getId().equals(Minecraft.getMinecraft().thePlayer.getGameProfile().getId())
                        && !playerInfoMap.containsKey(data.getProfile().getId())) {
                    ReplayRecording.INSTANCE.processServerPacket(ReplayRecording.INSTANCE.spawnPlayer(Minecraft.getMinecraft().thePlayer));
                }
            }
        }

    }

    @Inject(method = "handleRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(S07PacketRespawn packet, CallbackInfo ci) {
        if (ReplayRecording.INSTANCE.getEnabled()) {
            ReplayRecording.INSTANCE.processServerPacket(ReplayRecording.INSTANCE.spawnPlayer(Minecraft.getMinecraft().thePlayer));
        }
    }
}
