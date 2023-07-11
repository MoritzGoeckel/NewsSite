package structures

import com.google.gson.JsonObject
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp

data class ArticleDetails(val title: String, val description: String, val image: String, val date: Timestamp, val articleUrl: String, val url: String, val content: String, var summary: String, var imageCenter: Point? = null){

    companion object {
        private var preparedStatement: PreparedStatement? = null
    }

    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("title", title)
        result.addProperty("description", description)
        result.addProperty("image", image)
        result.addProperty("date", date.toString())
        result.addProperty("url", url)
        result.addProperty("article_url", articleUrl)
        result.addProperty("content", content)
        result.addProperty("summary", summary)

        if (imageCenter != null) {
            result.add("imageCenter", imageCenter!!.toJson())
        }

        return result
    }

    fun insertInto(article: Article, connection: Connection): Boolean {
        if (ArticleDetails.preparedStatement == null){
            ArticleDetails.preparedStatement = connection.prepareStatement(
                "INSERT INTO article_details (title, description, content, summary, image, image_metadata, published_at, url, article_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;"
            )
            // TODO: published_at column is null
        }

        ArticleDetails.preparedStatement!!.setString(1, title)
        ArticleDetails.preparedStatement!!.setString(2, description)
        ArticleDetails.preparedStatement!!.setString(3, content)
        ArticleDetails.preparedStatement!!.setString(4, summary)
        ArticleDetails.preparedStatement!!.setString(5, image)

        var imageMetadata = ""
        if(imageCenter != null){
            imageMetadata = imageCenter!!.toJson().toString()
        }
        ArticleDetails.preparedStatement!!.setString(6, imageMetadata)
        ArticleDetails.preparedStatement!!.setTimestamp(7, date)
        ArticleDetails.preparedStatement!!.setString(8, url)
        ArticleDetails.preparedStatement!!.setString(9, articleUrl)
        return ArticleDetails.preparedStatement!!.execute()
    }
}
