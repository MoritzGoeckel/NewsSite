package summarizer

import grouping.Cluster
import structures.Article
import structures.Original
import util.printError
import java.sql.Connection

const val MINIMUM_SOURCES = 5

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
            val oldSize = lookupNumberOfArticles(cluster) // TODO: Cache this
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
            val original = impl.summarize(articles)
            done.addAll(articleIds)
            val id = lookupOriginalId(articleIds)
            original.updateInto(connection, id)
            updateSummaryId(articleIds, id)
            printError("Summarizer", "Updated original id=$id")
        }
    }

    private fun lookupNumberOfArticles(cluster: Cluster<Article>): Int {
        val articleIds = cluster.docs.map { it.id }
        val originalId = lookupOriginalId(articleIds)
        return lookupNumberOfArticles(originalId)
    }

    private fun lookupNumberOfArticles(originalId: Int): Int {
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

    private fun lookupOriginalId(articleIds: List<Int>): Int{
        val stmt = connection.prepareStatement("SELECT original_id FROM articles WHERE id = ANY(?) LIMIT 1")
        stmt.setArray(1, connection.createArrayOf("int", articleIds.toTypedArray()))
        val result = stmt.executeQuery()
        if (result.next()) {
            return result.getInt(1)
        } else {
            throw Exception("Failed to lookup original ID.")
        }
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

