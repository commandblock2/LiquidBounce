package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.render;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCamAlternative;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.ccbluex.liquidbounce.utils.client.ClientUtilsKt.chat;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow
    private Vec3d pos;

    @Shadow
    @Final
    private BlockPos.Mutable blockPos;

    @Inject(method = "setPos(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    public void injectFreeCam(Vec3d pos, CallbackInfo callbackInfo) {
        final var freeCam = ModuleFreeCamAlternative.INSTANCE;

        this.pos = pos.add(freeCam.getXOffset(), freeCam.getYOffset(), freeCam.getZOffset());
        this.blockPos.set(pos.x, pos.y, pos.z);
        callbackInfo.cancel();
    }
}
