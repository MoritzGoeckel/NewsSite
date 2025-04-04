package grouping

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import structures.WithWords
import structures.Words

class Cluster<DocType : WithWords>(newDocs: DocType, wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>) {
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
        newDocs.getWords().words.forEach {
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

    fun pruneWordToClusterIndex(wordToCluster: MutableMap<String, MutableSet<Cluster<DocType>>>) {
        words.words.forEach {
                (word, _) -> wordToCluster[word]!!.remove(this) // We know the word existed before
        }
    }

    fun remove(doc: DocType): List<String> /*removed words*/ {
        val removedWords = mutableListOf<String>()
        docs.remove(doc)
        for((word, num) in doc.getWords().words){
            val remaining = words.remove(word, num)
            if (remaining == 0){
                removedWords.add(word)
            }
        }
        return removedWords
    }

    fun sortByRepresentative() {
        docs.sortByDescending { it.getWords().similarity(words) / it.getWords().words.size.toDouble() }
    }
}