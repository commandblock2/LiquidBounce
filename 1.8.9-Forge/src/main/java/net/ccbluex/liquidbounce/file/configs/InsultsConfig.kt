/*
    Licensed under GPL-v3
* */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.Insult
import java.io.File
import java.io.IOException

class InsultsConfig(file: File?) : FileConfig(file) {
    public val insults = mutableListOf<Insult>()

    /**
     * Load config from file
     *
     * @throws IOException
     */
    override fun loadConfig() {
        val jsonElem = JsonParser().parse(file.readText())

        if (jsonElem !is JsonArray)
            return

        for (insultJson in jsonElem) {
            if (insultJson !is JsonObject)
                continue

            val text = insultJson.get("text")?.asString ?: continue

            val tagsJson = insultJson.get("tags")
            if (tagsJson !is JsonArray)
                return

            val tags = mutableListOf<String>()
            for (tagJson in tagsJson)
                tags.add(tagJson.asString ?: continue)

            val used = insultJson.get("used")?.asInt ?: continue


            insults.add(Insult(text, tags, used))
        }
    }


    /**
     * Save config to file
     *
     * @throws IOException
     */
    public override fun saveConfig() {
        val jsonArray = JsonArray()

        for (insult in insults) {
            val jsonInsult = JsonObject()

            jsonInsult.addProperty("text", insult.text)

            val jsonTags = JsonArray()
            for (tag in insult.tags)
                jsonTags.add(FileManager.PRETTY_GSON.toJsonTree(tag))

            jsonInsult.add("tags", jsonTags)
            jsonInsult.addProperty("used", insult.used)

            jsonArray.add(jsonInsult)
        }

        file.writeText(FileManager.PRETTY_GSON.toJson(jsonArray))
    }

}