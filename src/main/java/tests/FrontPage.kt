package tests

import Configuration
import ingress.ContainsCache
import parsers.FrontPageParser
import processors.TextProcessor
import structures.Article
import structures.Language
import java.io.File
import printError
import printInfo
import printTrace
import java.sql.DriverManager

class ArticleDownloader (textProcessor: TextProcessor, private val urls: List<String>) {
    private val frontPageParser: FrontPageParser = FrontPageParser(textProcessor)

    fun getNew(): List<Article> {
        val articles = mutableListOf<Article>()
        for (url in urls) {
            try {
                val found = frontPageParser.extract(url)
                printTrace("ArticleDownloader", "Found ${found.size} for $url")

                articles.addAll(found)
            } catch (e: Exception) {
                printError("ArticleDownloader", """Failed downloading: $url ${e.message}""")
            }
        }
        return articles
    }
}

fun main() {
    val config = Configuration()
    val textProcessor = TextProcessor(Language.DE)
    val urls = File("data/pages/de.txt").readLines()
    val downloader = ArticleDownloader(textProcessor, urls)
    val containsCache = ContainsCache()

    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))
    containsCache.fill(connection)

    val articlesQueue = mutableListOf<Article>()

    while (true) {
        val foundArticles = downloader.getNew()
        val newArticles = foundArticles.filter(Article::isNotEmpty).filter(containsCache::insert)
        printInfo("main", """New articles: ${newArticles.size} (down from ${foundArticles.size})""")
        newArticles.forEach{
            try {
                val isInserted = it.insert(connection)
                if (isInserted){
                    articlesQueue.add(it)
                }
                // assert(isInserted)
            } catch (e: Exception) {
                printError("main", """"Failed inserting: $it ${e.message}""")
            }
        }
        Thread.sleep(config.frontPageScrapingInterval().toMillis())
    }

    // TODO handle articlesQueue with clusterer / details
}