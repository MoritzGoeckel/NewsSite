package structures

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import printInfo
import processors.TextProcessor
import processors.getBaseUrl
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime

class Article(val header: String, val content: String, val url: String, val source: String, var originalUrl: String, words: Words): Words(words.text, words.words) {

    companion object {
        private var preparedStatement: PreparedStatement? = null
    }

    var details: ArticleDetails? = null

    constructor(header: String, content: String, url: String, source: String): this(header, content, url, source, "", Words())
    constructor(header: String, content: String, url: String, source: String, processor: TextProcessor): this(header, content, url, source, "", processor.makeWords(header))
    constructor(header: String, content: String, url: String, source: String, originalUrl: String, processor: TextProcessor): this(header, content, url, source, originalUrl, processor.makeWords(header))

    fun toJson(): JsonObject{
        val result = JsonObject()
        result.addProperty("header", text)
        result.addProperty("content", content)
        result.addProperty("url", url)
        result.addProperty("original_url", originalUrl)
        result.addProperty("source", getBaseUrl(source))
        if(details != null) {
            result.add("details", details!!.toJson())
        }
        return result
    }

    fun normalized(): String{
        return getBaseUrl(source).toLowerCase() + header.toLowerCase().filter { it.isLetterOrDigit() }
    }

    fun insertInto(connection: Connection): Boolean {
        if (preparedStatement == null){
            preparedStatement = connection.prepareStatement("INSERT INTO articles (hash, head, content, url, original_url, source, created_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;")
        }

        preparedStatement!!.setString(1, normalized())
        preparedStatement!!.setString(2, header)
        preparedStatement!!.setString(3, content)
        preparedStatement!!.setString(4, url)
        preparedStatement!!.setString(5, originalUrl)
        preparedStatement!!.setString(6, source)
        preparedStatement!!.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
        return preparedStatement!!.execute()
    }

    fun loadDetails(connection: Connection): Boolean{
        val selectArticleDetails = connection.prepareStatement("SELECT * FROM article_details WHERE article_url = ?")
        selectArticleDetails.setString(1, url)
        val result = selectArticleDetails.executeQuery()

        if (result.next()) {
            val imageMetadata = result.getString("image_metadata")
            val imageMetadataJson = JsonParser.parseString(imageMetadata).asJsonObject
            details = ArticleDetails(
                title = result.getString("title"),
                description = result.getString("description"),
                image = result.getString("image"),
                date = result.getTimestamp("published_at"),
                articleUrl = result.getString("article_url"),
                url = result.getString("url"),
                content = result.getString("content"),
                summary = result.getString("summary"),
                imageCenter = Point(imageMetadataJson)
            )
        }

        return details != null
    }

    fun setOriginalUrl(newOriginalUrl: String, connection: Connection): Boolean {
        originalUrl = newOriginalUrl
        val statement = connection.prepareStatement("UPDATE articles SET original_url = ? WHERE hash = ?;")
        statement.setString(1, originalUrl)
        statement.setString(2, normalized())
        return statement.execute()
    }
}
