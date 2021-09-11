import java.io.File
import kotlin.math.round
import kotlin.math.roundToInt

data class Doc(val content: String, val words: Map<String, Int>) { }

class Loader {
    var stopwords: MutableSet<String> = mutableSetOf();
    var docs: MutableList<Doc> = mutableListOf();

    init {
        File("C:\\Users\\Moritz\\Desktop\\stopwords.txt").forEachLine { stopwords.add(it) }
        File("C:\\Users\\Moritz\\Desktop\\abcnews-date-text.csv").forEachLine {
            docs.add(parseDoc(it.split(',')[1]))
        }
    }

    fun stem(word: String): String{
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

class Cluster {
    val docs = mutableListOf<Doc>()
    val words = mutableMapOf<String, Int>()

    constructor(doc: Doc, wordToCluster: MutableMap<String, MutableSet<Cluster>>) {
        add(doc, wordToCluster)
    }

    override fun toString(): String{
        return docs.toString();
    }

    fun add(doc: Doc, wordToCluster: MutableMap<String, MutableSet<Cluster>>){
        docs.add(doc);
        doc.words.forEach {
            wordToCluster.getOrPut(it.key, { mutableSetOf() }).add(this)
            words[it.key] = words.getOrDefault(it.key, 0) + it.value;
        }
    }
}

class Clusterer {
    val NEW_CLUSTER_THRESHOLD = 0.7

    val clusters = mutableListOf<Cluster>()
    val wordToCluster = mutableMapOf<String, MutableSet<Cluster>>()

    fun addDoc(doc: Doc){
        var bestCluster: Cluster? = null;
        var bestSimilarity = Double.MIN_VALUE;
        doc.words.forEach { word ->
            val c = wordToCluster.get(word.key)?.forEach {
                val s = similarity(doc, it)
                if(s > bestSimilarity){
                    bestSimilarity = s;
                    bestCluster = it;
                }
            }
        }

        if(bestCluster == null || bestSimilarity < NEW_CLUSTER_THRESHOLD){
            clusters.add(Cluster(doc, wordToCluster))
        } else {
            bestCluster!!.add(doc, wordToCluster);
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

        var same = 0;
        var all = 0;
        smaller.forEach{
            if(larger.containsKey(it.key)) {
                same += larger[it.key]!! + it.value // count from both
            }
            all += it.value // only count from smaller
        }

        larger.forEach{
            all += it.value // only count from larger
        }

        return same.toDouble() / all.toDouble()
    }
}

fun main() {

    println("Start")
    val start = System.currentTimeMillis();
    val loader = Loader();
    val afterLoad = System.currentTimeMillis();
    val clusterer = Clusterer();
    loader.docs.take(100 * 1000).forEach {
        clusterer.addDoc(it)
    }
    val afterCluster = System.currentTimeMillis();

    clusterer.clusters.filter { it.docs.size > 2 }
                      .sortedBy { it.docs.size }
                      .forEach {
                          it.docs.forEach { println(it.content) };
                          println()
                      }
    val afterPrint = System.currentTimeMillis();

    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterLoad - start}ms")
    println("Clustering ${afterCluster - afterLoad}ms / ${(loader.docs.size.toDouble() / ((afterCluster - afterLoad) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}