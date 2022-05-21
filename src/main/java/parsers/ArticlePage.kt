package parsers

import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

data class ArticleDetails(val title: String, val description: String, val image: String, val date: String, val url: String){
    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("title", title)
        result.addProperty("description", description)
        result.addProperty("image", image)
        result.addProperty("date", date)
        result.addProperty("url", url)
        return result
    }
}

class ArticlePage {
    private val descriptionMetaNames = setOf("description", "og:description", "twitter:description", "sis-article-teaser")
    private val imageMetaNames = setOf("image", "og:image", "ob_image")
    private val dateMetaNames = setOf("date", "buildDate", "sis-article-published-date", "last-modified")
    private val urlMetaNames = setOf("url", "og:url")
    private val titleMetaNames = setOf("title", "og:title", "ob_headline", "sis-article-headline")

    fun extract(url: String): ArticleDetails{
        val doc = Jsoup.connect(url).get()

        return ArticleDetails(
            getTitle(doc),
            getDescription(doc),
            getImage(doc),
            getDate(doc),
            getUrl(doc)
        )
    }

    private fun getMetaContent(document: Document, candidates: Set<String>): String{
        return document.getElementsByTag("meta")
            .map {
                val content = it.attr("content")
                if(it.hasAttr("name"))
                   Pair<String, String>(it.attr("name"), content)
                else
                    Pair<String, String>(it.attr("property"), content)
            }
            .filter{ candidates.contains(it.first) }
            .map { it.second }
            .maxByOrNull { it.length }
            .orEmpty()
    }

    private fun getDescription(document: Document): String {
       return getMetaContent(document, descriptionMetaNames)
    }

    private fun getTitle(document: Document): String {
        return getMetaContent(document, titleMetaNames)
    }

    private fun getDate(document: Document): String {
        return getMetaContent(document, dateMetaNames)
    }

    private fun getImage(document: Document): String {
        return getMetaContent(document, imageMetaNames)
    }

    private fun getUrl(document: Document): String {
        return getMetaContent(document, urlMetaNames)
    }
}

fun main(){
    val article = ArticlePage()
        .extract("https://www.stern.de/wirtschaft/news/energie-russland-stoppt-gaslieferung-an-finnland---streit-um-zahlung-31884200.html")

    println(article.toJson())
}