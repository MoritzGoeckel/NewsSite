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
            processor.makeWords(it.split(',')[1])?.let { validWords -> docs.add(validWords) }
        }
    }
}

class Cluster(words: Words, wordToCluster: MutableMap<String, MutableSet<Cluster>>) {
    val docs = mutableListOf<Words>()
    val words = mutableMapOf<String, Int>()

    init {
        add(words, wordToCluster)
    }

    override fun toString(): String{
        return docs.toString()
    }

    fun add(words: Words, wordToCluster: MutableMap<String, MutableSet<Cluster>>){
        docs.add(words)
        words.words.forEach {
            wordToCluster.getOrPut(it.key, { mutableSetOf() }).add(this)
            this.words[it.key] = this.words.getOrDefault(it.key, 0) + it.value
        }
    }
}

class Clusterer {
    private val clusterCreationThreshold = 0.4

    val clusters = mutableListOf<Cluster>()
    private val wordToCluster = mutableMapOf<String, MutableSet<Cluster>>()

    fun addDoc(words: Words){
        var bestCluster: Cluster? = null
        var bestSimilarity = Double.MIN_VALUE
        words.words.forEach { word ->
            wordToCluster[word.key]?.forEach {
                val s = similarity(words, it)
                if(s > bestSimilarity){
                    bestSimilarity = s
                    bestCluster = it
                }
            }
        }

        if(bestCluster == null || bestSimilarity < clusterCreationThreshold){
            clusters.add(Cluster(words, wordToCluster))
        } else {
            bestCluster!!.add(words, wordToCluster)
        }
    }

    private fun similarity(words: Words, cluster: Cluster): Double {
        if(words.words.isEmpty() || cluster.words.isEmpty()) return 0.0

        val smaller: Map<String, Int>
        val larger: Map<String, Int>
        if(words.words.size > cluster.words.size) {
            larger = words.words
            smaller = cluster.words
        } else {
            larger = cluster.words
            smaller = words.words
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
    val clusterer = Clusterer()
    loader.docs.take(100 * 1000).forEach {
        clusterer.addDoc(it)
    }
    val afterCluster = System.currentTimeMillis()

    println("Print")
    clusterer.clusters.filter { it.docs.size > 2 }
                      .sortedBy { it.docs.size }
                      .forEach { cluster ->
                          cluster.docs.forEach { println(it.content) }
                          println()
                      }
    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterLoad - start}ms")
    println("Clustering ${afterCluster - afterLoad}ms / ${(loader.docs.size.toDouble() / ((afterCluster - afterLoad) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}