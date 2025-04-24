package server.pages

import processors.getBaseUrl
import server.*
import structures.Original
import java.sql.Connection

class OriginalPage() {
    val ROW_HTML = """
    <div class="col-sm-12">
        <div class="article_wrapper dark vertical">
            <div class="afterArticle">
                <h2>__HL__</h2>
                <div class="description regular_text">__TEASER__</div>
                <div class="description regular_text">__CONTENT__</div>
                <div class="regular_text">
                <h3>Sources:</h3>
                __SOURCES__
                </div>
            </div>
        </div>
    </div>
    """.trimIndent()

    fun html(connection: Connection, id: Int): String {
        val original = selectOriginal(connection, id)
        val articles = selectArticlesFor(connection, original)
        val sources = articles.joinToString(separator = "\n") { article ->
            val totalLength = 70
            val source = getBaseUrl(article.source) + ": "
            val text = shortenToClosestWord(article.preview_head, totalLength - source.length)
            """
            <span class="source">${source}<a target="_blank" href="${article.url}" class="sourceLink">${text}</a></span>
            """.trimIndent()
        }
        val row = ROW_HTML
            .replace("__ID__", original.id.toString())
            .replace("__HL__", original.head)
            .replace("__TEASER__", original.teaser)
            .replace("__CONTENT__", toHTML(original.content))
            .replace("__SOURCES__", sources)

        return ALL_HTML.replace("__ROWS__", row)
    }
}