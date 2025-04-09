package structures

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import util.printInfo
import java.sql.Connection
import java.sql.ResultSet

data class Original(val head: String,
                    val teaser: String,
                    val content: String,
                    val images: List<String>,
                    val url: String,
                    val rawIn: String) {

    var id: Int = -1

    constructor(sqlResult: ResultSet) : this(
        sqlResult.getString("head"),
        sqlResult.getString("teaser"),
        sqlResult.getString("content"),
        parseImages(sqlResult.getString("media")),
        sqlResult.getString("url"),
        sqlResult.getString("raw_in")){
        id = sqlResult.getInt("id")
    }

    private var sources = mutableListOf<ArticleLink>()

    fun insertInto(connection: Connection): Int {
        val preparedStatement = connection.prepareStatement(
            "INSERT INTO originals (url, head, content, media, raw_in, teaser) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING id;"
        )

        val mediaJson = JsonArray()
        images.forEach { mediaJson.add(it) }

        preparedStatement.setString(1, url)
        preparedStatement.setString(2, head)
        preparedStatement.setString(3, content)
        preparedStatement.setString(4, mediaJson.toString())
        preparedStatement.setString(5, rawIn)
        preparedStatement.setString(6, teaser)

        val resultSet = preparedStatement.executeQuery()
        return if (resultSet.next()) {
            resultSet.getInt("id")
        } else {
            throw Exception("Failed to insert original or retrieve ID.")
        }
    }

    fun getSources(): List<ArticleLink> {
        return sources
    }

    fun getSources(connection: Connection): List<ArticleLink> {
        if (sources.isNotEmpty()){
            return sources
        }

        val preparedStatement = connection.prepareStatement("SELECT head, url, source FROM articles WHERE original_id = ?;")
        preparedStatement.setInt(1, id)

        val queryResult = preparedStatement.executeQuery()
        while(queryResult.next()) {
            sources.add(
                ArticleLink(
                queryResult.getString("head"),
                queryResult.getString("url"),
                queryResult.getString("source"))
            )
        }

        return sources
    }

    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("url", url)
        result.addProperty("head", head)
        result.addProperty("teaser", teaser)
        result.addProperty("content", content)

        val media = JsonArray()
        images.forEach { media.add(it) }
        result.add("media", media)

        return result
    }

    companion object {
        private val urlToOriginal = mutableMapOf<String, Original>()
        // TODO remove from cache some time

        private fun parseImages(text: String): List<String>{
            if (text.isEmpty()) return emptyList()
            return parseString(text).asJsonArray.map { it.asString }
        }

        fun selectByUrl(url: String, connection: Connection): Original{
            val preparedStatement = connection.prepareStatement("SELECT * FROM original where url = ?")
            preparedStatement.setString(1, url)

            val queryResult = preparedStatement.executeQuery()
            if(queryResult.next()) {
                val result = Original(
                    head = queryResult.getString("head"),
                    content = queryResult.getString("content"),
                    images = parseImages(queryResult.getString("media")),
                    url = queryResult.getString("url"),
                    rawIn = queryResult.getString("raw_in"),
                    teaser = queryResult.getString("teaser"))
                result.getSources(connection)
                return result
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