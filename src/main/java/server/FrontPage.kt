package server

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import structures.Original
import java.io.StringWriter

class FrontPage {
    fun html(originals: List<Original>): String {

        val aEngine = TemplateEngine()

        val articles = originals.map { articleFromOriginal(it) }

        println(articles.first().headline)
        println(articles.first().body)

        aEngine.put("name", "Moritz")
        aEngine.put("articles", articles)

        return aEngine.run("static/templates/index.vm")
    }
}