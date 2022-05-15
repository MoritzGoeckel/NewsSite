import parsers.MainPage
import processors.TextProcessor
import structures.Article
import structures.Language
import structures.Words
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

    println(articles.distinct().size)

    val afterDownload = System.currentTimeMillis()

    println("Cluster")
    val clusterer = Clusterer<Article>()

    articles.filter { it.isNotEmpty() }
        .forEach { clusterer.addDoc(it) }

    val afterCluster = System.currentTimeMillis()

    println("Print")
    clusterer.clusters.filter { it.docs.size > 2 }
        .filter { it.docs.distinctBy { it.source }.size > 2 }
        .sortedBy { it.docs.distinctBy { it.source }.size }
        //.sortedBy { it.docs.size }
        .forEach { cluster ->
            cluster.docs.forEach { println(it.text + " -> " + it.words + " " + it.source) }
            println()
        }

    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterDownload - start}ms")
    println("Clustering ${afterCluster - afterDownload}ms / ${(articles.size.toDouble() / ((afterCluster - afterDownload) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}