/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ModuleFreeCamAlternative : Module("FreeCamAlternative", Category.RENDER) {
    private val speed by float("Speed", 1f, 0.1f..2f)
    private val resetOnDisable by boolean("ResetOnDisable", true)
    private val transformMode by enumChoice("TransformMode", TransformMode.RELATIVEROTATION, TransformMode.values())

    var xOffset = 0.0
    var yOffset = 0.0
    var zOffset = 0.0

    private var yaw = 0.0
    private var pitch = 0.0
    private var distance = 0.0

    override fun enable() {
        xOffset = 0.0
        yOffset = 0.0
        zOffset = 0.0

        yaw = 0.0
        pitch = 0.0
        distance = 0.0
    }

    override fun disable() {
        if (resetOnDisable) {
            xOffset = 0.0
            yOffset = 0.0
            zOffset = 0.0

            yaw = 0.0
            pitch = 0.0
            distance = 0.0
        }
    }

    val renderHandler = handler<EngineRenderEvent> { event ->

        if (transformMode == TransformMode.RELATIVEROTATION) {
            val rotationVec =
                Rotation(player.yaw + yaw.toFloat(), -player.pitch + pitch.toFloat())
                    .rotationVec.multiply(distance)

            xOffset = rotationVec.x
            yOffset = rotationVec.y
            zOffset = rotationVec.z
        }

        val angle = Math.toRadians(player.directionYaw.toDouble())

        if (mc.options.keyForward.isPressed ||
            mc.options.keyBack.isPressed ||
            mc.options.keyLeft.isPressed ||
            mc.options.keyRight.isPressed
        ) {
            xOffset += -sin(angle) * speed
            zOffset += cos(angle) * speed
        }

        yOffset += when {
            mc.options.keyJump.isPressed -> speed
            mc.options.keySneak.isPressed -> -speed
            else -> 0.0
        }.toDouble()

        if (transformMode == TransformMode.RELATIVEROTATION) {
            val rotation = RotationManager.makeRotation(Vec3d(xOffset, yOffset, zOffset), Vec3d(0.0, 0.0, 0.0))
            yaw = rotation.yaw.toDouble() - player.yaw
            pitch = rotation.pitch.toDouble() + player.pitch
            distance = sqrt(xOffset * xOffset + yOffset * yOffset + zOffset * zOffset)
        }


    }

    enum class TransformMode(override val choiceName: String) : NamedChoice {
        RELATIVETRANSLATION("RelativeTranslation"), RELATIVEROTATION("RelativeRotation")
    }
}
