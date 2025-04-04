package server

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import grouping.Cluster
import io.javalin.Javalin
import io.javalin.http.ContentType
import structures.Article
import structures.Original
import java.io.File
import java.sql.Connection

class WebServer(val connection: Connection) {

    private val basePath = "static"

    private fun loadFile(name: String) = File("""${basePath}/${name}""").readText()
    private fun loadBytes(name: String) = File("""${basePath}/${name}""").readBytes()

    // TODO maybe make private and get from db, just receive signal of change
    var clusters = listOf<Cluster<Article>>()
    var articles = listOf<Article>()

    fun start() {
        val app = Javalin.create()

        addFrontPage(app)
        addArticlesPage(app)
        addRestEndpoints(app)
        addStaticEndpoints(app)
        addArticleEndpoint(app)

        app.start(7000)
        println("Server running on http://localhost:7000/")
    }

    private fun addFrontPage(app: Javalin){
        app.get("/") {
            it.result("Front page!")
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addArticlesPage(app: Javalin) {
        app.get("/articles") {
            val page = ArticlesPage()
            it.result(page.html(articles))
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addArticleEndpoint(app: Javalin) {
        app.get("article/{id}") {
            val article = Original.getOriginal(it.pathParam("id"), connection)
            val page = ArticlePage(articleFromOriginal(article))

            it.result(page.html())
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addRestEndpoints(app: Javalin) {
        app.get("/articles.json") { it ->
            val root = JsonArray()
            articles.forEach { article -> root.add(article.toJson()) }
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

    private fun addStaticEndpoints(app: Javalin) {
        app.get("/articles") {
            it.contentType(ContentType.TEXT_HTML).result(
                loadFile("index.html")
            );
        }

        app.get("/styles.css") {
            it.contentType(ContentType.TEXT_CSS).result(
                loadFile("styles.css")
            )
        }

        app.get("/components.js") {
            it.contentType(ContentType.JAVASCRIPT).result(
                loadFile("components.js")
            )
        }

        app.get("/favicon.png") {
            it.contentType(ContentType.IMAGE_PNG).result(
                loadBytes("favicon.png")
            )
        }
    }
}
