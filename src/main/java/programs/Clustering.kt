package programs

import grouping.Clusterer
import processors.TextProcessor
import structures.Language
import structures.Words
import java.io.File
import kotlin.math.roundToInt

class Loader {
    var docs: MutableList<Words> = mutableListOf()

    init {
        val processor = TextProcessor(Language.EN)
        File("data/samples/abcnews-date-text.csv").forEachLine {
            val words = processor.makeWords(it.split(',')[1])
            if(words.isNotEmpty()) {
                docs.add(words)
            }
        }
    }
}

fun main() {
    val start = System.currentTimeMillis()

    println("Load")
    val loader = Loader()
    val afterLoad = System.currentTimeMillis()

    println("grouping.Cluster")
    val clusterer = Clusterer<Words>()
    loader.docs.take(100 * 1000).forEach {
        clusterer.add(it)
    }
    val afterCluster = System.currentTimeMillis()

    println("Print")
    clusterer.clusters().filter { it.docs.size > 2 }
                      .sortedBy { it.docs.size }
                      .forEach { cluster ->
                          cluster.docs.forEach { println(it.text) }
                          println()
                      }
    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters().size} clusters")
    println("Loading    ${afterLoad - start}ms")
    println("Clustering ${afterCluster - afterLoad}ms / ${(loader.docs.size.toDouble() / ((afterCluster - afterLoad) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}