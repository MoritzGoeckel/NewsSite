package server

import structures.Original

class OriginalsPage {
    fun html(originals: List<Original>): String {

        val aEngine = TemplateEngine()

        val articles = originals.map { articleFromOriginal(it) }

        println(articles.first().headline)
        println(articles.first().body)

        aEngine.put("articles", articles)

        return aEngine.run("static/templates/originals.vm")
    }
}