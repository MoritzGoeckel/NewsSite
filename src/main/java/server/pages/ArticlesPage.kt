package server.pages

import server.firstSentence
import structures.Article

val ALL_HTML = """
<!DOCTYPE html>
<html>
<head>
    <title>Articles DE</title>
    <link rel="shortcut icon" href="/favicon.png"/>
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Open+Sans" rel="stylesheet"/>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.0-beta1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-0evHe/X+R7YkIZDRvuzKMRqM+OrBnVFBL6DOitfPri4tjfHxaWutUpFmBp4vmVor" crossorigin="anonymous"/>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.0-beta1/dist/js/bootstrap.bundle.min.js" integrity="sha384-pprn3073KE6tl6bjs2QrFaJGz5/SUsLqktiwsUTF55Jfv3qYSDhgCecCxMW52nD2" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/styles.css"/>
</head>
<body>

<div class="container" id="outer">
    <div class="container" id="title">
        <h1><a href="/originals">NEWS</a></h1>
        <div id="circle"></div>
    </div>
    <div class="container" id="articles">
        <div class="container">
        __ROWS__
        </div>
    </div>
    <div class="container" id="footer"></div>
</div>

<script type="text/javascript" src="components.js"></script>

</body>
</html>
"""

class ArticlesPage {
    val ROW_HTML = """
    <div class="col-sm-12">
        <div class="article_wrapper dark vertical">
            <a href="article/__ID__" class="article">
                <div class="afterArticle">
                    <h2>__HL__</h2>
                    <div class="description regular_text">__DESCRIPTION__</div>
                </div>
            </a>
        </div>
    </div>
    """.trimIndent()

    fun html(articles: List<Article>): String {
        val rows = articles.joinToString(separator = "\n") { article ->
            var content = firstSentence(article.preview_content)
            if(content.isEmpty()){
                content = firstSentence(article.content)
            }
            ROW_HTML
                .replace("__ID__", article.id.toString())
                .replace("__HL__", article.preview_head)
                .replace("__DESCRIPTION__", content)
        }
        return ALL_HTML.replace("__ROWS__", rows)
    }
}