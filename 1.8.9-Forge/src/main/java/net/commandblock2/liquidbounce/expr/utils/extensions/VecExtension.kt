package net.commandblock2.liquidbounce.expr.utils.extensions

import net.minecraft.util.Vec3

fun Vec3.multiply(float: Float) : Vec3 {
    return Vec3(xCoord * float, yCoord * float, zCoord * float)
}