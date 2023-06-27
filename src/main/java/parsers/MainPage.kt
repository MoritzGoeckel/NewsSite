package parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.*
import processors.TextProcessor
import structures.Article
import structures.Language
import java.io.File

class MainPage(private val textProcessor: TextProcessor) {
    fun getLinkHeadlines(document: Document, base_url: String): List<Article>{
        val result = arrayListOf<Article>()
        val links = document.getElementsByTag("a")
        for(link in links){
            val url = link.attr("href").normalizeUrl(base_url)

            if(url.length - base_url.length < 25) continue

            val h1s = link.getElementsByTag("h1")
            if(!h1s.isEmpty()) {
                result.add(Article(h1s.first()!!.text(), link.text(), url, base_url, textProcessor))
                continue
            }

            val h2s = link.getElementsByTag("h2")
            if(!h2s.isEmpty()) {
                result.add(Article(h2s.first()!!.text(), link.text(), url, base_url, textProcessor))
                continue
            }

            val h3s = link.getElementsByTag("h3")
            if(!h3s.isEmpty()) {
                result.add(Article(h3s.first()!!.text(), link.text(), url, base_url, textProcessor))
                continue
            }

            var i = 0
            var node = link
            while(node.hasParent() && i < 7){
                node = node.parent()
                ++i
                if(node.tag().isHeadline()){
                    val texts = node.getTexts();
                    // TODO support multiple text fields
                    val text = texts.maxByOrNull { it.length }
                    if(text != null) {
                        result.add(Article(text, text, url, base_url, textProcessor))
                    }
                    break
                }
            }
        }
        return result
    }

    fun getLongLinks(document: Document, baseUrl: String): List<Article>{
        return document.getElementsByTag("a")
            .filter { it -> it.text().length > 50 && it.attr("href").normalizeUrl(baseUrl).length > baseUrl.length + 40 }
            .map { element ->
                val titles = element.attributes().filter { it.key.contains("title") }.map(Attribute::value)
                val title = if(titles.size == 1) titles.first() else element.text()
                Article(title, element.text(), element.attr("href").normalizeUrl(baseUrl), baseUrl, textProcessor)
            }
    }

    fun extract(url: String): List<Article>{
        val doc = Jsoup.connect(url).get()

        // MAYBE article tags

        var result = getLinkHeadlines(doc, url)
            .filter { it.content.length > 20 }
            .distinct()

        if(result.size < 10) {
            result = getLongLinks(doc, url)
        }

        if(result.size < 10) {
            println("Only ${result.size} articles for $url")
        }

        return result
    }
}

fun main() {
    val page = MainPage(TextProcessor(Language.DE));
    val articles = mutableListOf<Article>()
    for (url in File("data\\pages\\de.txt").readLines()) {
        val found = page.extract(url)
        println("Found ${found.size} for $url")
        found.map { println(it.header) }
        println()
        articles.addAll(found)
    }

    println(articles.distinct().size)
}