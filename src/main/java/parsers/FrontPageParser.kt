package parsers

import org.jsoup.Jsoup
import org.jsoup.nodes.*
import structures.Article
import util.printWarning
import java.time.Instant

class FrontPageParser() {
    fun getLinkHeadlines(document: Document, base_url: String): List<Article>{
        val result = arrayListOf<Article>()
        val links = document.getElementsByTag("a")
        for(link in links){
            val url = link.attr("href").normalizeUrl(base_url)

            if(url.length - base_url.length < 25) continue

            val h1s = link.getElementsByTag("h1")
            if(!h1s.isEmpty()) {
                result.add(Article(h1s.first()!!.text(), link.text(), url, base_url, Instant.now()))
                continue
            }

            val h2s = link.getElementsByTag("h2")
            if(!h2s.isEmpty()) {
                result.add(Article(h2s.first()!!.text(), link.text(), url, base_url, Instant.now()))
                continue
            }

            val h3s = link.getElementsByTag("h3")
            if(!h3s.isEmpty()) {
                result.add(Article(h3s.first()!!.text(), link.text(), url, base_url, Instant.now()))
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
                        result.add(Article(text, text, url, base_url, Instant.now()))
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
                Article(title, element.text(), element.attr("href").normalizeUrl(baseUrl), baseUrl, Instant.now())
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
            printWarning("FrontPageParser", "Only ${result.size} articles for $url")
        }

        return result
    }
}