package grouping

import structures.Words

class Clusterer<DocType : Words> {
    private val clusterCreationThreshold = 0.4
    private val clusterSizeThresholdForMergeAttempt = 3
    private val clusterSimilarityMergeThreshold = 0.4

    private val clusters = mutableListOf<Cluster<DocType>>()
    private val wordToCluster = mutableMapOf<String, MutableSet<Cluster<DocType>>>()

    fun clusters(): List<Cluster<DocType>>{
        return clusters
    }

    fun remove(doc: DocType){
        TODO()
        // Find the cluster and remove the doc
        // Remove the words of the doc from the wordToCluster
    }

    fun add(doc: DocType){
        // Find most similar cluster
        var bestCluster: Cluster<DocType>? = null
        var bestSimilarity = Double.MIN_VALUE
        doc.words.forEach { word ->
            wordToCluster[word.key]?.forEach {
                val s = doc.similarity(it.words)
                if(s > bestSimilarity){
                    bestSimilarity = s
                    bestCluster = it
                }
            }
        }

        if(bestCluster == null || bestSimilarity < clusterCreationThreshold){
            // Create new cluster, because nothing is similar enough
            clusters.add(Cluster(doc, wordToCluster))
        } else {
            // Add into most similar cluster
            val bestExistingCluster = bestCluster!!
            bestExistingCluster.add(doc, wordToCluster)

            if(bestExistingCluster.docs.size >= clusterSizeThresholdForMergeAttempt){
                // Find most similar cluster
                val candidates = mutableSetOf<Cluster<DocType>>()
                bestExistingCluster.words.words
                    .map { (word, _) -> wordToCluster[word] } // All clusters with same words
                    .forEach { candidates.addAll(it!!.asIterable()) }

                candidates.remove(bestExistingCluster) // Not this cluster
                val mostSimilarCluster = candidates.maxByOrNull { it.words.similarity(bestExistingCluster.words) }
                if(mostSimilarCluster != null && mostSimilarCluster.words.similarity(bestExistingCluster.words) >= clusterSimilarityMergeThreshold){
                    var smaller = mostSimilarCluster
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