package crawler

import org.jsoup.Jsoup
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.parser.Tag
import structures.Article
import java.io.File

class MainPage {
    fun String.normalizeUrl(base_url: String): String{
        if(this.startsWith(base_url)) return this;
        val separator = if(this.startsWith("/") || base_url.endsWith("/")) "" else "/"
        return base_url + separator + this
    }

    fun Tag.isHeadline(): Boolean {
        val name = this.normalName()
        if(name == "h1" || name == "h2" || name == "h3") return true
        return false
    }

    fun getLinkHeadlines(document: Document, base_url: String): List<Article>{
        val result = arrayListOf<Article>()
        val links = document.getElementsByTag("a")
        for(link in links){
            val url = link.attr("href").normalizeUrl(base_url)

            if(url.length - base_url.length < 25) continue

            val h1s = link.getElementsByTag("h1")
            if(!h1s.isEmpty()) {
                result.add(Article(h1s.first().text(), link.text(), url))
                continue
            }

            val h2s = link.getElementsByTag("h2")
            if(!h2s.isEmpty()) {
                result.add(Article(h2s.first().text(), link.text(), url))
                continue
            }

            val h3s = link.getElementsByTag("h3")
            if(!h3s.isEmpty()) {
                result.add(Article(h3s.first().text(), link.text(), url))
                continue
            }

            var i = 0
            var node = link
            while(node.hasParent() && i < 7){
                node = node.parent()
                ++i
                if(node.tag().isHeadline()){
                    result.add(Article(node.text(), node.text(), url))
                    break
                }
            }
        }
        return result
    }

    fun getLongLinks(document: Document, base_url: String): List<Article>{
        return document.getElementsByTag("a")
            .filter { it.text().length > 50 && it.attr("href").normalizeUrl(base_url).length > base_url.length + 40 }
            .map {
                val titles = it.attributes().filter { it.key.contains("title") }.map(Attribute::value)
                val title = if(titles.size == 1) titles.first() else it.text()
                Article(title, it.text(), it.attr("href").normalizeUrl(base_url))
            }
    }

    fun extract(url: String): List<Article>{
        val doc = Jsoup.connect(url).get()

        // MAYBE article tags

        var result = getLinkHeadlines(doc, url)
            .filter { it.text.length > 20 }
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
    val page = MainPage();
    val articles = mutableListOf<Article>()
    for (url in File("data\\pages\\de.txt").readLines()) {
        val found = page.extract(url)
        println("Found ${found.size} for $url")
        articles.addAll(found)
    }

    println(articles.distinct().size)
}