package structures

import com.google.gson.JsonObject

data class ArticleDetails(val title: String, val description: String, val image: String, val date: String, val url: String, val content: String, var summary: String, var imageCenter: Point? = null){
    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("title", title)
        result.addProperty("description", description)
        result.addProperty("image", image)
        result.addProperty("date", date)
        result.addProperty("url", url)
        result.addProperty("content", content)
        result.addProperty("summary", summary)

        if (imageCenter != null) {
            result.add("imageCenter", imageCenter!!.toJson())
        }

        return result
    }
}
