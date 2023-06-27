package server

import grouping.Cluster
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.Javalin
import io.javalin.http.ContentType
import structures.Article
import java.io.File

class WebServer {

    private val basePath = "static\\"

    private fun loadFile(name: String) = File("""${basePath}\${name}""").readText()
    private fun loadBytes(name: String) = File("""${basePath}\${name}""").readBytes()

    var clusters: List<Cluster<Article>> = listOf()

    fun start() {
        val app = Javalin.create()

        app.get("/") {
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
                if(cluster.representative != null) {
                    clusterJson.add("representative", cluster.representative!!.toJson())
                }

                clusterJson
            } .forEach { clusterJson -> root.add(clusterJson) }

            it.contentType(ContentType.JSON).result(root.toString())
        }

        app.start(7000)
        println("Server running on http://localhost:7000/")
    }
}

fun main() {
    WebServer().start()
}
