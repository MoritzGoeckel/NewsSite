package summarizer

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import printInfo
import structures.Original
import java.security.InvalidParameterException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class GPT(private val apiKey: String) {
    private val model = "gpt-3.5-turbo-16k"
    // gpt-4, gpt-4-0613, gpt-4-32k, gpt-4-32k-0613, gpt-3.5-turbo, gpt-3.5-turbo-0613, gpt-3.5-turbo-16k, gpt-3.5-turbo-16k-0613

    private val temperature = 1.0
    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    private val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()

    private val charsPerToken = 4
    private val maximumTokens = 2000 // is actually 4097, but that is too tight

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

    fun maxLength(): Int{
        return maximumTokens * charsPerToken;
    }

    fun doRequest(request: String): JsonObject {
        /*
        curl https://api.openai.com/v1/chat/completions \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $OPENAI_API_KEY" \
          -d '{
             "model": "gpt-3.5-turbo",
             "messages": [{"role": "user", "content": "Say this is a test!"}],
             "temperature": 0.9
           }'
         */

        lastRequest = Instant.now()

        val dataJson = JsonObject()
        dataJson.addProperty("model", model)
        dataJson.addProperty("temperature", temperature)

        val message = JsonObject()
        message.addProperty("role", "user")
        message.addProperty("content", request)
        val messages = JsonArray()
        messages.add(message)
        dataJson.add("messages", messages)

        val body: RequestBody = dataJson.toString().toRequestBody(mediaType)
        val httpRequest: Request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        val jsonResponse = response.body!!.string()
        val rootObject = JsonParser.parseString(jsonResponse).asJsonObject

        // Usually happens if you give improper arguments (either an unrecognized argument or bad argument value)
        if (rootObject.has("error"))
            throw IllegalArgumentException(rootObject["error"].asJsonObject["message"].asString)

        return rootObject
    }

    fun generateOriginal(text: String, images: List<String>): Original{
        // https://chat.openai.com/
        val prompt = StringBuilder()
        prompt.append("Du bist ein professioneller Journalist. Um einen Artikel zu schreiben, hast du die Informationen in der nächsten Zeile:\n")
        prompt.append(removeSpecial(text))
        prompt.append("\n")
        prompt.append("Schreibe nun einen Artikel zu dem Thema. Schreibe in die erste Zeile eine kurze Überschrift, gefolgt von dem Artikel in den nächsten Absätzen.")

        if(text.length > maxLength()){
            throw InvalidParameterException("Input (${text.length}) is too long for gpt, get it bellow ${maxLength()}")
        }

        val requestString = prompt.toString()

        val answer = doRequest(requestString)
        val jsonContent = answer.get("choices").asJsonArray.first().asJsonObject.get("message").asJsonObject.get("content").asString

        val regex = "Zusammenfassung:([\\s\\S]+)Überschrift:([\\s\\S]+)|Überschrift:([\\s\\S]+)Zusammenfassung:([\\s\\S]+)".toRegex()

        // printInfo("GPT", "Request: $requestString")
        // printInfo("GPT", "Answer: $jsonContent")

        val lines = jsonContent.split("\n")

        var header = lines.first()
        if (header.startsWith('"') && header.endsWith('"')){
            header = header.trim('"')
        }
        if (header.startsWith('\'') && header.endsWith('\'')){
            header = header.trim('\'')
        }
        val content = lines.subList(1, lines.size - 1).joinToString("\n")

        assert(header.isNotEmpty())
        assert(content.isNotEmpty())

        printInfo("GPT", "Generated original: ${header.trim()}")

        return Original(header.trim(), content.trim(), images, headerToUrl(header).trim(), text, jsonContent)
    }

    private fun headerToUrl(header: String): String {
        val url = header.toLowerCase().toCharArray().map {
            if((it in 'a'..'z') || (it in '0'..'9')) {
                it
            } else {
                '_'
            }
        }.toCharArray()

        val urlStr = String(url)

        return if (urlStr.length < 300) urlStr
               else urlStr.substring(0, 300)
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