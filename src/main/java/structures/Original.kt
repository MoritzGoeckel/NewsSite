package structures

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import printInfo
import java.sql.Connection

data class Original(val head: String, val content: String, val images: List<String>, val url: String, val rawIn: String, val rawOut: String) {
    fun insertInto(connection: Connection): Boolean {
        val preparedStatement = connection.prepareStatement("INSERT INTO originals (url, head, content, media, raw_in, raw_out) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;")

        printInfo("Original", "Inserted original: $url")

        val mediaJson = JsonArray()
        images.forEach{ mediaJson.add(it) }

        preparedStatement.setString(1, url)
        preparedStatement.setString(2, head)
        preparedStatement.setString(3, content)
        preparedStatement.setString(4, mediaJson.toString())
        preparedStatement.setString(5, rawIn)
        preparedStatement.setString(6, rawOut)
        return preparedStatement.execute()
    }

    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("url", url)
        result.addProperty("head", head)
        result.addProperty("content", content)

        val media = JsonArray()
        images.forEach { media.add(it) }
        result.add("media", media)

        // result.addProperty("raw_in", raw_in)
        // result.addProperty("raw_out", raw_out)
        return result
    }

    companion object {
        private val urlToOriginal = mutableMapOf<String, Original>()
        // TODO remove from cache some time

        private fun parseImages(text: String): List<String>{
            return parseString(text).asJsonArray.map { it.asString }
        }

        fun selectByUrl(url: String, connection: Connection): Original{
            val preparedStatement = connection.prepareStatement("SELECT * FROM originals where url = ?")
            preparedStatement.setString(1, url)

            val result = preparedStatement.executeQuery()
            if(result.next()) {
                return Original(
                    head = result.getString("head"),
                    content = result.getString("content"),
                    images = parseImages(result.getString("media")),
                    url = result.getString("url"),
                    rawIn = result.getString("raw_in"),
                    rawOut = result.getString("raw_out"))
            } else {
                throw Exception("No result!")
            }
        }

        fun getOriginal(url: String, connection: Connection): Original{
            var original = urlToOriginal[url]
            if(original != null){
                return original
            }

            original = selectByUrl(url, connection)
            urlToOriginal[original.url] = original
            return original
        }
    }
}