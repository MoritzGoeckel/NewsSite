import java.io.File

data class Doc(val content: String, val words: Set<String>) { }

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
                .toSet()
        )
    }
}

class Cluster {
    val docs = mutableListOf<Doc>()
    val words = mutableSetOf<String>()

    constructor(doc: Doc, wordToCluster: MutableMap<String, MutableSet<Cluster>>) {
        add(doc, wordToCluster)
    }

    override fun toString(): String{
        return docs.toString();
    }

    fun add(doc: Doc, wordToCluster: MutableMap<String, MutableSet<Cluster>>){
        docs.add(doc);
        doc.words.forEach {
            wordToCluster.getOrPut(it, { mutableSetOf() }).add(this)
            words.add(it)
        }
    }
}

class Clusterer {
    val clusters = mutableListOf<Cluster>()
    val wordToCluster = mutableMapOf<String, MutableSet<Cluster>>()

    fun addDoc(doc: Doc){
        var bestCluster: Cluster? = null;
        var bestSimilarity = Double.MIN_VALUE;
        doc.words.forEach { word ->
            val c = wordToCluster.get(word)?.forEach {
                val s = similarity(doc, it)
                if(s > bestSimilarity){
                    bestSimilarity = s;
                    bestCluster = it;
                }
            }
        }

        if(bestCluster == null || bestSimilarity < 0.6){
            clusters.add(Cluster(doc, wordToCluster))
        } else {
            bestCluster!!.add(doc, wordToCluster);
        }
    }

    fun similarity(doc: Doc, cluster: Cluster): Double {
        if(doc.words.isEmpty() || cluster.words.isEmpty()) return 0.0

        val smaller: Set<String>
        val larger: Set<String>
        if(doc.words.size > cluster.words.size) {
            larger = doc.words
            smaller = cluster.words
        } else {
            larger = cluster.words
            smaller = doc.words
        }

        val numMatching = smaller.count { larger.contains(it) }
        // TODO: Also consider that some words are more important in a cluster than others
        // TODO: So the word set in the cluster should be a map<String, Integer>

        return numMatching.toDouble() / larger.size.toDouble();
    }
}

fun main() {
    println("Start")
    val loader = Loader();
    val clusterer = Clusterer();
    loader.docs.take(10000).forEach {
        clusterer.addDoc(it)
    }

    clusterer.clusters.filter { it.docs.size > 2 }
                      .sortedBy { it.docs.size }
                      .forEach {
                          it.docs.forEach { println(it.content) };
                          println()
                      }
    println(clusterer.clusters.size)
}