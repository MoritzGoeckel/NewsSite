package programs

import grouping.Clusterer
import structures.Words
import kotlin.math.roundToInt

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