package programs

import Configuration
import com.google.gson.JsonParser
import graphics.downloadImage
import graphics.getVisualCenter
import grouping.Cluster
import grouping.Clusterer
import ingress.ContainsCache
import parsers.ArticlePageParser
import parsers.FrontPageParser
import processors.TextProcessor
import structures.Article
import structures.Language
import java.io.File
import printError
import printInfo
import printTrace
import printWarning
import server.WebServer
import structures.ArticleDetails
import structures.Point
import summarizer.Summarizer
import java.net.URL
import java.sql.Connection
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

private const val num_articles = 1000

fun main() {
    val config = Configuration()
    val textProcessor = TextProcessor(Language.DE)
    val urls = File("data/pages/de.txt").readLines()
    val downloader = ArticleDownloader(textProcessor, urls)
    val containsCache = ContainsCache()

    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))
    containsCache.fill(connection)

    val select = connection.prepareStatement("SELECT * FROM articles ORDER BY created_at ASC") // TODO limit
    val result = select.executeQuery()

    val clusterer = Clusterer<Article>()
    val insertedDocs = mutableListOf<Article>()

    val articleParser = ArticlePageParser()
    val summarizer = summarizer.Summarizer(Language.DE, 300)

    val articlesQueue = mutableListOf<Article>()
    while (result.next()){
        val article = Article(
            header = result.getString("head"),
            content = result.getString("content"),
            url = result.getString("url"),
            source = result.getString("source"),
            processor = textProcessor)
        // TODO: Also add date/created?
        // TODO add details from database
        articlesQueue.add(article)
    }

    insertQueueIntoClusterer(articlesQueue, insertedDocs, clusterer)
    val server = WebServer()
    run {
        val clusters = sortedClusters(clusterer)
        addDetails(clusters, articleParser, summarizer, connection)
        server.clusters = clusters
        server.start()
    }

    while (true) {
        val foundArticles = downloader.getNew()
        val newArticles = foundArticles.filter(Article::isNotEmpty).filter(containsCache::insert)
        printInfo("main", """New articles: ${newArticles.size} (down from ${foundArticles.size})""")
        newArticles.forEach{
            try {
                val isInserted = it.insertInto(connection)
                if (isInserted){
                    articlesQueue.add(it)
                }
                // assert(isInserted)
            } catch (e: Exception) {
                printError("main", """"Failed inserting: $it ${e.message}""")
            }
        }

        insertQueueIntoClusterer(articlesQueue, insertedDocs, clusterer)

        val clusters = sortedClusters(clusterer)
        addDetails(clusters, articleParser, summarizer, connection)
        server.clusters = clusters
        Thread.sleep(config.frontPageScrapingInterval().toMillis())
    }
}

private fun addDetails(clusters: List<Cluster<Article>>, articleParser: ArticlePageParser, summarizer: Summarizer, connection: Connection) {
    val selectArticleDetails = connection.prepareStatement("SELECT * FROM article_details WHERE article_url = ?")

    clusters.filter { it.docs.size >= 3 }     // Clusters with at least 3 doc
        .takeLast(50 /* max */)            // Only 50 clusters
        .filter { it.representative == null } // Only clusters without representative
        .forEach {
            it.sortByRepresentative()
            for (article in it.docs) {
                if(article.details != null){
                    // already got details
                    it.representative = article
                    // TODO maybe details.content.isEmpty()
                    // TODO maybe pixels < 400 * 400 || widthRatio < 1.3
                    break
                }

                try {
                    selectArticleDetails.setString(1, article.url)
                    val result = selectArticleDetails.executeQuery()

                    if (result.next()){
                        val imageMetadata = result.getString("image_metadata")
                        val imageMetadataJson = JsonParser.parseString(imageMetadata).asJsonObject
                        article.details = ArticleDetails(
                            title = result.getString("title"),
                            description = result.getString("description"),
                            image = result.getString("image"),
                            date = result.getTimestamp("published_at"),
                            articleUrl = result.getString("article_url"),
                            url = result.getString("url"),
                            content = result.getString("content"),
                            summary = result.getString("summary"),
                            imageCenter = Point(imageMetadataJson)
                        )
                        printInfo("RunServer", "Loaded representative and details for ${article.url}")

                    } else {
                        article.details = articleParser.extract(article.url)
                        val details = article.details!!
                        val image = downloadImage(URL(details.image))

                        val widthRatio = image.width / image.height.toDouble()
                        val pixels = image.height * image.width

                        if (pixels < 400 * 400 || widthRatio < 1.3) {
                            // image is too small
                            printWarning(
                                "RunServer",
                                "Image too small for ${article.url}: ${image.height}x${image.width} pixels=$pixels widthRatio=${widthRatio}"
                            )
                            continue
                        }

                        if (details.content.isEmpty()) {
                            // Can't find content
                            printWarning("RunServer", "No content for ${article.url}")
                            continue
                        }

                        details.imageCenter = getVisualCenter(image)
                        details.summary = summarizer.summarize(details.content, details.content)

                        details.insertInto(article, connection)
                        printInfo("RunServer", "Created representative and details for ${article.url}")

                    }

                    it.representative = article
                    // TODO: Find good image for cluster
                    break
                } catch (e: Exception) {
                    printError("RunServer", "Can't download details for ${article.url} because of $e")
                }
            }
        }
}

private fun sortedClusters(clusterer: Clusterer<Article>): List<Cluster<Article>> {
    return clusterer.clusters().filter { it.docs.size > 2 }
        .filter { cluster -> cluster.docs.distinctBy { it.source }.size >= 2 }
        .sortedBy { cluster -> cluster.docs.distinctBy { it.source }.size }
}

private fun insertQueueIntoClusterer(articlesQueue: MutableList<Article>, insertedDocs: MutableList<Article>, clusterer: Clusterer<Article>) {
    articlesQueue.forEach {
        insertedDocs.add(it)
        clusterer.add(it)
        while (insertedDocs.size > num_articles) {
            clusterer.remove(insertedDocs.first())
            insertedDocs.removeFirst()
        }
    }
    articlesQueue.clear()
}