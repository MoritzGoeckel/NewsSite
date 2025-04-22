package server

import io.javalin.Javalin
import io.javalin.http.ContentType
import structures.Article
import structures.Original
import java.io.File
import java.sql.Connection

class FrontendServer(val connection: Connection, val port: Int) {
    private val basePath = "static"

    private fun loadFile(name: String) = File("""${basePath}/${name}""").readText()
    private fun loadBytes(name: String) = File("""${basePath}/${name}""").readBytes()

    fun start(): FrontendServer {
        val app = Javalin.create()

        addFrontPage(app)
        addArticlesPage(app)
        addArticleEndpoint(app)
        addStaticEndpoints(app)

        app.start(port)
        println("Server running on http://localhost:$port/")
        return this
    }

    private fun addFrontPage(app: Javalin){
        app.get("/") {
            it.result("Front page!")
                .contentType("text/html; charset=utf-8")
            // TODO Originals
        }
    }

    private fun addArticlesPage(app: Javalin) {
        app.get("/articles") {
            val page = ArticlesPage()
            it.result(page.html(selectArticles(connection)))
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addArticleEndpoint(app: Javalin) {
        app.get("article/{id}") {
            val original = Original.getOriginal(it.pathParam("id"), connection)
            val page = ArticlePage(articleFromOriginal(original))

            it.result(page.html())
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addStaticEndpoints(app: Javalin) {
        app.get("/styles.css") {
            it.contentType(ContentType.TEXT_CSS).result(
                loadFile("styles.css")
            )
        }

        app.get("/components.js") {
            it.contentType(ContentType.JAVASCRIPT_MODERN).result(
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
