package server

import structures.Article

class ArticlesPage {
    fun html(articles: List<Article>): String {
        val aEngine = TemplateEngine()
        aEngine.put("articles", articles)
        return aEngine.run("static/templates/articles.vm")
    }
}