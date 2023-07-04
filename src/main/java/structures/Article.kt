package structures

import com.google.gson.JsonObject
import processors.TextProcessor
import processors.getBaseUrl
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime

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

    fun insertInto(connection: Connection): Boolean {
        if (preparedStatement == null){
            preparedStatement = connection.prepareStatement("INSERT INTO articles (hash, head, content, url, source, created) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;")
        }

//        var normal = normalized()
//        if(normal.length >= 300){
//            println(normal)
//            normal = normal.substring(0, 299)
//            // TODO put into normalize method
//        }

        preparedStatement!!.setString(1, normalized())
        preparedStatement!!.setString(2, header)
        preparedStatement!!.setString(3, content)
        preparedStatement!!.setString(4, url)
        preparedStatement!!.setString(5, source)
        preparedStatement!!.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()))
        return preparedStatement!!.execute()
    }

    companion object {
        private var preparedStatement: PreparedStatement? = null
    }
}
