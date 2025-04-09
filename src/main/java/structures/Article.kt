package structures

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import processors.TextProcessor
import processors.getBaseUrl
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

class Article(val preview_head: String,
              val preview_content: String,
              val preview_url: String,
              val source: String,
              val created_at: Instant,
              val head: String = "",
              val description: String = "",
              val content: String = "",
              var url: String = "",
              val image: String = "",
              val image_metadata: String = "",
              val published_at: Instant = Instant.EPOCH) : WithWords
{
    companion object {
        fun fromJson(s: String): Article {
            val elem = JsonParser.parseString(s)
            val obj = elem.asJsonObject
            fun getStr(attr: String): String{
               return if(obj.has(attr)) obj[attr].asString else ""
            }
            return Article(
                getStr("preview_head"),
                getStr("preview_content"),
                getStr("preview_url"),
                getStr("source"),
                Instant.parse(getStr("created_at")),
                getStr("head"),
                getStr("description"),
                getStr("content"),
                getStr("url"),
                getStr("image"),
                getStr("image_metadata"),
                Instant.parse(getStr("published_at")))
        }

        private var preparedStatementInsert: PreparedStatement? = null
        private var preparedStatementUpdate: PreparedStatement? = null
    }

    private var words: Words? = null

    constructor(sqlResult: ResultSet) : this(
        sqlResult.getString("preview_head"),
        sqlResult.getString("preview_content"),
        sqlResult.getString("preview_url"),
        sqlResult.getString("source"),
        sqlResult.getTimestamp("created_at").toInstant(),
        sqlResult.getString("head"),
        sqlResult.getString("description"),
        sqlResult.getString("content"),
        sqlResult.getString("url"),
        sqlResult.getString("image"),
        sqlResult.getString("image_metadata"),
        sqlResult.getTimestamp("published_at").toInstant()){
        summary_id = sqlResult.getInt("summary_id")
        id = sqlResult.getInt("id")
    }

    constructor(article: Article,
                head: String,
                description: String,
                content: String,
                url: String,
                image: String,
                published_at: Instant) : this(
        article.preview_head,
        article.preview_content,
        article.preview_url,
        article.source,
        article.created_at,
        head,
        description,
        content,
        url,
        image,
        article.image_metadata,
        published_at){
        summary_id = article.summary_id
        id = article.id
    }

    var id: Int = -1
    var summary_id: Int = -1

    override fun getWords(): Words {
        if(words == null){
            val text = StringBuilder()
                .append(" ").append(preview_head)
                .append(" ").append(preview_content)
                .append(" ").append(head)
                .append(" ").append(description)
                //.append(" ").append(content) // TODO this dilutes the clusters, maybe it works with different settings
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
        result.addProperty("created_at", created_at.toString())
        result.addProperty("published_at", published_at.toString())
        result.addProperty("id", id)
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
        // println("From: $normalized (${normalized.length})\nTo:   $result (${result.length})")
        return result
    }

    fun insertInto(connection: Connection) {
        if (preparedStatementInsert == null){
            preparedStatementInsert = connection.prepareStatement(
                "INSERT INTO articles (" +
                            "hash, preview_head, preview_content, preview_url, source, " +
                            "head, description, content, url, " +
                            "image, image_metadata, " +
                            "published_at, created_at" +
                        ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;"
            )
        }

        val stmt = preparedStatementInsert!!
        stmt.setString(1, normalized())
        stmt.setString(2, preview_head)
        stmt.setString(3, preview_content)
        stmt.setString(4, preview_url)
        stmt.setString(5, source)
        stmt.setString(6, head)
        stmt.setString(7, description)
        stmt.setString(8, content)
        stmt.setString(9, url)
        stmt.setString(10, image)
        stmt.setString(11, image_metadata)
        stmt.setTimestamp(12, Timestamp.from(published_at))
        stmt.setTimestamp(13, Timestamp.from(created_at))
        stmt.execute() // TODO check for insert
    }

    fun updateInto(connection: Connection): Boolean {
        if (preparedStatementUpdate == null){
            preparedStatementUpdate = connection.prepareStatement(
                "UPDATE articles SET " +
                        "hash = ?, preview_head = ?, preview_content = ?, preview_url = ?, source = ?, " +
                        "head = ?, description = ?, content = ?, url = ?, " +
                        "image = ?, image_metadata = ?, " +
                        "published_at = ?, summary_id = ? " +
                        "WHERE id = ?;"
            )
        }

        if (id == -1){
            throw Exception("Id is not set, can't do update")
        }

        val stmt = preparedStatementUpdate!!
        stmt.setString(1, normalized()) // Maybe we should not update the hash
        stmt.setString(2, preview_head)
        stmt.setString(3, preview_content)
        stmt.setString(4, preview_url)
        stmt.setString(5, source)
        stmt.setString(6, head)
        stmt.setString(7, description)
        stmt.setString(8, content)
        stmt.setString(9, url)
        stmt.setString(10, image)
        stmt.setString(11, image_metadata)
        stmt.setTimestamp(12, Timestamp.from(published_at))
        stmt.setInt(13, summary_id)
        stmt.setInt(14, id)
        // We don't update the created_at
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
