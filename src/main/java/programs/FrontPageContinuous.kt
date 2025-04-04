package programs

import Configuration
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
// import server.WebServer
import summarizer.GPT
import summarizer.Summarizer
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import kotlin.concurrent.thread
/*
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
                printError("ArticleDownloader", "Failed downloading: $url ${e.message}")
            }
        }
        return articles
    }
}

private const val num_articles = 2000

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
            originalUrl = result.getString("original_url"),
            source = result.getString("source"),
            processor = textProcessor)
        // TODO: Also add date/created?
        articlesQueue.add(article)
    }

    insertQueueIntoClusterer(articlesQueue, insertedDocs, clusterer)
    val server = WebServer()

    run {
        val clusters = sortedClusters(clusterer)
        assignRepresentatives(clusters, articleParser, summarizer, connection)
        server.clusters = clusters
        server.connection = connection
        server.start()
    }

    // Create originals every 21 seconds
    val gpt = GPT(config.openAIKey())
    thread(start = true) {
        while(true) {
            try {
                val clusters = server.clusters
                if (gpt.isAvailable()) {
                    createOriginal(clusters, articleParser, summarizer, connection, gpt)
                }
            } catch (e: Exception){
                printError("RunServer/GPT", "Failed creating original: $e\n${e.stackTraceToString()}")
            }
            gpt.waitUntilAvailable()
        }
    }

    // Keep downloading in main thread
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
        assignRepresentatives(clusters, articleParser, summarizer, connection)
        server.clusters = clusters
        Thread.sleep(config.frontPageScrapingInterval().toMillis())
    }
}

private fun createOriginal(
    clusters: List<Cluster<Article>>,
    articleParser: ArticlePageParser,
    summarizer: Summarizer,
    connection: Connection,
    gpt: GPT
) {
    val unusedClusters = clusters.filter { it.docs.size >= 3 }       // Clusters with at least 3 doc
        .reversed() // TODO this should be reversed always. Important should always come first
        .take(50) // Only 50 clusters
        .filter { cluster -> cluster.docs.filter { doc -> doc.originalUrl.isEmpty() }.size > (cluster.docs.size * 0.9) }   // Only with > 70% not used docs

    for (cluster in unusedClusters){
        val chunks = mutableListOf<String>()
        val images = mutableListOf<String>()
        cluster.docs
            .filter { it.originalUrl.isEmpty() }
            .forEach {
                chunks.add(it.header)
                chunks.add(it.content)
                assignDetails(it, connection, articleParser, summarizer);
                if(it.details != null) {
                    chunks.add(it.details!!.content)
                    chunks.add(it.details!!.title)
                    if(it.details!!.image.isNotEmpty()) {
                        images.add(it.details!!.image)
                    }
            }
        }

        var text = chunks
            .asSequence()
            .map { it.trim() }
            .map {
                if(!it.endsWith(".")){
                    "$it. "
                } else {
                    it
                }
            }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
            .trim()

        if(text.length > 1000 /* min char threshold */) {
            if (text.length > gpt.maxLength()) {
                var i = gpt.maxLength()
                while (i > 0 && text[i] != '.'){
                    --i
                }
                text = text.substring(0, i + 1)
            }
            val original = gpt.generateOriginal(text, images)
            original.insertInto(connection)
            original.getSources(connection) // populate cache
            cluster.docs.forEach { it.setOriginalUrl(original.url, connection) }
            break
        }
    }
}

private fun assignRepresentatives(clusters: List<Cluster<Article>>, articleParser: ArticlePageParser, summarizer: Summarizer, connection: Connection) {
    clusters.filter { it.docs.size >= 3 }       // Clusters with at least 3 doc
        .takeLast(50 /* max */)              // Only 50 clusters
        .filter { it.representative == null }   // Only clusters without representative
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

                val success = assignDetails(article, connection, articleParser, summarizer)
                if(success) {
                    it.representative = article
                    // TODO: Find good image for cluster
                    break
                }
            }
        }
}

private fun assignDetails(
    article: Article,
    connection: Connection,
    articleParser: ArticlePageParser,
    summarizer: Summarizer
): Boolean {
    try {
        if (article.loadDetails(connection)) {
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
                return false
            }

            if (details.content.isEmpty()) {
                // Can't find content
                printWarning("RunServer", "No content for ${article.url}")
                return false
            }

            details.imageCenter = getVisualCenter(image)
            details.summary = summarizer.summarize(details.content, details.content)

            details.insertInto(article, connection)
            printInfo("RunServer", "Created details for ${article.url}")
        }
    } catch (e: Exception) {
        printError("RunServer", "Can't download details for ${article.url} because of $e")
        return false
    }
    return true
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
}*/