package net.ccbluex.liquidbounce.render.fxweb

import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform
import javafx.stage.Stage
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.util.math.MatrixStack

object FxEngine {

    val handle = mc.window.handle

    private val views = mutableListOf<View>()

    fun init() {
        Platform.startup {  }
    }

    fun newSplashView(): View
    = View(RenderLayer.SPLASH_LAYER).also { views += it }

    fun render(renderLayer: RenderLayer, matrices: MatrixStack) {
        views.filter { it.layer == renderLayer }
            .forEach{ it.render(matrices) }
    }

    fun removeView(view: View) {
        views -= view
    }
}

enum class RenderLayer {
    OVERLAY_LAYER, SCREEN_LAYER, SPLASH_LAYER
}
