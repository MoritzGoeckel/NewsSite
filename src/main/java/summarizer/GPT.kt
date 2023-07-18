package summarizer

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import printError
import printInfo
import structures.Original
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs


class GPT(private val apiKey: String) {
    companion object {
        var lastRequest: Instant = Instant.EPOCH

        val normalChars = setOf('^', '[', ']', '_', '{', '}', '|', '€')
        val specialChars = hashMapOf(
            '`' to '\'', '„' to '"', '”' to '"', '“' to '"', '‘' to '\'', '’' to '\'', '´' to '\'',
            '–' to '-', '—' to '-', '›' to '>', '«' to '<', '»' to '>', '«' to '<',
            '˄' to '^', 'ˆ' to '^', 'ˉ' to '-', '˵' to '"', '˶' to '"', '˷' to '"',
            '˸' to ':')
        val allowedChars = setOf('ß', 'Ö', 'ö', 'Ü', 'ü', 'Ä', 'ä')
        //'\\' to '/',
    }

    fun waitUntilAvailable() {
        val waitingTime = ChronoUnit.MILLIS.between(Instant.now(), lastRequest.plusSeconds(22))
        Thread.sleep(abs(waitingTime))
    }

    fun isAvailable(): Boolean{
        return ChronoUnit.SECONDS.between(lastRequest, Instant.now()) >= 21
    }

    fun doRequest(request: String): JsonObject {
        /*
        curl https://api.openai.com/v1/chat/completions \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $OPENAI_API_KEY" \
          -d '{
             "model": "gpt-3.5-turbo",
             "messages": [{"role": "user", "content": "Say this is a test!"}],
             "temperature": 0.7
           }'
         */

        lastRequest = Instant.now()

        val dataJson = JsonObject()
        dataJson.addProperty("model", "gpt-3.5-turbo")
        dataJson.addProperty("temperature", 0.7)

        val message = JsonObject()
        message.addProperty("role", "user")
        message.addProperty("content", request)
        val messages = JsonArray()
        messages.add(message)
        dataJson.add("messages", messages)

        val connection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        // connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true

        // Write
        val dataStr = dataJson.toString()

        // validateUTF8(dataStr) // TODO

        printInfo("GPT", dataStr)
        connection.outputStream.use { os ->
            val input = dataStr.encodeToByteArray (0, dataStr.length, true)
            input.decodeToString() // TODO

            os.write(input, 0, input.size)
        }

        if(connection.responseCode != 200) {
            printError("GPT", "${connection.responseCode}: ${connection.responseMessage}")
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream, "utf-8"))
        val response = reader.readText()

        return JsonParser.parseString(response).asJsonObject
    }

    fun generateOriginal(text: String, images: List<String>): Original{
        // https://chat.openai.com/
        val prompt = StringBuilder()
        prompt.append("Schreibe eine Zusammenfassung und eine Überschrift für den folgenden Text:\n")
        prompt.append(removeSpecial(text))

        val requestString = prompt.toString()

        val answer = doRequest(requestString)
        val jsonContent = answer.get("choices").asJsonArray.first().asJsonObject.get("message").asJsonObject.get("content").asString

        val regex = "Zusammenfassung:([\\s\\S]+)Überschrift:([\\s\\S]+)|Überschrift:([\\s\\S]+)Zusammenfassung:([\\s\\S]+)".toRegex()

        printInfo("GPT", jsonContent)

        val matches = regex.matchEntire(jsonContent)
        assert(matches != null)
        assert(matches!!.groups.size == 5)

        var header = ""
        var content = ""

        run {
            val match = matches.groups[1]
            if (match?.value?.isNotEmpty() == true) {
                content = match.value
            }
        }

        run {
            val match = matches.groups[2]
            if (match?.value?.isNotEmpty() == true) {
                header = match.value
            }
        }

        run {
            val match = matches.groups[3]
            if (match?.value?.isNotEmpty() == true) {
                header = match.value
            }
        }

        run {
            val match = matches.groups[4]
            if (match?.value?.isNotEmpty() == true) {
                content = match.value
            }
        }

        assert(header.isNotEmpty())
        assert(content.isNotEmpty())

        return Original(header, content, images, headerToUrl(header), text, jsonContent)
    }

    private fun headerToUrl(header: String): String {
        return header.lowercase().toCharArray().map {
            if((it in 'a'..'z') || (it in '0'..'9')) {
                it
            } else {
                '_'
            }
        }.toString()
    }

    private fun mapSpecialToNormal(c: Char): Char?{
        if (c.isISOControl()) return null

        val isNormal =
                c == ' '            ||
                c in '!'..'/' ||
                c in '0'..'9' ||
                c in ':'..'@' ||
                c in 'A'..'Z' ||
                c in 'a'..'z' ||
                c in normalChars    ||
                c in allowedChars

        if(isNormal){
            return c
        }

        val maybe = specialChars[c]
        if(maybe != null) return maybe

        if(c.category == CharCategory.CURRENCY_SYMBOL) return c

        return null
    }

    private fun removeSpecial(text: String): String {
        var result = text.replace('\t', ' ')
        result = result.replace("\\n[^n]".toRegex(), "")
        val list = result.map {
            mapSpecialToNormal(it)
        }.filterNotNull()
        return String(list.toCharArray())
    }
}