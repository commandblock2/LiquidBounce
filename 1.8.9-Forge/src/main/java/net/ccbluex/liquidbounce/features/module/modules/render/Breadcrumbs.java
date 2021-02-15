/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.LinkedList;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "Breadcrumbs", description = "Leaves a trail behind you.", category = ModuleCategory.RENDER)
public class Breadcrumbs extends Module {
    public final IntegerValue colorRedValue = new IntegerValue("R", 255, 0, 255);
    public final IntegerValue colorGreenValue = new IntegerValue("G", 179, 0, 255);
    public final IntegerValue colorBlueValue = new IntegerValue("B", 72, 0, 255);
    public final BoolValue colorRainbow = new BoolValue("Rainbow", false);
    private final LinkedList<Vec3> positions = new LinkedList<>();

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        final Color color = colorRainbow.get() ? ColorUtils.rainbow() : new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get());

        synchronized (positions) {
            RenderUtils.drawPoses(color, positions);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        synchronized (positions) {
            positions.add(new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ));
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null)
            return;

        synchronized (positions) {
            positions.add(new Vec3(mc.thePlayer.posX,
                    mc.thePlayer.getEntityBoundingBox().minY + (mc.thePlayer.getEyeHeight() * 0.5f),
                    mc.thePlayer.posZ));
            positions.add(new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ));
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        synchronized (positions) {
            positions.clear();
        }
        super.onDisable();
    }
}
