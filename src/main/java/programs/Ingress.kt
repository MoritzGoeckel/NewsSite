package programs

import grouping.Cluster
import grouping.Clusterer
import ingress.ContainsCache
import parsers.ArticlePageParser
import parsers.FrontPageParser
import server.DebugServer
import structures.Article
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
    val frontPageParser = FrontPageParser()
    val containsCache = ContainsCache()
    val articleParser = ArticlePageParser()

    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))
    containsCache.fill(connection)

    // TODO
    // connection.prepareStatement("DELETE FROM articles;").execute()

    val debugServer = DebugServer(connection, config.debugPort())
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
        debugServer.clusters = sortedClusters(clusterer)
    }

    val numSegments = 4
    for(segment in 0 until numSegments){
        downloadDetails(connection, articleParser, segment, numSegments)
    }

    // Model: "gemma3:1b"
    // Model: "mistral-nemo"
    val summarizer = summarizer.Summarizer(summarizer.Ollama("mistral-nemo"), connection)
    insertOriginals(summarizer, clusterer)

    waitForever()
    // TODO populate media / image size
}

fun insertOriginals(summarizer: summarizer.Summarizer, clusterer: Clusterer<Article>): Worker {
    return Worker {
        while (true) {
            summarizer.makeAndInsertOriginals(sortedClusters(clusterer))
            Thread.sleep(Duration.ofSeconds(10))
        }
    }.start()
}

private fun sortedClusters(clusterer: Clusterer<Article>): List<Cluster<Article>> {
    return clusterer.clusters().filter { it.docs.size > 6 }
        .filter { cluster -> cluster.docs.distinctBy { it.source }.size >= 4 }
        .sortedByDescending { cluster -> cluster.docs.distinctBy { it.source }.size }
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
