import java.io.File
import kotlin.math.roundToInt

data class Doc(val content: String, val words: Map<String, Int>)

class Loader {
    private var stopwords: MutableSet<String> = mutableSetOf()
    var docs: MutableList<Doc> = mutableListOf()

    init {
        File("C:\\Users\\Moritz\\Desktop\\stopwords.txt").forEachLine { stopwords.add(it) }
        File("C:\\Users\\Moritz\\Desktop\\abcnews-date-text.csv").forEachLine {
            docs.add(parseDoc(it.split(',')[1]))
        }
    }

    private fun stem(word: String): String{
        return word.removeSuffix("ed")
            .removeSuffix("s")
            .removeSuffix("ly")
            .removeSuffix("ing")
    }

    private fun parseDoc(content: String): Doc{
        return Doc(
            content,
            content.split(' ')
                .filter{ !stopwords.contains(it) }
                .map { stem(it) }
                .associateWith { 1 }
        )
    }
}

class Cluster(doc: Doc, wordToCluster: MutableMap<String, MutableSet<Cluster>>) {
    val docs = mutableListOf<Doc>()
    val words = mutableMapOf<String, Int>()

    init {
        add(doc, wordToCluster)
    }

    override fun toString(): String{
        return docs.toString()
    }

    fun add(doc: Doc, wordToCluster: MutableMap<String, MutableSet<Cluster>>){
        docs.add(doc)
        doc.words.forEach {
            wordToCluster.getOrPut(it.key, { mutableSetOf() }).add(this)
            words[it.key] = words.getOrDefault(it.key, 0) + it.value
        }
    }
}

class Clusterer {
    private val clusterCreationThreshold = 0.7

    val clusters = mutableListOf<Cluster>()
    private val wordToCluster = mutableMapOf<String, MutableSet<Cluster>>()

    fun addDoc(doc: Doc){
        var bestCluster: Cluster? = null
        var bestSimilarity = Double.MIN_VALUE
        doc.words.forEach { word ->
            wordToCluster[word.key]?.forEach {
                val s = similarity(doc, it)
                if(s > bestSimilarity){
                    bestSimilarity = s
                    bestCluster = it
                }
            }
        }

        if(bestCluster == null || bestSimilarity < clusterCreationThreshold){
            clusters.add(Cluster(doc, wordToCluster))
        } else {
            bestCluster!!.add(doc, wordToCluster)
        }
    }

    private fun similarity(doc: Doc, cluster: Cluster): Double {
        if(doc.words.isEmpty() || cluster.words.isEmpty()) return 0.0

        val smaller: Map<String, Int>
        val larger: Map<String, Int>
        if(doc.words.size > cluster.words.size) {
            larger = doc.words
            smaller = cluster.words
        } else {
            larger = cluster.words
            smaller = doc.words
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