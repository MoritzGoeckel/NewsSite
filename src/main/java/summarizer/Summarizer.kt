package summarizer

import grouping.Cluster
import structures.Article
import structures.Original
import util.printError
import java.sql.Connection

const val MINIMUM_SOURCES = 5
const val NULL_ORIGINAL_ID = 1

abstract class SummarizerImpl {
    abstract fun summarize(article: Article): Original;
    abstract fun summarize(articles: List<Article>): Original;
}

class Summarizer(val impl: SummarizerImpl, val connection: Connection,) {
    private val done = mutableSetOf<Int>() // Article IDs
    fun makeAndInsertOriginals(clusters: List<Cluster<Article>>){
        val goodClusters = clusters
            // More than MINIMUM_SOURCES sources
            .filter { cluster ->
                cluster.docs.distinctBy { it.source }.size >= MINIMUM_SOURCES
            }
            // Many sources should be on top
            .sortedByDescending { cluster -> cluster.docs.distinctBy { it.source }.size }

        val clustersToSummarize = mutableListOf<Cluster<Article>>()
        val alreadySummarized = mutableListOf<Cluster<Article>>()
        goodClusters.forEach { cluster ->
            val articleIds = cluster.docs.map { it.id }
            if (notSummarized(articleIds)){
                clustersToSummarize.add(cluster)
            } else {
                alreadySummarized.add(cluster)
            }
        }

        insertNewOriginals(clustersToSummarize)
        updateOriginals(alreadySummarized)
    }

    private fun insertNewOriginals(clusters: List<Cluster<Article>>){
        clusters.forEach {  cluster ->
            val articleIds = cluster.docs.map { it.id }
            val articles = lookupArticles(articleIds)
            val original = impl.summarize(articles)
            done.addAll(articleIds)
            val id = original.insertInto(connection)
            updateSummaryId(articleIds, id)
            printError("Summarizer", "Inserted original id=$id")
        }
    }

    private fun updateOriginals(clusters: List<Cluster<Article>>){
        clusters
        .filter { cluster ->
            val newSize = cluster.docs.distinctBy { it.source }.size
            val oldSize = lookupArticlesReferencedByOriginalIn(cluster) // TODO: Cache this
            if(newSize > (oldSize + oldSize * 0.1)) // 10% increase
            {
                printError("Summarizer", "Updating original from $oldSize to $newSize articles")
                true
            } else {
                false
            }
        }
        .forEach { cluster ->
            val articleIds = cluster.docs.map { it.id }
            val articles = lookupArticles(articleIds)
            val originalIds = lookupOriginalIds(articleIds)
            val originalIdSet = originalIds.toMutableSet()
            originalIdSet.remove(NULL_ORIGINAL_ID)
            if (originalIdSet.size > 1) {
                printError("Summarizer", "Cannot update original, multiple originals in cluster: $originalIds")
            } else if (originalIdSet.isEmpty()) {
                throw Exception("Can't update, because no original in cluster $articleIds")
            } else {
                val id = originalIdSet.first()
                val original = impl.summarize(articles)
                original.updateInto(connection, id)
                updateSummaryId(articleIds, id)
                done.addAll(articleIds)
                printError("Summarizer", "Updated original id=$id")
            }
        }
    }

    private fun lookupArticlesReferencedByOriginalIn(cluster: Cluster<Article>): Int {
        val articleIds = cluster.docs.map { it.id }
        val originalIds = lookupOriginalIds(articleIds)
        val originalIdSet = originalIds.toMutableSet()
        originalIdSet.remove(NULL_ORIGINAL_ID)

        if (originalIdSet.size > 1) {
            printError("Summarizer", "Multiple originals in cluster, can't lookup articles: $originalIds")
            return 0
        } else if (originalIdSet.isEmpty()) {
            printError("Summarizer", "No original in cluster, can't lookup articles: $articleIds")
            return 0
        }

        val originalId = originalIdSet.first()
        return lookupNumberOfArticlesIn(originalId)
    }

    private fun lookupNumberOfArticlesIn(originalId: Int): Int {
        val stmt = connection.prepareStatement("SELECT COUNT(DISTINCT source) FROM articles WHERE original_id = ?")
        stmt.setInt(1, originalId)
        val result = stmt.executeQuery()
        if (result.next()) {
            return result.getInt(1)
        } else {
            throw Exception("Failed to lookup number of articles.")
        }
    }

    private fun updateSummaryId(articleIds: List<Int>, originalId: Int) {
        val stmt = connection.prepareStatement("UPDATE articles SET original_id = ? WHERE id = ANY(?)")
        stmt.setInt(1, originalId)
        stmt.setArray(2, connection.createArrayOf("int", articleIds.toTypedArray()))
        stmt.executeUpdate()
    }

    private fun lookupOriginalIds(articleIds: List<Int>): List<Int>{
        val stmt = connection.prepareStatement("SELECT original_id FROM articles WHERE id = ANY(?) AND original_id != $NULL_ORIGINAL_ID")
        stmt.setArray(1, connection.createArrayOf("int", articleIds.toTypedArray()))
        val result = stmt.executeQuery()
        val originalIds = mutableListOf<Int>()
        while (result.next()) {
            originalIds.add(result.getInt(1))
        }
        return originalIds
    }

    private fun notSummarized(articleIds: List<Int>): Boolean {
        // Check cache
        if (articleIds.any { done.contains(it) }) return false

        val stmt = connection.prepareStatement("SELECT COUNT(*) FROM articles WHERE id = ANY(?) AND original_id != 1")
        stmt.setArray(1, connection.createArrayOf("int", articleIds.toTypedArray()))
        val result = stmt.executeQuery()
        if (result.next()) {
            val count = result.getInt(1)
            return count == 0
        } else {
            throw Exception("Failed to check if articles are summarized.")
        }
    }

    private fun lookupArticles(articleIds: List<Int>): List<Article> {
        val stmt = connection.prepareStatement("SELECT * FROM articles WHERE id = ANY(?)")
        stmt.setArray(1, connection.createArrayOf("int", articleIds.toTypedArray()))
        val result = stmt.executeQuery()
        val articles = mutableListOf<Article>()
        while (result.next()) {
            articles.add(Article(result))
        }
        return articles
    }
}

