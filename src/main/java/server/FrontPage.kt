package server

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import structures.Original
import java.io.StringWriter

class Article(val headline: String, val body: String, val links: List<String>, val img: String) {

}

fun formatContent(text: String): String{
    return text.replace("(?:^|\\n)(.+)(?:\$|\\n)".toRegex()) {
        if(it.groups.first() != null) {
            "<p>" + it.groups.first()!!.value.replace("\n", "") + "</p>" + "\n"
        }
        else {
            ""
        }
    }
}

class FrontPage {
    fun html(originals: List<Original>): String {

        val charset = Charsets.UTF_8.name()

        val velocityEngine = VelocityEngine()
        velocityEngine.init()
        velocityEngine.setProperty("directive.parse.max_depth", "100")
        velocityEngine.setProperty("velocimacro.max_depth", "100")
        velocityEngine.setProperty("resource.default_encoding", charset)
        velocityEngine.setProperty("output.encoding", charset)

        val template: Template = velocityEngine.getTemplate("static/templates/index.vm")
        template.encoding = charset

        val articles = originals.map { Article(it.head, formatContent(it.content), listOf("URL1", "URL2", "URL3"), it.images.first()) }

        println(articles.first().headline)
        println(articles.first().body)

        val context = VelocityContext()
        context.put("name", "Moritz")
        context.put("articles", articles)

        val writer = StringWriter()
        template.merge(context, writer)

        return writer.toString()
    }
}