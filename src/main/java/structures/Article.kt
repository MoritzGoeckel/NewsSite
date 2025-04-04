package structures

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import printInfo
import processors.TextProcessor
import processors.getBaseUrl
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

class Article(val preview_head: String,
              val preview_content: String,
              val preview_url: String,
              val source: String,
              val head: String = "",
              val description: String = "",
              val content: String = "",
              var url: String = "",
              val image: String = "",
              val image_metadata: String = "",
              val published_at: Timestamp = Timestamp.from(Instant.EPOCH)) : WithWords
{
    companion object {
        private var preparedStatement: PreparedStatement? = null
        private var words: Words? = null
    }

    constructor(sqlResult: ResultSet) : this(
        sqlResult.getString("preview_head"),
        sqlResult.getString("preview_content"),
        sqlResult.getString("preview_url"),
        sqlResult.getString("source"),
        sqlResult.getString("head"),
        sqlResult.getString("description"),
        sqlResult.getString("content"),
        sqlResult.getString("url"),
        sqlResult.getString("image"),
        sqlResult.getString("image_metadata"),
        sqlResult.getTimestamp("published_at")){
        created_at = sqlResult.getTimestamp("created_at").toInstant()
        summary_id = sqlResult.getInt("summary_id")
        id = sqlResult.getInt("id")
    }

    var id: Int = -1
    var summary_id: Int = -1
    var created_at: Instant = Instant.EPOCH

    override fun getWords(): Words {
        if(words == null){
            val text = StringBuilder()
                .append(" ").append(preview_head)
                .append(" ").append(preview_content)
                .append(" ").append(head)
                .append(" ").append(description)
                .append(" ").append(content)
                .toString()
            words = TextProcessor(Language.DE).makeWordsKeepText(text)
        }
        return words!!
    }

    fun toJson(): JsonObject{
        val result = JsonObject()
        result.addProperty("preview_head", preview_head)
        result.addProperty("preview_content", preview_content)
        result.addProperty("preview_url", preview_url)
        result.addProperty("source", source)
        result.addProperty("base_url", getBaseUrl(source))
        result.addProperty("url", url)
        result.addProperty("head", head)
        result.addProperty("description", description)
        result.addProperty("content", content)
        result.addProperty("image", image)
        result.addProperty("image_metadata", image_metadata)
        result.addProperty("published_at", published_at.toString())
        return result
    }

    fun normalized(): String{
        val MAX_LENGTH: Int = 350
        val normalized = getBaseUrl(source).toLowerCase() + preview_head.toLowerCase().filter { it.isLetterOrDigit() }
        if (normalized.length < MAX_LENGTH){
            return normalized;
        }

        val factor: Int = normalized.length / MAX_LENGTH
        val sb = StringBuilder()
        for (i in normalized.indices){
            if (sb.length >= MAX_LENGTH) break
            if (i % factor == 0) sb.append(normalized[i])
        }
        val result = sb.toString()
        // printInfo("normalize", "From: $normalized (${normalized.length})\nTo:   $result (${result.length})")
        return result
    }

    fun insertInto(connection: Connection): Boolean {
        if (preparedStatement == null){
            preparedStatement = connection.prepareStatement(
                "INSERT INTO articles (" +
                            "hash, preview_head, preview_content, preview_url, source, " +
                            "head, description, content, url, " +
                            "image, image_metadata, " +
                            "published_at" +
                        ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;"
            )
        }

        val stmt = preparedStatement!!
        stmt.setString(1, normalized())
        stmt.setString(2, preview_head)
        stmt.setString(3, preview_content)
        stmt.setString(4, preview_url)
        stmt.setString(5, source)
        stmt.setString(6, head)
        stmt.setString(7, description)
        stmt.setString(8, content)
        stmt.setString(9, preview_url)
        stmt.setString(10, image)
        stmt.setString(11, image_metadata)
        stmt.setTimestamp(12, published_at)
        return stmt.execute()
    }

    /*fun setOriginalUrl(newOriginalUrl: String, connection: Connection): Boolean {
        originalUrl = newOriginalUrl
        val statement = connection.prepareStatement("UPDATE articles SET original_url = ? WHERE hash = ?;")
        statement.setString(1, originalUrl)
        statement.setString(2, normalized())
        return statement.execute()
    }*/
}
