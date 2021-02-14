package net.ccbluex.liquidbounce.utils

class Insult(public val text: String, public var tags: List<String>, public var used: Int) {
    override fun toString(): String {
        return "text:${text},tags:${tags},used:${used}"
    }
}