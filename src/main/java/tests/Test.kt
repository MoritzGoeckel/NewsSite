import parsers.MainPage
import processors.TextProcessor
import server.WebServer
import structures.Article
import structures.Language
import java.io.File
import kotlin.math.roundToInt

fun main() {
    val processor = TextProcessor(Language.DE)

    val start = System.currentTimeMillis()
    val page = MainPage(processor);
    val articles = mutableListOf<Article>()

    for (url in File("data\\pages\\de.txt").readLines()) {
        val found = page.extract(url)
        println("Found ${found.size} for $url")
        articles.addAll(found)
    }

    val afterDownload = System.currentTimeMillis()

    println("Cluster")
    val clusterer = Clusterer<Article>()

    val acceptedArticles = articles.filter { it.isNotEmpty() }.distinctBy { it.normalized() }
    println("""Distinct articles: ${acceptedArticles.size} (down from ${articles.size})""")

    // Add to clusterer
    acceptedArticles.forEach { clusterer.addDoc(it) }

    val afterCluster = System.currentTimeMillis()

    // Sort clusters
    val clusters = clusterer.clusters.filter { it.docs.size > 2 }
        .filter { it.docs.distinctBy { it.source }.size >= 2 }
        .sortedBy { it.docs.distinctBy { it.source }.size }

    // Print clusters
    clusters.forEach { cluster ->
        cluster.docs.forEach { println(it.text + " -> " + it.words + " " + it.source) }
        println()
    }

    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterDownload - start}ms")
    println("Clustering ${afterCluster - afterDownload}ms / ${(articles.size.toDouble() / ((afterCluster - afterDownload) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")

    // Start server
    val server = WebServer()
    server.clusters = clusters
    server.start()
    println("After start")
}