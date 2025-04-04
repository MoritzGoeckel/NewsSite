package server

import grouping.Cluster
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.ContentType
import okio.ByteString.Companion.encode
import structures.Article
import structures.Original
import java.io.File
import java.sql.Connection
/*
class WebServer {

    private val basePath = "static"

    private fun loadFile(name: String) = File("""${basePath}/${name}""").readText()
    private fun loadBytes(name: String) = File("""${basePath}/${name}""").readBytes()

    var clusters: List<Cluster<Article>> = listOf()
    var connection: Connection? = null

    fun start() {
        val app = Javalin.create()

        if (connection != null) {
            val frontPage = FrontPage()
            app.get("/") {
                val originals = mutableListOf<Original>()
                clusters
                    .reversed()
                    .map { cluster ->
                        for (doc in cluster.docs) {
                            if (doc.originalUrl.isNotEmpty()) {
                                originals.add(Original.getOriginal(doc.originalUrl, connection!!))
                                break // next cluster
                            }
                        }
                    }

                it.result(frontPage.html(originals))
                    .contentType("text/html; charset=utf-8")
            }
        }

        addRestEndpoints(app)
        addStaticEndpoints(app)
        addArticleEndpoint(app)

        app.start(7000)
        println("Server running on http://localhost:7000/")
    }

    private fun addArticleEndpoint(app: Javalin) {
        app.get("article/{id}") {
            val article = Original.getOriginal(it.pathParam("id"), connection!!)
            val page = ArticlePage(articleFromOriginal(article))

            it.result(page.html())
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addRestEndpoints(app: Javalin) {
        app.get("/clusters.json") {
            val root = JsonArray()
            clusters.map { cluster ->
                val clusterJson = JsonObject()
                //clusterJson.addProperty("")

                // all articles
                val docs = JsonArray()
                cluster.docs.map { doc -> doc.toJson() }.forEach { docJson -> docs.add(docJson) }
                clusterJson.add("articles", docs)

                // representative article
                if (cluster.representative != null) {
                    clusterJson.add("representative", cluster.representative!!.toJson())
                }

                clusterJson
            }.forEach { clusterJson -> root.add(clusterJson) }

            it.contentType(ContentType.JSON).result(root.toString())
        }

        if (connection != null) {
            app.get("/originals.json") {
                val root = JsonArray()
                // clusters should already be sorted
                clusters
                    .reversed()
                    .map { cluster ->
                        for (doc in cluster.docs) {
                            if (doc.originalUrl.isNotEmpty()) {
                                root.add(Original.getOriginal(doc.originalUrl, connection!!).toJson())
                                break // next cluster
                            }
                        }
                    }
                it.contentType(ContentType.JSON).result(root.toString())
            }
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
*/