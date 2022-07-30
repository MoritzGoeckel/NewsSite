package parsers

import com.google.gson.JsonObject
import graphics.getVisualCenter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import structures.Point
import java.net.URL
import kotlin.text.StringBuilder

data class ArticleDetails(val title: String, val description: String, val image: String, val date: String, val url: String, val content: String, val imageCenter: Point? = null){
    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("title", title)
        result.addProperty("description", description)
        result.addProperty("image", image)
        result.addProperty("date", date)
        result.addProperty("url", url)
        result.addProperty("content", content)

        if (imageCenter != null) {
            result.add("imageCenter", imageCenter.toJson())
        }

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
        val imageUrl = getImage(doc)

        return ArticleDetails(
            getTitle(doc),
            getDescription(doc),
            imageUrl,
            getDate(doc),
            getUrl(doc),
            getContent(doc),
            getVisualCenter(URL(imageUrl)) // does this belong here?
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


    private fun collectText(textNode: TextNode,
                            parentElement: Element,
                            previous: Node?,
                            next: Node?,
                            sb: StringBuilder) {
        var text = textNode.text().trim()
        text = text.replace("""\r\n""", " ");
        text = text.replace("""\n""", " ");

        while(text.contains("  ")){
            text.replace("  ", " ")
        }

        if(text.isEmpty() || text.isHTML()) return

        if(text.contains('©') && text.length < 150){
            // Probably a copyright note
            return
        }

        if(text.none { it.isWhitespace() } && !isValidText(previous) && !isValidText(next)) {
            // Only one word and both sides don't have text. Skip
            return
        }

        // Add the text
        if(sb.isNotEmpty() && !sb.last().isWhitespace()){
            sb.append(' ')
        }
        sb.append(text)
    }

    private fun isEmbeddedInText(previous: Node?, next: Node?): Boolean{
       return isValidText(previous) && isValidText(next)
    }

    private fun isValidText(node: Node?): Boolean{
        return node != null
                && node is TextNode
                && node.text().trim().isNotEmpty();
    }

    private fun collectText(element: Element,
                            parentElement: Element?,
                            previous: Node?,
                            next: Node?,
                            sb: StringBuilder) {

        val embeddedInText = isEmbeddedInText(previous, next)
        if (element.normalName() == "a" && !embeddedInText){
            return // skip links that are not embedded in text nodes
        }

        // TODO external constants
        val classBlacklist = setOf("teaser", "advertisment", "credits", "author", "pagination", "sidebar", "date", "release")
        for(className in classBlacklist) {
            if (element.className().contains(className)) return
        }

        val tagBlacklist = setOf("ul", "li", "time")
        if (tagBlacklist.contains(element.normalName())) return

        val localSb = StringBuilder()

        val children = element.childNodes()
        for(idx in children.indices){
            val localPrevious = children.getOrElse(idx - 1) { previous }
            val current = children[idx]
            val localNext = children.getOrElse(idx + 1) { next }

            if (current is TextNode) {
                collectText(current, element, localPrevious, localNext, localSb)
            } else if (current is Element) {
                collectText(current, element, localPrevious, localNext, localSb)
            }
        }

        var result = localSb.toString()
        result = result.trim()
        if(result.isEmpty()) return

        val words = result.count { it.isWhitespace() }
        if(words < 2 && !embeddedInText) return

        if(sb.isNotEmpty() && !sb.last().isWhitespace()) {
            sb.append(' ')
        }
        sb.append(result)

        if(element.isBlock) {
            if (sb.last() != '\n') {
                sb.append('\n')
            }
        } else {
            if (!sb.last().isWhitespace()) {
                sb.append(' ')
            }
        }

        // TODO: This needs to be more structures. We need the raw text with everything.
        // TODO: And we need clean sentences that could go into a summary
    }

    private fun getContent(document: Document): String {
        val articles = document.getElementsByTag("article")
        if(articles.isEmpty()) {
            return ""
            // TODO fallback
        }

        articles.sortByDescending { it.text().length }
        val sb = StringBuilder()
        collectText(articles.first(), null, null, null, sb) // longest article is actual article

        val text = sb.toString()
        if(text.isNotEmpty()) {
            return text
        }

        // TODO fallback
        return ""
    }
}

fun main(){
    val article = ArticlePage()
        .extract("https://www.stern.de/wirtschaft/news/energie-russland-stoppt-gaslieferung-an-finnland---streit-um-zahlung-31884200.html")

    println(article.toJson())
}