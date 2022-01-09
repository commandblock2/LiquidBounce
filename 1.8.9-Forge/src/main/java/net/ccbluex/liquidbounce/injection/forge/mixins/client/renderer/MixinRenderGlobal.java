package net.ccbluex.liquidbounce.injection.forge.mixins.client.renderer;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.commandblock2.liquidbounce.expr.features.module.modules.render.ExperimentalFreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Final
    @Shadow
    private Minecraft mc;

    @Inject(method = "renderEntities", at = @At(value = "TAIL"))
    public void renderEntities(Entity i, ICamera entity3, float flag, CallbackInfo ci) {
        if (mc.gameSettings.thirdPersonView == 0 && LiquidBounce.moduleManager.getModule(ExperimentalFreeCam.class).getState())
            mc.getRenderManager().renderEntitySimple(mc.thePlayer, flag);
    }
}
