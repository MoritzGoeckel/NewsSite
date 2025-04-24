package server.pages

import processors.getBaseUrl
import server.toHTML
import structures.Article
import structures.Original

class ArticlePage() {
    val ROW_HTML = """
    <div class="col-sm-12">
        <div class="article_wrapper dark vertical">
            <div class="afterArticle">
                <h2>__HL__</h2>
                <div class="description regular_text">__DESCRIPTION__</div>
                <div class="description regular_text">__CONTENT__</div>
                <span class="source" >Source: <a target="_blank" href="__SOURCE_URL__" class="sourceLink">__SOURCE__</a></span>
            </div>
        </div>
    </div>
    """.trimIndent()

    fun html(article: Article): String {
        var content = article.content
        if (content.isEmpty()){
            content = article.preview_content
        }

        var url = article.url
        if (url.isEmpty()){
            url = article.preview_url
        }

        val row = ROW_HTML
            .replace("__ID__", article.id.toString())
            .replace("__HL__", article.head)
            .replace("__DESCRIPTION__", article.description)
            .replace("__CONTENT__", toHTML(content))
            .replace("__SOURCE__", getBaseUrl(article.source))
            .replace("__SOURCE_URL__", url)

        return ALL_HTML.replace("__ROWS__", row)
    }
}