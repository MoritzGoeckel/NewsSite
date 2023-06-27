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

    // get the connection
    val connection = DriverManager.getConnection(jdbcUrl, "postgres", "manager")
    assert(connection.isValid(0))
    containsCache.fill(connection)

    while (true) {
        val articles = downloader.getNew()
        val acceptedArticles = articles.filter(containsCache::insert).filter(Article::isNotEmpty)
        println("""Distinct articles: ${acceptedArticles.size} (down from ${articles.size})""")
        acceptedArticles.forEach{
            try {
                val isInserted = it.insert(connection)
                // assert(isInserted)
            } catch (e: Exception) {
                println("""Error when inserting: ${e.message}""")
            }
        }
        Thread.sleep(Duration.ofSeconds(30))
    }
}