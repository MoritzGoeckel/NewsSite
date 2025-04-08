package programs

import grouping.Cluster
import grouping.Clusterer
import ingress.ContainsCache
import parsers.ArticlePageParser
import parsers.FrontPageParser
import processors.TextProcessor
import server.WebServer
import structures.Article
import structures.Language
import util.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private const val num_articles = 2000

private val rand = Random()

fun main() {
    val config = Configuration()
    val textProcessor = TextProcessor(Language.DE)
    val frontPageParser = FrontPageParser()
    val containsCache = ContainsCache()
    val articleParser = ArticlePageParser()

    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))
    containsCache.fill(connection)

    // TODO
    // connection.prepareStatement("DELETE FROM articles;").execute()

    val server = WebServer(connection)
        .start()

    val articleQueue: Queue<Article> = ConcurrentLinkedQueue()

    val urlsQueue: Queue<Pair<Instant, String>> = ConcurrentLinkedQueue()
    File("data/pages/de.txt").readLines().forEach {
        urlsQueue.add(Pair(Instant.MIN, it))
    }

    for (i in 0 until 2) {
        downloadLinks(urlsQueue, frontPageParser, articleQueue)
    }

    writeToDb(connection, articleQueue)

    val clusterer = Clusterer<Article>()

    updateCluster(connection, clusterer) {
        // Update server's clusters
        server.clusters = sortedClusters(clusterer)
    }

    val numSegments = 4
    for(segment in 0 until numSegments){
        downloadDetails(connection, articleParser, segment, numSegments)
    }

    waitForever()

    // use to create summary / originals

    // populate media / image size


    // val summarizer = summarizer.Summarizer(Language.DE, 300)

    // val select = connection.prepareStatement("SELECT * FROM articles ORDER BY created_at ASC") // TODO limit
    // val result = select.executeQuery()

    // val clusterer = Clusterer<Article>()
    // val insertedDocs = mutableListOf<Article>()

    /* val articlesQueue = mutableListOf<Article>()
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
        val foundArticles = getNew(urls, frontPageParser)
        val newArticles = foundArticles.filter(Article::isNotEmpty).filter(containsCache::insert)
        printInfo("main", """New articles: ${newArticles.size} (down from ${foundArticles.size})""")
        newArticles.forEach{
            try {
                val isInserted = it.insertInto(connection)
                if (isInserted){
                    articlesQueue.add(it)
                }
            } catch (e: Exception) {
                printError("main", """"Failed inserting: $it ${e.message}""")
            }
        }

        insertQueueIntoClusterer(articlesQueue, insertedDocs, clusterer)

        val clusters = sortedClusters(clusterer)
        assignRepresentatives(clusters, articleParser, summarizer, connection)
        server.clusters = clusters
        Thread.sleep(config.frontPageScrapingInterval().toMillis())
    }*/
}

private fun sortedClusters(clusterer: Clusterer<Article>): List<Cluster<Article>> {
    return clusterer.clusters().filter { it.docs.size > 6 }
        .filter { cluster -> cluster.docs.distinctBy { it.source }.size >= 4 }
        .sortedBy { cluster -> cluster.docs.distinctBy { it.source }.size }
        .reversed() // We want the top articles first
}

fun waitForever() {
    while (true){
        Thread.sleep(Duration.ofSeconds(3))
    }
}

fun updateCluster(connection: Connection, clusterer: Clusterer<Article>, onClusterChanged: () -> Unit): Worker {
    val selectStmt = connection.prepareStatement("SELECT * FROM articles WHERE created_at > ? AND created_at <= ? ORDER BY created_at ASC")
    return Worker {
        var lastSeen = Instant.now().minus(Duration.ofHours(24))
        while (true) {
            selectStmt.setTimestamp(1, Timestamp.from(lastSeen))
            selectStmt.setTimestamp(2, Timestamp.from(Instant.now()))

            val result = selectStmt.executeQuery()
            val articles = mutableListOf<Article>()
            while (result.next()){
                articles.add(Article(result))
            }

            val last = articles.lastOrNull()
            if(last != null){
                lastSeen = last.created_at
            }

            printError("Clusterer", "new articles: ${articles.size}")

            // Remove
            val cutoff = Instant.now().minus(Duration.ofHours(24))
            val removed = clusterer.removeIf { it.created_at < cutoff }
            printError("Clusterer", "Removed: ${removed}")

            // Add
            articles.forEach(clusterer::add)
            if(removed > 0 || articles.isNotEmpty()) {
                onClusterChanged()
            }

            Thread.sleep(Duration.ofSeconds(30))
        }
    }.start()
}

fun downloadLinks(urls: Queue<Pair<Instant, String>>, frontPageParser: FrontPageParser, queue: Queue<Article>): Worker {
    return Worker {
        while (true) {
            val pair = urls.poll()
            if(pair == null){
                printWarning("ArticleDownloader", "No more urls, waiting")
                Thread.sleep(Duration.ofSeconds(5))
                continue
            }

            val timestamp = pair.first
            val url = pair.second
            val now = Instant.now()

            if(now < timestamp){
                Thread.sleep(Duration.between(now, timestamp))
            }

            try {
                val found = frontPageParser.extract(url)
                found.forEach { queue.add(it) }
                printInfo("ArticleDownloader", "Found ${found.size}")
            } catch (e: Exception) {
                printError("ArticleDownloader", "Failed downloading: $url ${e.message}")
                e.printStackTrace()
            }
            urls.add(Pair(Instant.now().plusSeconds(rand.nextLong(60, 90)), url))
        }
    }.start()
}

fun writeToDb(connection: Connection, inputQueue: Queue<Article>): Worker {
    return Worker {
        val article = inputQueue.poll()
        if(article != null) {
            article.insertInto(connection)
        } else {
            Thread.sleep(Duration.ofSeconds(5))
        }
    }.start();
}

fun downloadDetails(connection: Connection, articleParser: ArticlePageParser, segment: Int, numSegments: Int): Worker{
    var lastSeen = Instant.now().minus(Duration.ofHours(24))
    return Worker {
        val selectStmt = connection.prepareStatement("SELECT * FROM articles " +
                "WHERE created_at > ? AND created_at <= ? " +
                "AND content = '' AND head = '' " +
                "AND (id % $numSegments) = $segment ORDER BY created_at ASC")

        while (true) {
            selectStmt.setTimestamp(1, Timestamp.from(lastSeen))
            selectStmt.setTimestamp(2, Timestamp.from(Instant.now()))

            val result = selectStmt.executeQuery()
            val articles = mutableListOf<Article>()
            while (result.next()) {
                articles.add(Article(result))
            }

            if(articles.isEmpty()){
                Thread.sleep(Duration.ofSeconds(5))
                continue
            }

            val last = articles.lastOrNull()
            if (last != null) {
                lastSeen = last.created_at
            }
            articles.shuffle()
            articles.forEach {
                try {
                    val extendedArticle = articleParser.fill(it)
                    extendedArticle.updateInto(connection)
                    printInfo("FillDetails", "Filled successfully")
                } catch (e: Exception){
                    printError("FillDetails", e.toString())
                    e.printStackTrace()
                }
            }
        }
    }.start();
}

/*private fun createOriginal(
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
}*/

/*private fun assignRepresentatives(clusters: List<Cluster<Article>>, articleParser: ArticlePageParser, summarizer: Summarizer, connection: Connection) {
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
}*/

/*private fun assignDetails(
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
}*/

/*

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
 */