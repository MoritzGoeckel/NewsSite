import processors.TextProcessor
import structures.Language
import structures.Words
import java.io.File
import kotlin.math.roundToInt

class Loader {
    var docs: MutableList<Words> = mutableListOf()

    init {
        val processor = TextProcessor(Language.EN)
        File("data\\samples\\abcnews-date-text.csv").forEachLine {
            val words = processor.makeWords(it.split(',')[1])
            if(words.isNotEmpty()) {
                docs.add(words)
            }
        }
    }
}

class Cluster<DocType : Words>(newDocs: DocType, wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>) {
    val docs = mutableListOf<DocType>()
    val words = mutableMapOf<String, Int>()

    init {
        add(newDocs, wordToCluster)
    }

    override fun toString(): String{
        return docs.toString()
    }

    fun add(newDocs: DocType, wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>){
        docs.add(newDocs)
        newDocs.words.forEach {
            wordToCluster.getOrPut(it.key, { mutableSetOf() }).add(this)
            this.words[it.key] = this.words.getOrDefault(it.key, 0) + it.value
        }
    }
}

class Clusterer<DocType : Words> {
    private val clusterCreationThreshold = 0.4

    val clusters = mutableListOf<Cluster<DocType>>()
    private val wordToCluster = mutableMapOf<String, MutableSet<Cluster<DocType>>>()

    fun addDoc(docs: DocType){
        var bestCluster: Cluster<DocType>? = null
        var bestSimilarity = Double.MIN_VALUE
        docs.words.forEach { word ->
            wordToCluster[word.key]?.forEach {
                val s = similarity(docs, it)
                if(s > bestSimilarity){
                    bestSimilarity = s
                    bestCluster = it
                }
            }
        }

        if(bestCluster == null || bestSimilarity < clusterCreationThreshold){
            clusters.add(Cluster(docs, wordToCluster))
        } else {
            bestCluster!!.add(docs, wordToCluster)
        }
    }

    private fun similarity(docs: DocType, cluster: Cluster<DocType>): Double {
        if(docs.words.isEmpty() || cluster.words.isEmpty()) return 0.0

        val smaller: Map<String, Int>
        val larger: Map<String, Int>
        if(docs.words.size > cluster.words.size) {
            larger = docs.words
            smaller = cluster.words
        } else {
            larger = cluster.words
            smaller = docs.words
        }

        var same = 0
        var all = 0
        smaller.forEach{
            if(larger.containsKey(it.key)) same += larger[it.key]!! + it.value
            all += it.value
        }

        larger.forEach{
            all += it.value
        }

        return same.toDouble() / all.toDouble()
    }
}

fun main() {
    val start = System.currentTimeMillis()

    println("Load")
    val loader = Loader()
    val afterLoad = System.currentTimeMillis()

    println("Cluster")
    val clusterer = Clusterer<Words>()
    loader.docs.take(100 * 1000).forEach {
        clusterer.addDoc(it)
    }
    val afterCluster = System.currentTimeMillis()

    println("Print")
    clusterer.clusters.filter { it.docs.size > 2 }
                      .sortedBy { it.docs.size }
                      .forEach { cluster ->
                          cluster.docs.forEach { println(it.text) }
                          println()
                      }
    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterLoad - start}ms")
    println("Clustering ${afterCluster - afterLoad}ms / ${(loader.docs.size.toDouble() / ((afterCluster - afterLoad) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}