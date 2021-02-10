package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.Insult
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import java.io.File

class InsultCommand : Command("Insults", arrayOf("iss")) {
    override fun execute(args: Array<String>) {

        if (args.size == 1) {
            overallUsage()
            return
        }

        val insults = LiquidBounce.fileManager.insultsConfig.insults

        when (args[1].toLowerCase()) {
            "new" -> {
                when (args.size) {
                    2 -> chat("insults new <message> [tags]")
                    3 -> {
                        insults.add(Insult(insults.size, args[2], emptyList(), 0))
                        chat("messaged added" + insults.last())
                    }
                    else -> {
                        insults.add(Insult(insults.size, args[2], args.slice(3 until args.size), 0))
                        chat("messaged added" + insults.last())
                    }
                }
            }

            "import" -> {
                try {
                    val file = MiscUtils.openFileChooser() ?: return
                    val fileName = file.name

                    File(fileName).forEachLine {
                        insults.add(Insult(insults.size, it, emptyList(), 0))
                    }
                } catch (t: Throwable) {
                    ClientUtils.getLogger().error("Something went wrong while importing a insult text.", t)
                    chat("${t.javaClass.name}: ${t.message}")
                }
            }

            "tag" -> {
                if (args.size < 4)
                    chat("insults tag <id> <tags>")
                else {
                    try {
                        val id = args[2].toInt()
                        insults[id].tags = args.slice(3 until args.size)
                        chat(insults.last().toString())
                    } catch (t:Throwable) {
                        chat("insult tag <id> <tags>")
                    }

                }
            }

            "remove" -> {
                when (args.size) {
                    3 -> try {
                        val insult = insults[args[2].toInt()]
                        insults.remove(insult)
                        chat("insult " + insult.toString() + "removed")
                    } catch (t: Throwable) {
                        chat(t.toString())
                    }

                    else -> chat("insults remove <id>")
                }
            }

            "view" -> {
                when (args.size) {
                    2 -> chat("You have ${insults.size} insults, use \"insult view [id]\" to view one")
                    3 -> try {
                        chat(insults[args[2].toInt()].toString())
                    } catch (t: Throwable) {
                        chat(t.toString())
                    }

                    else -> chat("insults view <id>")
                }
            }

            "random" -> mc.thePlayer.sendChatMessage(insults.random().text)

            else -> overallUsage()
        }

        LiquidBounce.fileManager.insultsConfig.saveConfig()
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("new", "import", "tag", "view", "random")
                .filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }

    private fun overallUsage() {chat("insults <new/import/tag/remove/view/random> [...]")}
}