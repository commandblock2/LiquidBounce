package net.ccbluex.liquidbounce.utils

class Insult(public val id: Int, public val text: String, public var tags: List<String>, public var used: Int) {
    override fun toString(): String {
        return "id:,${id},text:${text},tags:${tags},used:${used}"
    }
}