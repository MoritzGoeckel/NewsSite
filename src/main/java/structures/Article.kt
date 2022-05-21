package structures

import com.google.gson.JsonObject
import parsers.ArticleDetails
import processors.TextProcessor
import processors.getBaseUrl

class Article(val header: String, val content: String, val url: String, val source: String, words: Words): Words(words.text, words.words) {
    constructor(header: String, content: String, url: String, source: String): this(header, content, url, source, Words())
    constructor(header: String, content: String, url: String, source: String, processor: TextProcessor): this(header, content, url, source, processor.makeWords(header))

    var details: ArticleDetails? = null

    fun toJson(): JsonObject{
        val result = JsonObject()
        result.addProperty("header", text)
        result.addProperty("content", content)
        result.addProperty("url", url)
        result.addProperty("source", getBaseUrl(source))
        if(details != null) {
            result.add("details", details!!.toJson())
        }
        return result
    }

    fun normalized(): String{
        return getBaseUrl(source).lowercase() + header.lowercase().filter { it.isLetterOrDigit() }
    }
}
