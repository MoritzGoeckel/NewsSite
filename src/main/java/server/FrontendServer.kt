package server

import io.javalin.Javalin
import io.javalin.http.ContentType
import server.pages.ArticlePage
import server.pages.ArticlesPage
import server.pages.OriginalPage
import server.pages.OriginalsPage
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
        addOriginalsEndpoint(app)
        addOriginalEndpoint(app)
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
            val page = ArticlePage()
            it.result(page.html(selectArticle(connection, it.pathParam("id").toInt())))
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addOriginalEndpoint(app: Javalin) {
        app.get("original/{id}") {
            val page = OriginalPage()
            it.result(page.html(connection, it.pathParam("id").toInt()))
                .contentType("text/html; charset=utf-8")
        }
    }

    private fun addOriginalsEndpoint(app: Javalin) {
        app.get("/originals") {
            val page = OriginalsPage()
            it.result(page.html(connection))
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
