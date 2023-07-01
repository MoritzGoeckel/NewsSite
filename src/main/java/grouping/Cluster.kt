package grouping

import structures.Words

class Cluster<DocType : Words>(newDocs: DocType, wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>) {
    val docs = mutableListOf<DocType>()
    val words = Words()

    var representative: DocType? = null

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
            wordToCluster.getOrPut(it.key) { mutableSetOf() }.add(this)
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

    fun sortByRepresentative() {
        docs.sortByDescending { it.similarity(words) / it.words.size.toDouble() }
    }
}