package net.ccbluex.liquidbounce.render.fxweb

import javafx.application.Platform
import javafx.scene.web.WebView
import net.ccbluex.liquidbounce.render.fxweb.theme.Page
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.longedSize
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.util.math.MatrixStack

class View(val layer: RenderLayer) {
    lateinit var view: WebView
    var viewingPage: Page? = null

    init {
        // Setup view
        val (width, height) = mc.window.longedSize

        //view.lock(FxEngine.renderer.get().createView(width, height, viewConfig))
        //view.get().setViewListener(ViewListener())
        //view.get().setLoadListener(ViewLoadListener(this))

        Platform.runLater {
            view = WebView()
        }

        // Setup JS bindings
        //context = UltralightJsContext(this, ultralightView)

        logger.debug("Successfully created new view")
    }

    /**
     * Loads the specified [page]
     */
    fun loadPage(page: Page) {

        Platform.runLater {
            // Unregister listeners
            //context.events._unregisterEvents()

            if (viewingPage != page && viewingPage != null) {
                page.close()
            }

            view.engine.load(page.viewableFile)
            view.isVisible = true
            viewingPage = page
            logger.debug("Successfully loaded page ${page.name} from ${page.viewableFile}")
        }
    }

    fun render(matrices: MatrixStack) {

    }
}
