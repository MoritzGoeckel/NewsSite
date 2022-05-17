package structures

import com.google.gson.JsonObject
import processors.TextProcessor

class Article(val header: String, val content: String, val url: String, val source: String, words: Words): Words(words.text, words.words) {
    constructor(header: String, content: String, url: String, source: String): this(header, content, url, source, Words())
    constructor(header: String, content: String, url: String, source: String, processor: TextProcessor): this(header, content, url, source, processor.makeWords(header))

    fun toJson(): JsonObject{
        val result = JsonObject()
        result.addProperty("header", header)
        result.addProperty("content", content)
        result.addProperty("url", url)
        result.addProperty("source", source)
        return result
    }
}
