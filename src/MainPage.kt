import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag

data class Article (val header: String, val text: String, val url: String)

class MainPage {
    fun Tag.isHeadline(): Boolean {
        val name = this.normalName()
        if(name == "h1" || name == "h2" || name == "h3") return true
        return false
    }

    fun getLinkHeadlines(document: Document, base_url: String): List<Article>{
        val result = arrayListOf<Article>()
        val links = document.getElementsByTag("a")
        for(link in links){
            val url = link.attr("href")

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

    fun extract(url: String): List<Article>{
        val doc = Jsoup.connect(url).get()
        // article tags
        // a with long title
        // a with long content
        return getLinkHeadlines(doc, url)
            .filter { it.text.length > 20 }
            .distinct()
    }
}

fun main() {
    val page = MainPage();
    
    val urls = listOf<String>(
        "https://www.spiegel.de/",
        "https://www.tagesschau.de/",
        "https://www.bild.de/",
        "https://www.br.de/nachrichten/",
        "https://www.morgenpost.de/vermischtes/",
        "https://www.t-online.de/nachrichten/",
        "https://www.ka-news.de/nachrichten/",
        "https://www.stern.de/",
        "https://www.augsburger-allgemeine.de/",
        "https://www.srf.ch/",
        "https://www.tah.de/",
        "https://www.br.de/",
        "https://www.finanznachrichten.de/",
        "https://www.ariva.de/news/")

    val articles = mutableListOf<Article>()

    urls.map { url ->
        println(url)
        page.extract(url).map {
            articles.add(it)
            println(it)
        }
        println("#####################################")
    }

    println(articles.distinct().size)
}