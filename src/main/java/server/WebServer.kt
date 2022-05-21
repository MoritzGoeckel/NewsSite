package server

import Cluster
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.Javalin
import io.javalin.http.ContentType
import structures.Article
import java.io.File

class WebServer {

    val basePath = "static\\"

    private fun loadFile(name: String) = File("""${basePath}\${name}""").readText()

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

        app.get("/scripts.js") {
            it.contentType(ContentType.JAVASCRIPT).result(
                loadFile("scripts.js")
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
                clusterJson.add("representative", cluster.mostRepresentativeDoc().toJson())

                clusterJson
            } .forEach { clusterJson -> root.add(clusterJson) }

            it.contentType(ContentType.JSON).result(root.toString())
        }

        app.start(7000)
    }
}

fun main() {
    WebServer().start()
}
