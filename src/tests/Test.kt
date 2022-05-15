import parsers.MainPage
import processors.TextProcessor
import structures.Article
import structures.Language
import java.io.File
import kotlin.math.roundToInt

fun main() {
    val start = System.currentTimeMillis()
    val page = MainPage();
    val articles = mutableListOf<Article>()

    for (url in File("data\\pages\\de.txt").readLines()) {
        val found = page.extract(url)
        println("Found ${found.size} for $url")
        articles.addAll(found)
    }

    println(articles.distinct().size)

    val afterDownload = System.currentTimeMillis()

    println("Cluster")
    val clusterer = Clusterer()
    val processor = TextProcessor(Language.DE)

    articles.forEach {
        processor.makeWords(it.header)?.let { validWords -> clusterer.addDoc(validWords) }
    }

    val afterCluster = System.currentTimeMillis()

    println("Print")
    clusterer.clusters.filter { it.docs.size > 2 }
            // TODO: only show clusters with more than one source url
        .sortedBy { it.docs.size }
        .forEach { cluster ->
            cluster.docs.forEach { println(it.content + " -> " + it.words) }
            println()
        }

    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterDownload - start}ms")
    println("Clustering ${afterCluster - afterDownload}ms / ${(articles.size.toDouble() / ((afterCluster - afterDownload) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}