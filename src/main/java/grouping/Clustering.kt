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
    val words = Words()

    private var mostRepresentative: DocType? = null

    init {
        add(newDocs, wordToCluster)
    }

    override fun toString(): String{
        return docs.toString()
    }

    fun size(): Int{
        return docs.size
    }

    fun add(newDocs: DocType, wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>){
        docs.add(newDocs)
        newDocs.words.forEach {
            wordToCluster.getOrPut(it.key, { mutableSetOf() }).add(this)
            this.words.words[it.key] = this.words.words.getOrDefault(it.key, 0) + it.value
        }
    }

    fun add(cluster: Cluster<DocType>, wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>){
        words.add(cluster.words)
        docs.addAll(cluster.docs)
        cluster.words.words.forEach {
                (word, _) -> wordToCluster[word]!!.add(this) // We know the word existed before in the other cluster
        }
    }

    fun remove(wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>) {
        words.words.forEach {
                (word, _) -> wordToCluster[word]!!.remove(this) // We know the word existed before
        }
    }

    fun mostRepresentativeDoc(): DocType{
        if(mostRepresentative == null)
            mostRepresentative = docs.maxByOrNull { it.similarity(words) }!!
        return mostRepresentative!!
    }
}

private fun Words.similarity(other: Words): Double {
    if(this.words.isEmpty() || other.words.isEmpty()) return 0.0

    val smaller: Map<String, Int>
    val larger: Map<String, Int>
    if(this.words.size > other.words.size) {
        larger = this.words
        smaller = other.words
    } else {
        larger = other.words
        smaller = this.words
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

private fun Words.add(other: Words) {
    other.words.forEach {
            (word, num) -> this.words[word] = this.words.getOrDefault(word, 0) + num
    }
}

class Clusterer<DocType : Words> {
    private val clusterCreationThreshold = 0.4
    private val clusterSizeThresholdForMergeAttempt = 3
    private val clusterSimilarityMergeThreshold = 0.4

    val clusters = mutableListOf<Cluster<DocType>>()
    private val wordToCluster = mutableMapOf<String, MutableSet<Cluster<DocType>>>()

    fun addDoc(docs: DocType){
        // Find most similar cluster
        var bestCluster: Cluster<DocType>? = null
        var bestSimilarity = Double.MIN_VALUE
        docs.words.forEach { word ->
            wordToCluster[word.key]?.forEach {
                val s = docs.similarity(it.words)
                if(s > bestSimilarity){
                    bestSimilarity = s
                    bestCluster = it
                }
            }
        }

        if(bestCluster == null || bestSimilarity < clusterCreationThreshold){
            // Create new cluster, because nothing is similar enough
            clusters.add(Cluster(docs, wordToCluster))
        } else {
            // Add into most similar cluster
            val bestExistingCluster = bestCluster!!
            bestExistingCluster.add(docs, wordToCluster)

            if(bestExistingCluster.docs.size >= clusterSizeThresholdForMergeAttempt){
                // Find most similar cluster
                val candidates = mutableSetOf<Cluster<DocType>>()
                bestExistingCluster.words.words
                    .map { (word, _) -> wordToCluster[word] } // All clusters with same words
                    .forEach { candidates.addAll(it!!.asIterable()) }

                candidates.remove(bestExistingCluster); // Not this cluster
                val mostSimilarCluster = candidates.maxByOrNull { it.words.similarity(bestExistingCluster.words) }
                if(mostSimilarCluster != null && mostSimilarCluster.words.similarity(bestExistingCluster.words) >= clusterSimilarityMergeThreshold){
                    var smaller = mostSimilarCluster;
                    var larger = bestExistingCluster

                    if(smaller.size() > larger.size()){
                        // Swap
                        val tmp = larger
                        larger = smaller
                        smaller = tmp
                    }

                    // Merge smaller into larger
                    larger.add(smaller, wordToCluster)

                    // Remove smaller
                    smaller.remove(wordToCluster)
                    clusters.remove(smaller)

                    //println("Merging: ${smaller.words.words} -> ${larger.words.words}")
                }
            }
        }
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