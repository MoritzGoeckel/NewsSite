package server

class ArticlePage(val article: Article) {
    fun html(): String {
        val aEngine = TemplateEngine()
        aEngine.put("article", article)
        return aEngine.run("static/templates/article.vm")
    }
}