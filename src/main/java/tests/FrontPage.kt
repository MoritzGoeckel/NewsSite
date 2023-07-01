package tests

import ingress.ContainsCache
import parsers.FrontPageParser
import processors.TextProcessor
import structures.Article
import structures.Language
import java.io.File
import java.time.Duration

import java.sql.DriverManager

class ArticleDownloader (textProcessor: TextProcessor, private val urls: List<String>) {
    private val frontPageParser: FrontPageParser = FrontPageParser(textProcessor)

    fun getNew(): List<Article> {
        val articles = mutableListOf<Article>()
        for (url in urls) {
            val found = frontPageParser.extract(url)
            println("Found ${found.size} for $url")

            articles.addAll(found)
        }
        return articles
    }
}

fun main() {
    val textProcessor = TextProcessor(Language.DE)
//    val urls = listOf("https://www.spiegel.de/", "https://www.tagesschau.de/", "https://www.bild.de/")
    val urls = File("data\\pages\\de.txt").readLines()
    val downloader = ArticleDownloader(textProcessor, urls)
    val containsCache = ContainsCache()

    val jdbcUrl = "jdbc:postgresql://localhost:5432/news_site"

    val connection = DriverManager.getConnection(jdbcUrl, "postgres", "manager")
    assert(connection.isValid(0))
    containsCache.fill(connection)

    val articlesQueue = mutableListOf<Article>()

    while (true) {
        val foundArticles = downloader.getNew()
        val newArticles = foundArticles.filter(Article::isNotEmpty).filter(containsCache::insert)
        println("""New articles: ${newArticles.size} (down from ${foundArticles.size})""")
        newArticles.forEach{
            try {
                val isInserted = it.insert(connection)
                if (isInserted){
                    articlesQueue.add(it)
                }
                // assert(isInserted)
            } catch (e: Exception) {
                println("""Error when inserting: ${e.message}""")
            }
        }
        Thread.sleep(Duration.ofSeconds(30))
    }

    // TODO handle articlesQueue with clusterer / details
}