package server.pages

import okhttp3.internal.concurrent.formatDuration
import server.formatTimeDuration
import server.selectOriginals
import java.sql.Connection
import java.time.Duration
import java.time.Instant

class OriginalsPage {
    val ROW_HTML = """
    <div class="col-sm-12">
        <div class="article_wrapper dark vertical">
            <a href="original/__ID__" class="article">
                <div class="afterArticle">
                    <h2>__HL__</h2>
                    <div class="description regular_text">__TEASER__</div>
                    <div class="time regular_text">__TIME__</div>
                </div>
            </a>
        </div>
    </div>
    """.trimIndent()

    fun html(connection: Connection): String {
        val originals = selectOriginals(connection, 20)
        val rows = originals.joinToString(separator = "\n") { article ->
            ROW_HTML
                .replace("__ID__", article.id.toString())
                .replace("__HL__", article.head)
                .replace("__TEASER__", article.teaser)
                .replace("__TIME__", formatTimeDuration(Duration.between(article.time, Instant.now())) + " ago")
        }
        return ALL_HTML.replace("__ROWS__", rows)
    }
}