package summarizer

import grouping.Cluster
import structures.Article
import structures.Original
import java.sql.Connection

const val MINIMUM_SOURCES = 5

abstract class SummarizerImpl {
    abstract fun summarize(article: Article): Original;
    abstract fun summarize(articles: List<Article>): Original;
}

class Summarizer(val impl: SummarizerImpl, val connection: Connection,) {
    private val done = mutableSetOf<Int>() // Article IDs
    fun makeAndInsertOriginals(clusters: List<Cluster<Article>>){
        val clustersToSummarize = clusters
            // More than MINIMUM_SOURCES docs
            .filter { cluster -> cluster.docs.size >= MINIMUM_SOURCES }
            // Not already summarized (cache)
            .filter { cluster ->
                cluster.docs.count { doc -> !done.contains(doc.id) } >= MINIMUM_SOURCES
            }
            // More than MINIMUM_SOURCES sources
            .filter { cluster ->
                cluster.docs.distinctBy { it.source }.size >= MINIMUM_SOURCES
            }
            // Not already summarized (db)
            .filter { cluster ->
                val articleIds = cluster.docs.map { it.id }
                notSummarized(articleIds)
            }
            // Many sources should be on top
            .sortedByDescending { cluster -> cluster.docs.distinctBy { it.source }.size }

        clustersToSummarize.forEach {  cluster ->
            val articleIds = cluster.docs.map { it.id }
            val articles = lookupArticles(articleIds)
            val original = impl.summarize(articles)
            done.addAll(articleIds)
            val id = original.insertInto(connection)
            updateSummaryId(articleIds, id)
        }
    }

    private fun updateSummaryId(articleIds: List<Int>, originalId: Int) {
        val stmt = connection.prepareStatement("UPDATE articles SET original_id = ? WHERE id = ANY(?)")
        stmt.setInt(1, originalId)
        stmt.setArray(2, connection.createArrayOf("int", articleIds.toTypedArray()))
        stmt.executeUpdate()
    }

    private fun notSummarized(articleIds: List<Int>): Boolean {
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

