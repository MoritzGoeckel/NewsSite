package grouping

import printTrace
import structures.Words

class Clusterer<DocType : Words> {
    private val clusterCreationThreshold = 0.4
    private val clusterSizeThresholdForMergeAttempt = 3
    private val clusterSimilarityMergeThreshold = 0.4

    private val clusters = mutableListOf<Cluster<DocType>>()
    private val wordToCluster = mutableMapOf<String, MutableSet<Cluster<DocType>>>()
    private val docToCluster = mutableMapOf<DocType, Cluster<DocType>>()

    fun clusters(): List<Cluster<DocType>>{
        return clusters
    }

    fun remove(doc: DocType){
        val cluster = docToCluster[doc]
        if (cluster != null) {
            // Remove / count down the words in the cluster, remove the document
            val removedWords = cluster.remove(doc)

            // Remove the cluster from the wordToCluster index, for words that hit a count of zero
            for(word in removedWords){
                wordToCluster.computeIfPresent(word) {_, clusters ->
                    clusters.remove(cluster)
                    if(clusters.isNotEmpty()) {
                        clusters
                    } else {
                        null
                    }
                }
            }

            if(cluster.docs.isEmpty()){
                assert(cluster.words.isEmpty())
                clusters.remove(cluster)
            }
        }
        docToCluster.remove(doc)
    }

    fun add(doc: DocType){
        // Find most similar cluster
        var bestCluster: Cluster<DocType>? = null
        var bestSimilarity = Double.MIN_VALUE
        doc.words.forEach { word ->
            wordToCluster[word.key]?.forEach { cluster ->
                val similarity = doc.similarity(cluster.words)
                if(similarity > bestSimilarity){
                    bestSimilarity = similarity
                    bestCluster = cluster
                }
            }
        }

        if(bestCluster == null || bestSimilarity < clusterCreationThreshold){
            // Create new cluster, because nothing is similar enough
            val newCluster = Cluster(doc, wordToCluster)
            clusters.add(newCluster)
            docToCluster[doc] = newCluster
        } else {
            // Add into most similar cluster
            val bestExistingCluster = bestCluster!!
            bestExistingCluster.add(doc, wordToCluster)
            docToCluster[doc] = bestExistingCluster

            // Try merging large enough clusters with similar ones
            if (bestExistingCluster.docs.size >= clusterSizeThresholdForMergeAttempt) {
                tryMergeClusterWithSimilar(bestExistingCluster)
            }
        }
    }

    private fun tryMergeClusterWithSimilar(clusterToMerge: Cluster<DocType>) {
        // Find most similar cluster
        val candidatesWithSameWords = mutableSetOf<Cluster<DocType>>()
        clusterToMerge.words.words
            .map { (word, _) -> wordToCluster[word] } // All clusters with same words
            .forEach { candidatesWithSameWords.addAll(it!!.asIterable()) }
        candidatesWithSameWords.remove(clusterToMerge) // Not this cluster

        val mostSimilarCluster =
            candidatesWithSameWords.maxByOrNull { it.words.similarity(clusterToMerge.words) }

        if (mostSimilarCluster != null && mostSimilarCluster.words.similarity(clusterToMerge.words) >= clusterSimilarityMergeThreshold) {
            var smaller = mostSimilarCluster
            var larger = clusterToMerge

            if (smaller.size() > larger.size()) {
                // Swap
                val tmp = larger
                larger = smaller
                smaller = tmp
            }

            // Merge smaller into larger
            larger.add(smaller, wordToCluster)

            // Remove smaller
            smaller.pruneWordToClusterIndex(wordToCluster)
            clusters.remove(smaller)

            // Update docToCluster, point all docs of the merged from cluster to the merged to cluster
            smaller.docs.forEach {
                docToCluster[it] = larger
            }

            printTrace("Clusterer", "Merging: ${smaller.words.words} -> ${larger.words.words}")
        }
    }

    fun validate(){
        TODO()
        // wordToCluster
        // docToCluster
        // clusters
    }

    fun statistics(): String {
        val builder = StringBuilder()
        builder.append("Number of clusters:        ${clusters.size}\n")
        builder.append("Word to cluster index:     ${wordToCluster.size}\n")
        builder.append("Document to cluster index: ${docToCluster.size}\n")
        return builder.toString()
    }
}