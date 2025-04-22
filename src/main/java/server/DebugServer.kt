package server

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import grouping.Cluster
import io.javalin.Javalin
import io.javalin.http.ContentType
import structures.Article
import structures.Original
import java.sql.Connection

class DebugServer(val connection: Connection, val port: Int) {

    var clusters = listOf<Cluster<Article>>()

    fun start(): DebugServer {
        val app = Javalin.create()
        addRestEndpoints(app)
        app.start(port)
        println("DebugServer running on http://localhost:$port/")
        return this
    }

    private fun addRestEndpoints(app: Javalin) {
        app.get("/articles.json") { it ->
            val root = JsonArray()
            selectArticles(connection).forEach { article -> root.add(article.toJson()) }
            it.contentType(ContentType.JSON).result(root.toString())
        }

        app.get("article/{id}") {
            val article = Original.getOriginal(it.pathParam("id"), connection)
            val page = ArticlePage(articleFromOriginal(article))

            it.result(page.html())
                .contentType("text/html; charset=utf-8")
        }

        app.get("/originals.json") {
            val root = JsonArray()
            selectOriginals(connection).forEach { original -> root.add(original.toJson()) }
            it.contentType(ContentType.JSON).result(root.toString())
        }

        app.get("/clusters.json") {
            val root = JsonArray()
            clusters.forEach { cluster ->
                val clusterJson = JsonObject()
                val docsJson = JsonArray()
                cluster.docs.forEach { doc -> docsJson.add(doc.toJson()) }
                clusterJson.add("docs", docsJson)
                val wordsJson = JsonObject()
                cluster.words.words.forEach{ word -> wordsJson.addProperty(word.key, word.value) }
                clusterJson.add("words", wordsJson)
                root.add(clusterJson)
            }
            it.contentType(ContentType.JSON).result(root.toString())
        }
    }
}
